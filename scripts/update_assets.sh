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

echo "Waiting for device to complete boot..."
while [ "$($ADB -s "$DEVICE_ID" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
    sleep 1
done

# Ensure device screen is on and keyguard dismissed
$ADB -s "$DEVICE_ID" shell input keyevent 224 || true
$ADB -s "$DEVICE_ID" shell wm dismiss-keyguard || true
$ADB -s "$DEVICE_ID" shell input keyevent 82 || true

# Select the appropriate APK matching the device ABI
DEVICE_ABI="$($ADB -s "$DEVICE_ID" shell getprop ro.product.cpu.abi 2>/dev/null | tr -d '\r' || true)"
APK_PATH="$(find app/build/outputs/apk/debug -name "*${DEVICE_ABI}*debug.apk" | head -n 1)"
if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
    APK_PATH="$(find app/build/outputs/apk/debug -name "*debug.apk" ! -name "*-arm*" ! -name "*-x86*" | head -n 1)"
fi
if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
    APK_PATH="$(find app/build/outputs/apk/debug -name "*.apk" | head -n 1)"
fi
echo "Using APK ($DEVICE_ABI): $APK_PATH"

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

cat << 'EOF' > /tmp/sotto_voice_prefs_en.xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <float name="speech_rate" value="1.0" />
    <float name="speech_pitch" value="1.0" />
    <string name="secondary_language">id</string>
    <boolean name="show_language_switcher" value="true" />
    <boolean name="attention_chime" value="false" />
</map>
EOF

$ADB -s "$DEVICE_ID" install -r "$APK_PATH"

$ADB -s "$DEVICE_ID" push /tmp/sotto_locale_prefs_en.xml /data/local/tmp/sotto_locale_prefs.xml
$ADB -s "$DEVICE_ID" push /tmp/sotto_prefs_en.xml /data/local/tmp/sotto_prefs.xml
$ADB -s "$DEVICE_ID" push /tmp/sotto_voice_prefs_en.xml /data/local/tmp/sotto_voice_prefs.xml
$ADB -s "$DEVICE_ID" shell "run-as com.amh.sotto mkdir -p /data/data/com.amh.sotto/shared_prefs" || true
$ADB -s "$DEVICE_ID" shell "run-as com.amh.sotto cp /data/local/tmp/sotto_locale_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_locale_prefs.xml"
$ADB -s "$DEVICE_ID" shell "run-as com.amh.sotto cp /data/local/tmp/sotto_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_prefs.xml"
$ADB -s "$DEVICE_ID" shell "run-as com.amh.sotto cp /data/local/tmp/sotto_voice_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_voice_prefs.xml"

echo "=== 4. Capturing Main Screen Screenshot ==="
$ADB -s "$DEVICE_ID" shell am force-stop com.amh.sotto
sleep 1
$ADB -s "$DEVICE_ID" shell am start -n com.amh.sotto/.MainActivity
# Wait 5s for cold start and TTS initialization badge to clear
sleep 5
$ADB -s "$DEVICE_ID" exec-out screencap -p > /tmp/raw_main_screen.png

echo "=== 5. Capturing Fullscreen Emergency Screenshot ==="
# Long-press the emergency card (center: 540, 550)
$ADB -s "$DEVICE_ID" shell input swipe 540 550 540 550 1200
sleep 2.0
$ADB -s "$DEVICE_ID" exec-out screencap -p > /tmp/raw_fullscreen.png
# Dismiss fullscreen dialog via '✕' close button at (954, 193)
$ADB -s "$DEVICE_ID" shell input tap 954 193
sleep 1.0

echo "=== 6. Capturing Edit Phrase Dialog Screenshot ==="
# Tap "Edit List" in top bar (center: 860, 145)
$ADB -s "$DEVICE_ID" shell input tap 860 145
sleep 1.5
# Tap second card ("I need a quiet space.") to edit (center: 799, 1014)
$ADB -s "$DEVICE_ID" shell input tap 799 1014
sleep 1.5
# Tap "Alternate spoken text" to expand it (center: 400, 1060)
$ADB -s "$DEVICE_ID" shell input tap 400 1060
sleep 1.0
$ADB -s "$DEVICE_ID" exec-out screencap -p > /tmp/raw_edit_phrase.png
# Dismiss edit dialog
$ADB -s "$DEVICE_ID" shell input keyevent 4
sleep 1.0

echo "=== 7. Capturing Voice & Language Settings Screenshot ==="
# Tap Settings gear icon (permanent top bar icon, center: 680, 145)
$ADB -s "$DEVICE_ID" shell input tap 680 145
sleep 2.0
$ADB -s "$DEVICE_ID" exec-out screencap -p > /tmp/raw_voice_settings.png
# Dismiss settings dialog
$ADB -s "$DEVICE_ID" shell input keyevent 4
sleep 1.0
# Tap "Done" to exit edit mode (center: 880, 145)
$ADB -s "$DEVICE_ID" shell input tap 880 145
sleep 1.0

echo "=== 8. Processing and Formatting Assets ==="
python3 - << 'PYEOF'
import os
from PIL import Image, ImageDraw

crop_box = (0, 65, 1080, 65 + 2280)

# 1. Process 4 phone screenshots (528x1024 RGB)
screens = [
    ('raw_main_screen.png', 'assets/screenshot.png'),
    ('raw_fullscreen.png', 'assets/screenshot_fullscreen.png'),
    ('raw_edit_phrase.png', 'assets/screenshot_edit_phrase.png'),
    ('raw_voice_settings.png', 'assets/screenshot_voice_settings.png'),
]

for raw_name, out_name in screens:
    raw_path = os.path.join('/tmp', raw_name)
    if os.path.exists(raw_path):
        raw_img = Image.open(raw_path).convert('RGB')
        screen = raw_img.crop(crop_box).resize((528, 1024), Image.Resampling.LANCZOS)
        screen.save(out_name)
        print(f"Updated {out_name} (528x1024 RGB)")

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
    echo "=== 9. Stopping Emulator ==="
    if command -v android &>/dev/null; then
        android emulator stop "$DEVICE_ID" || true
    else
        $ADB -s "$DEVICE_ID" emu kill || true
    fi
fi

# Cleanup temp files
rm -f /tmp/raw_main_screen.png /tmp/raw_fullscreen.png /tmp/raw_edit_phrase.png /tmp/raw_voice_settings.png /tmp/sotto_locale_prefs_en.xml /tmp/sotto_prefs_en.xml /tmp/sotto_voice_prefs_en.xml

echo "=== Visual assets successfully updated! ==="
file assets/*.png
