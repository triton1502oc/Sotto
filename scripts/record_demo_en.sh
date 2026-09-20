#!/usr/bin/env bash
set -e

ADB="${ADB:-$(which adb 2>/dev/null || echo "${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb")}"

echo "Configuring clean recording environment for English demo..."
$ADB shell settings put system show_touches 1
$ADB shell settings put system pointer_location 0
$ADB shell settings put global hide_error_dialogs 1
$ADB shell settings put secure accessibility_button_mode 0
$ADB shell settings put secure accessibility_enabled 0

# Set English preferences
cat << 'EOF' > /tmp/sotto_locale_prefs_en.xml
<?xml version="1.0" encoding="utf-8" standalone="yes" ?><map><string name="selected_language">en</string></map>
EOF

cat << 'EOF' > /tmp/sotto_prefs_en.xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="v2_migrated" value="true" />
    <string name="saved_phrases">[{&quot;text&quot;:&quot;I cannot speak right now. Please read my screen.&quot;,&quot;language&quot;:&quot;auto&quot;,&quot;isEmergency&quot;:true,&quot;category&quot;:&quot;Emergency&quot;},{&quot;text&quot;:&quot;Please give me time.&quot;,&quot;language&quot;:&quot;auto&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;Needs&quot;},{&quot;text&quot;:&quot;I need a quiet space.&quot;,&quot;language&quot;:&quot;auto&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;Needs&quot;},{&quot;text&quot;:&quot;I need to leave now.&quot;,&quot;language&quot;:&quot;auto&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;Needs&quot;},{&quot;text&quot;:&quot;Yes, please.&quot;,&quot;language&quot;:&quot;auto&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;Social&quot;},{&quot;text&quot;:&quot;No, thank you.&quot;,&quot;language&quot;:&quot;auto&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;Social&quot;},{&quot;text&quot;:&quot;Thank you.&quot;,&quot;language&quot;:&quot;auto&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;Social&quot;},{&quot;text&quot;:&quot;Hello.&quot;,&quot;language&quot;:&quot;auto&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;General&quot;},{&quot;text&quot;:&quot;Please repeat that.&quot;,&quot;language&quot;:&quot;auto&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;General&quot;}]</string>
</map>
EOF

$ADB push /tmp/sotto_locale_prefs_en.xml /data/local/tmp/sotto_locale_prefs.xml
$ADB push /tmp/sotto_prefs_en.xml /data/local/tmp/sotto_prefs.xml

$ADB shell "run-as com.amh.sotto cp /data/local/tmp/sotto_locale_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_locale_prefs.xml"
$ADB shell "run-as com.amh.sotto cp /data/local/tmp/sotto_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_prefs.xml"

# Generate English audio clips
python3 scripts/generate_audio.py

# Clean existing video on device
$ADB shell rm -f /sdcard/sotto_demo_en.mp4

# Relaunch Sotto cleanly
echo "Launching Sotto in English..."
$ADB shell am force-stop com.amh.sotto
sleep 1
$ADB shell am start -n com.amh.sotto/.MainActivity
sleep 2.5

echo "Starting screen recording (720x1600 @ 4Mbps)..."
$ADB shell "screenrecord --size 720x1600 --bit-rate 4000000 --time-limit 48 /sdcard/sotto_demo_en.mp4" &
REC_PID=$!
sleep 1.0

echo "Running English demo flow with audio event logging..."
chmod +x scripts/run_demo_flow_en.sh
./scripts/run_demo_flow_en.sh

echo "Finishing recording..."
sleep 2
$ADB shell pkill -2 screenrecord || true
sleep 3

echo "Pulling video..."
mkdir -p demo
$ADB pull /sdcard/sotto_demo_en.mp4 demo/sotto_demo_en_video.mp4

echo "Muxing video with synchronized audio..."
python3 scripts/mux_audio.py

# Optional: Copy to artifact directory if set
if [ -n "$ARTIFACT_DIR" ] && [ -d "$ARTIFACT_DIR" ]; then
    cp demo/sotto_demo_en.mp4 "$ARTIFACT_DIR/sotto_demo_en.mp4"
fi

# Clean up raw video on device and local intermediate files
$ADB shell rm -f /sdcard/sotto_demo_en.mp4
rm -f demo/sotto_demo_en_video.mp4 demo/sotto_soundtrack.wav demo/events.txt
rm -rf demo/audio_scratch

echo "English video with sound completed successfully: demo/sotto_demo_en.mp4"
ls -lh demo/sotto_demo_en.mp4
