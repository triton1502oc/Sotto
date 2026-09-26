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
cat << 'PREF_EOF' > /tmp/sotto_locale_prefs_id.xml
<?xml version="1.0" encoding="utf-8" standalone="yes" ?><map><string name="selected_language">id</string></map>
PREF_EOF

cat << 'PREF_EOF' > /tmp/sotto_voice_prefs_id.xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="show_language_switcher" value="true" />
    <string name="secondary_language">en</string>
    <boolean name="attention_chime" value="false" />
    <float name="speech_rate" value="1.0" />
    <float name="speech_pitch" value="1.0" />
</map>
PREF_EOF

cat << 'PREF_EOF' > /tmp/sotto_prefs_id.xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="v2_migrated" value="true" />
    <string name="saved_phrases">[{"text":"Saya tidak bisa bicara sekarang. Tolong baca layar saya.","spokenText":"I cannot speak right now. Please read my screen.","spokenLanguage":"en","language":"id","isEmergency":true,"category":"Emergency"},{"text":"Tolong beri saya waktu.","spokenText":"Please give me time.","spokenLanguage":"en","language":"id","isEmergency":false,"category":"Needs"},{"text":"Saya butuh tempat tenang.","spokenText":"I need a quiet space.","spokenLanguage":"en","language":"id","isEmergency":false,"category":"Needs"},{"text":"Saya harus pergi sekarang.","spokenText":"I need to leave now.","spokenLanguage":"en","language":"id","isEmergency":false,"category":"Needs"},{"text":"Ya, silakan.","spokenText":"Yes, please.","spokenLanguage":"en","language":"id","isEmergency":false,"category":"Social"},{"text":"Tidak, terima kasih.","spokenText":"No, thank you.","spokenLanguage":"en","language":"id","isEmergency":false,"category":"Social"},{"text":"Terima kasih.","spokenText":"Thank you.","spokenLanguage":"en","language":"id","isEmergency":false,"category":"Social"},{"text":"Halo.","spokenText":"Hello.","spokenLanguage":"en","language":"id","isEmergency":false,"category":"General"},{"text":"Tolong ulangi.","spokenText":"Please repeat that.","spokenLanguage":"en","language":"id","isEmergency":false,"category":"General"}]</string>
</map>
PREF_EOF

$ADB push /tmp/sotto_locale_prefs_id.xml /data/local/tmp/sotto_locale_prefs.xml
$ADB push /tmp/sotto_voice_prefs_id.xml /data/local/tmp/sotto_voice_prefs.xml
$ADB push /tmp/sotto_prefs_id.xml /data/local/tmp/sotto_prefs.xml

$ADB shell "run-as com.amh.sotto cp /data/local/tmp/sotto_locale_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_locale_prefs.xml"
$ADB shell "run-as com.amh.sotto cp /data/local/tmp/sotto_voice_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_voice_prefs.xml"
$ADB shell "run-as com.amh.sotto cp /data/local/tmp/sotto_prefs.xml /data/data/com.amh.sotto/shared_prefs/sotto_prefs.xml"

# Generate Indonesian audio clips
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
$ADB shell "screenrecord --size 720x1600 --bit-rate 4000000 --time-limit 65 /sdcard/sotto_demo_id.mp4" &
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

echo "Muxing video with synchronized audio..."
python3 scripts/mux_audio_id.py

# Clean up raw video on device and local intermediate files
$ADB shell rm -f /sdcard/sotto_demo_id.mp4
rm -f demo/sotto_demo_id_video.mp4 demo/sotto_soundtrack_id.wav demo/events_id.txt
rm -rf demo/audio_scratch_id

echo "Indonesian video with sound completed successfully: demo/sotto_demo_id.mp4"
ls -lh demo/sotto_demo_id.mp4
