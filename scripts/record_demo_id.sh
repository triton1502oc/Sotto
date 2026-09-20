#!/usr/bin/env bash
set -e

ADB="${ADB:-$(which adb 2>/dev/null || echo "${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb")}"

echo "Configuring clean recording environment for Indonesian demo..."
$ADB shell settings put system show_touches 1
$ADB shell settings put system pointer_location 0
$ADB shell settings put global hide_error_dialogs 1
$ADB shell settings put secure accessibility_button_mode 0
$ADB shell settings put secure accessibility_enabled 0

# Set Indonesian preferences with dual-language enabled
cat << 'EOF' > /tmp/sotto_locale_prefs_id.xml
<?xml version="1.0" encoding="utf-8" standalone="yes" ?><map><string name="selected_language">id</string></map>
EOF

cat << 'EOF' > /tmp/sotto_voice_prefs_id.xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="show_language_switcher" value="true" />
    <string name="secondary_language">en</string>
    <boolean name="attention_chime" value="false" />
    <float name="speech_rate" value="1.0" />
    <float name="speech_pitch" value="1.0" />
</map>
EOF

cat << 'EOF' > /tmp/sotto_prefs_id.xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="v2_migrated" value="true" />
    <string name="saved_phrases">[{&quot;text&quot;:&quot;Saya tidak bisa bicara sekarang. Tolong baca layar saya.&quot;,&quot;spokenText&quot;:&quot;I cannot speak right now. Please read my screen.&quot;,&quot;spokenLanguage&quot;:&quot;en&quot;,&quot;language&quot;:&quot;id&quot;,&quot;isEmergency&quot;:true,&quot;category&quot;:&quot;Emergency&quot;},{&quot;text&quot;:&quot;Tolong beri saya waktu.&quot;,&quot;spokenText&quot;:&quot;Please give me time.&quot;,&quot;spokenLanguage&quot;:&quot;en&quot;,&quot;language&quot;:&quot;id&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;Needs&quot;},{&quot;text&quot;:&quot;Saya butuh tempat tenang.&quot;,&quot;spokenText&quot;:&quot;I need a quiet space.&quot;,&quot;spokenLanguage&quot;:&quot;en&quot;,&quot;language&quot;:&quot;id&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;Needs&quot;},{&quot;text&quot;:&quot;Saya harus pergi sekarang.&quot;,&quot;spokenText&quot;:&quot;I need to leave now.&quot;,&quot;spokenLanguage&quot;:&quot;en&quot;,&quot;language&quot;:&quot;id&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;Needs&quot;},{&quot;text&quot;:&quot;Ya, silakan.&quot;,&quot;spokenText&quot;:&quot;Yes, please.&quot;,&quot;spokenLanguage&quot;:&quot;en&quot;,&quot;language&quot;:&quot;id&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;Social&quot;},{&quot;text&quot;:&quot;Tidak, terima kasih.&quot;,&quot;spokenText&quot;:&quot;No, thank you.&quot;,&quot;spokenLanguage&quot;:&quot;en&quot;,&quot;language&quot;:&quot;id&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;Social&quot;},{&quot;text&quot;:&quot;Terima kasih.&quot;,&quot;spokenText&quot;:&quot;Thank you.&quot;,&quot;spokenLanguage&quot;:&quot;en&quot;,&quot;language&quot;:&quot;id&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;Social&quot;},{&quot;text&quot;:&quot;Halo.&quot;,&quot;spokenText&quot;:&quot;Hello.&quot;,&quot;spokenLanguage&quot;:&quot;en&quot;,&quot;language&quot;:&quot;id&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;General&quot;},{&quot;text&quot;:&quot;Tolong ulangi.&quot;,&quot;spokenText&quot;:&quot;Please repeat that.&quot;,&quot;spokenLanguage&quot;:&quot;en&quot;,&quot;language&quot;:&quot;id&quot;,&quot;isEmergency&quot;:false,&quot;category&quot;:&quot;General&quot;}]</string>
</map>
EOF

$ADB push /tmp/sotto_locale_prefs_id.xml /data/local/tmp/sotto_locale_prefs.xml
$ADB push /tmp/sotto_voice_prefs_id.xml /data/local/tmp/sotto_voice_prefs.xml
$ADB push /tmp/sotto_prefs_id.xml /data/local/tmp/sotto_prefs.xml

$ADB shell "run-as com.amh.sotto cp /data/local/tmp/sotto_locale_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_locale_prefs.xml"
$ADB shell "run-as com.amh.sotto cp /data/local/tmp/sotto_voice_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_voice_prefs.xml"
$ADB shell "run-as com.amh.sotto cp /data/local/tmp/sotto_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_prefs.xml"

# Generate Indonesian and dual-language audio clips
python3 scripts/generate_audio_id.py

# Clean existing video on device
$ADB shell rm -f /sdcard/sotto_demo_id.mp4

# Relaunch Sotto cleanly
echo "Launching Sotto in Indonesian..."
$ADB shell am force-stop com.amh.sotto
sleep 1
$ADB shell am start -n com.amh.sotto/.MainActivity
sleep 2.5

echo "Starting screen recording (720x1600 @ 4Mbps)..."
$ADB shell "screenrecord --size 720x1600 --bit-rate 4000000 --time-limit 180 /sdcard/sotto_demo_id.mp4" &
REC_PID=$!
sleep 1.0

echo "Running Indonesian demo flow with audio event logging..."
chmod +x scripts/run_demo_flow_id.sh
./scripts/run_demo_flow_id.sh

echo "Finishing recording..."
sleep 2
$ADB shell pkill -2 screenrecord || true
sleep 3

echo "Pulling video..."
mkdir -p demo
$ADB pull /sdcard/sotto_demo_id.mp4 demo/sotto_demo_id_video.mp4

echo "Muxing video with synchronized Indonesian audio..."
python3 scripts/mux_audio_id.py

# Optional: Copy to artifact directory if set
if [ -n "$ARTIFACT_DIR" ] && [ -d "$ARTIFACT_DIR" ]; then
    cp demo/sotto_demo_id.mp4 "$ARTIFACT_DIR/sotto_demo_id.mp4"
fi

# Clean up raw video on device and local intermediate files
$ADB shell rm -f /sdcard/sotto_demo_id.mp4
rm -f demo/sotto_demo_id_video.mp4 demo/sotto_soundtrack_id.wav demo/events_id.txt
rm -rf demo/audio_scratch_id

echo "Indonesian video with sound completed successfully: demo/sotto_demo_id.mp4"
ls -lh demo/sotto_demo_id.mp4
