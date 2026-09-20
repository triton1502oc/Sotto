#!/usr/bin/env bash
set -e

ADB="$HOME/Library/Android/sdk/platform-tools/adb"
EVENTS_FILE="demo/events.txt"
rm -f "$EVENTS_FILE"

# Record start time
START_TIME=$(python3 -c "import time; print(time.time())")

log_event() {
    local event_name="$1"
    python3 -c "import time; print(f'${event_name}:{time.time() - $START_TIME:.3f}')" >> "$EVENTS_FILE"
}

echo "=== Scene 1: Instant Quick Speak ==="
# Focus Quick Speak input field
$ADB shell input tap 390 2210
sleep 0.8

# Type phrase
$ADB shell input text "I%sneed%sa%smoment,%splease"
sleep 1.0

# Dismiss voice capsule and close keyboard
$ADB shell input tap 88 500 || true
sleep 0.5
$ADB shell input keyevent 4
sleep 0.8

# Tap 🔊 Speak button
log_event "speak_moment_1"
$ADB shell input tap 980 2210
sleep 2.2

echo "=== Scene 2: Large Visual Presentation Mode ==="
# Tap ⛶ Fullscreen button
$ADB shell input tap 815 2210
sleep 2.5

# Tap Speak Aloud button in fullscreen
log_event "speak_moment_2"
$ADB shell input tap 540 2175
sleep 2.2

# Dismiss fullscreen dialog
$ADB shell input keyevent 4
sleep 1.0

echo "=== Scene 3: Phrase Categories & Emergency ==="
# Tap "Needs"
$ADB shell input tap 530 310
sleep 1.8

# Tap "Social"
$ADB shell input tap 720 310
sleep 1.8

# Tap "Emergency"
$ADB shell input tap 300 310
sleep 1.8

# Long-press emergency card to open full-screen emergency
$ADB shell input swipe 540 580 540 580 900
sleep 2.5

# Tap Speak Aloud button in emergency fullscreen
log_event "speak_emergency"
$ADB shell input tap 540 2175
sleep 3.5

# Dismiss emergency dialog
$ADB shell input keyevent 4
sleep 1.0

# Return to "All"
$ADB shell input tap 100 310
sleep 1.5

echo "=== Scene 4: Edit Mode & Personalization ==="
# Tap "Edit List"
$ADB shell input tap 870 150
sleep 1.5

# Long-press & drag card to reorder
$ADB shell input swipe 280 1020 800 1020 900
sleep 2.0

echo "=== Scene 5: Voice Customization & Attention Chime ==="
# Tap "Voice" button in edit mode
$ADB shell input tap 720 150
sleep 2.0

# Tap "Attention chime before speaking" switch
$ADB shell input tap 820 1320 || true
sleep 1.0

# Tap "Test Voice" button
log_event "test_voice"
$ADB shell input tap 540 1500 || true
sleep 3.0

# Dismiss voice dialog
$ADB shell input keyevent 4
sleep 1.0

# Tap "Done" to exit edit mode
$ADB shell input tap 870 150
sleep 2.5

log_event "end"
echo "=== English Demo Flow Complete ==="
