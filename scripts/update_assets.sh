#!/usr/bin/env bash
set -e

# Setup environment
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$HOME/.local/bin:$PATH"

ADB="adb"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== 1. Building Latest Debug APK ==="
./gradlew assembleDebug

APK_PATH="$(find app/build/outputs/apk/debug -name "*.apk" | head -n 1)"
if [ -z "$APK_PATH" ]; then
    echo "Error: No debug APK found in app/build/outputs/apk/debug/"
    exit 1
fi
echo "Using APK: $APK_PATH"

echo "=== 2. Checking Connected Device / Emulator ==="
EMULATOR_STARTED=0
DEVICE_ID="$($ADB devices | grep -E "emulator-|device\b" | grep -v "devices" | awk '{print $1}' | head -n 1 || true)"

if [ -z "$DEVICE_ID" ]; then
    echo "No running emulator or device found. Starting 'medium_phone' emulator..."
    if command -v android &>/dev/null; then
        android emulator start medium_phone
    else
        emulator -avd medium_phone -no-snapshot-load &
        $ADB wait-for-device
    fi
    DEVICE_ID="$($ADB devices | grep "emulator-" | awk '{print $1}' | head -n 1)"
    EMULATOR_STARTED=1
fi
echo "Target device: $DEVICE_ID"

echo "=== 3. Configuring Clean Environment & Seeding Preferences ==="
$ADB -s "$DEVICE_ID" shell settings put system show_touches 0
$ADB -s "$DEVICE_ID" shell settings put system pointer_location 0
$ADB -s "$DEVICE_ID" shell settings put global hide_error_dialogs 1

cat << 'EOF' > /tmp/sotto_locale_prefs_en.xml
<?xml version="1.0" encoding="utf-8" standalone="yes" ?><map><string name="selected_language">en</string></map>
EOF

cat << 'EOF' > /tmp/sotto_prefs_en.xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="v2_migrated" value="true" />
    <string name="saved_phrases">[{"text":"I cannot speak right now. Please read my screen.","language":"auto","isEmergency":true,"category":"Emergency"},{"text":"Please give me time.","language":"auto","isEmergency":false,"category":"Needs"},{"text":"I need a quiet space.","language":"auto","isEmergency":false,"category":"Needs"},{"text":"I need to leave now.","language":"auto","isEmergency":false,"category":"Needs"},{"text":"Yes, please.","language":"auto","isEmergency":false,"category":"Social"},{"text":"No, thank you.","language":"auto","isEmergency":false,"category":"Social"},{"text":"Thank you.","language":"auto","isEmergency":false,"category":"Social"},{"text":"Hello.","language":"auto","isEmergency":false,"category":"General"},{"text":"Please repeat that.","language":"auto","isEmergency":false,"category":"General"}]</string>
</map>
EOF

$ADB -s "$DEVICE_ID" install -r "$APK_PATH"

$ADB -s "$DEVICE_ID" push /tmp/sotto_locale_prefs_en.xml /data/local/tmp/sotto_locale_prefs.xml
$ADB -s "$DEVICE_ID" push /tmp/sotto_prefs_en.xml /data/local/tmp/sotto_prefs.xml
$ADB -s "$DEVICE_ID" shell "run-as com.amh.sotto mkdir -p /data/data/com.amh.sotto/shared_prefs" || true
$ADB -s "$DEVICE_ID" shell "run-as com.amh.sotto cp /data/local/tmp/sotto_locale_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_locale_prefs.xml"
$ADB -s "$DEVICE_ID" shell "run-as com.amh.sotto cp /data/local/tmp/sotto_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_prefs.xml"

echo "=== 4. Capturing Main Screen Screenshot ==="
$ADB -s "$DEVICE_ID" shell am force-stop com.amh.sotto
sleep 1
$ADB -s "$DEVICE_ID" shell am start -n com.amh.sotto/.MainActivity
sleep 3
$ADB -s "$DEVICE_ID" exec-out screencap -p > /tmp/raw_main_screen.png

echo "=== 5. Capturing Voice & Language Settings Screenshot ==="
# Tap "Edit List"
$ADB -s "$DEVICE_ID" shell input tap 870 150
sleep 1.5
# Tap Settings gear icon
$ADB -s "$DEVICE_ID" shell input tap 802 148
sleep 2.0
$ADB -s "$DEVICE_ID" exec-out screencap -p > /tmp/raw_voice_settings.png

echo "=== 6. Processing and Formatting Assets ==="
python3 - << 'PYEOF'
import os
from PIL import Image, ImageDraw

# 1. Process screenshot.png and screenshot_voice_settings.png
raw_main = Image.open('/tmp/raw_main_screen.png').convert('RGB')
raw_voice = Image.open('/tmp/raw_voice_settings.png').convert('RGB')

crop_box = (0, 65, 1080, 65 + 2280)

screen_main = raw_main.crop(crop_box).resize((528, 1024), Image.Resampling.LANCZOS)
screen_voice = raw_voice.crop(crop_box).resize((528, 1024), Image.Resampling.LANCZOS)

screen_main.save('assets/screenshot.png')
screen_voice.save('assets/screenshot_voice_settings.png')
print("Updated assets/screenshot.png and assets/screenshot_voice_settings.png (528x1024 RGB)")

# 2. Process play_store_feature_graphic.png
if os.path.exists('assets/play_store_feature_graphic.png'):
    fg = Image.open('assets/play_store_feature_graphic.png').convert('RGBA')
    raw_for_mockup = Image.open('/tmp/raw_main_screen.png').convert('RGBA')
    raw_mockup_cropped = raw_for_mockup.crop((0, 20, 1080, 2390)).resize((222, 475), Image.Resampling.LANCZOS)
    mask = Image.new('L', (222, 475), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([(0, 0), (222, 475)], radius=22, fill=255)
    fg.paste(raw_mockup_cropped, (711, 21, 933, 496), mask)
    fg.save('assets/play_store_feature_graphic.png')
    print("Updated assets/play_store_feature_graphic.png (1024x500 RGBA)")
PYEOF

if [ "$EMULATOR_STARTED" -eq 1 ]; then
    echo "=== 7. Stopping Emulator ==="
    if command -v android &>/dev/null; then
        android emulator stop "$DEVICE_ID" || true
    else
        $ADB -s "$DEVICE_ID" emu kill || true
    fi
fi

# Cleanup temp files
rm -f /tmp/raw_main_screen.png /tmp/raw_voice_settings.png /tmp/sotto_locale_prefs_en.xml /tmp/sotto_prefs_en.xml

echo "=== Visual assets successfully updated! ==="
file assets/*.png
