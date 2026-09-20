#!/usr/bin/env bash
set -e

ADB="$HOME/Library/Android/sdk/platform-tools/adb"
EVENTS_FILE="demo/events_id.txt"
rm -f "$EVENTS_FILE"

# Record start time
START_TIME=$(python3 -c "import time; print(time.time())")

log_event() {
    local event_name="$1"
    python3 -c "import time; print(f'${event_name}:{time.time() - $START_TIME:.3f}')" >> "$EVENTS_FILE"
}

echo "=== Scene 1: Instant Quick Speak ==="
# Focus Quick Speak input field
$ADB shell input tap 350 2210
sleep 0.8

# Type phrase
$ADB shell input text "Tolong%sberi%ssaya%swaktu."
sleep 1.0

# Dismiss voice capsule and close keyboard
$ADB shell input tap 88 500 || true
sleep 0.5
$ADB shell input keyevent 4
sleep 0.8

# Tap 🔊 Speak button
log_event "speak_waktu_1"
$ADB shell input tap 986 2210
sleep 2.4

echo "=== Scene 2: Large Visual Presentation Mode ==="
# Tap ⛶ Fullscreen button
$ADB shell input tap 839 2210
sleep 2.5

# Tap Bicara button in fullscreen
log_event "speak_waktu_2"
$ADB shell input tap 540 2175
sleep 2.4

# Dismiss fullscreen dialog
$ADB shell input keyevent 4
sleep 1.0

echo "=== Scene 3: Phrase Categories & Emergency ==="
# Tap "Kebutuhan" (Needs)
$ADB shell input tap 618 305
sleep 1.8

# Tap "Sosial" (Social)
$ADB shell input tap 863 305
sleep 1.8

# Tap "Darurat" (Emergency)
$ADB shell input tap 364 305
sleep 1.8

# Long-press emergency card to open full-screen emergency
$ADB shell input swipe 540 600 540 600 900
sleep 2.5

# Tap Bicara button in emergency fullscreen
log_event "speak_darurat"
$ADB shell input tap 540 2175
sleep 3.8

# Dismiss emergency dialog
$ADB shell input keyevent 4
sleep 1.0

# Return to "Semua" (All)
$ADB shell input tap 141 305
sleep 1.5

echo "=== Scene 4: Edit Mode & Personalization ==="
# Tap "Ubah Daftar"
$ADB shell input tap 897 147
sleep 1.5

# Long-press & drag card to reorder
$ADB shell input swipe 280 1020 800 1020 900
sleep 2.0

echo "=== Scene 5: Voice Customization & Attention Chime ==="
# Tap "Suara" button in edit mode
$ADB shell input tap 753 147
sleep 2.0

# Tap "Nada perhatian sebelum bicara" switch
$ADB shell input tap 820 1340 || true
sleep 1.0

# Tap "Tes Suara" button
log_event "test_voice"
$ADB shell input tap 540 1552 || true
sleep 3.5

# Dismiss voice dialog (or tap Selesai at 804 2020)
$ADB shell input tap 804 2020 || $ADB shell input keyevent 4
sleep 1.0

# Tap "Selesai" to exit edit mode
$ADB shell input tap 935 147
sleep 2.5

log_event "end"
echo "=== Indonesian Demo Flow Complete ==="
