#!/usr/bin/env bash
set -e

ADB="${ADB:-$(which adb 2>/dev/null || echo "${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb")}"
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
$ADB shell input tap 303 2242
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
$ADB shell input tap 986 2243
sleep 2.4

echo "=== Scene 2: Large Visual Presentation Mode ==="
# Tap ⛶ Fullscreen button
$ADB shell input tap 839 2243
sleep 2.5

# Tap Speak Aloud button in fullscreen
log_event "speak_moment_2"
$ADB shell input tap 540 2175
sleep 2.4

# Dismiss fullscreen dialog
$ADB shell input keyevent 4
sleep 1.0

echo "=== Scene 3: Category Filtering & Emergency Dual-Language ==="
# Tap "Needs"
$ADB shell input tap 574 305
sleep 1.8

# Tap "Social"
$ADB shell input tap 783 305
sleep 1.8

# Tap "Emergency"
$ADB shell input tap 325 305
sleep 1.8

# Long-press emergency card to open full-screen emergency
$ADB shell input swipe 540 620 540 621 800
sleep 2.5

# Tap Speak (English) button
log_event "speak_emergency_en"
$ADB shell input tap 294 2200
sleep 3.2

# Tap Speak (Indonesia) button
log_event "speak_emergency_id"
$ADB shell input tap 787 2200
sleep 3.5

# Dismiss emergency dialog
$ADB shell input keyevent 4
sleep 1.0

# Return to "All"
$ADB shell input tap 107 305
sleep 1.5

echo "=== Scene 4: Dual-Language TopBar Switcher & Card Taps ==="
# Tap phrase card while EN is active -> Speaks English
log_event "speak_time_en"
$ADB shell input tap 281 1080
sleep 2.4

# Tap ID in top bar switcher
$ADB shell input tap 778 147
sleep 1.5

# Tap phrase card again while ID is active -> Speaks Indonesian
log_event "speak_time_id"
$ADB shell input tap 281 1080
sleep 2.4

# Switch back to EN
$ADB shell input tap 696 147
sleep 1.0

echo "=== Scene 5: Voice Customization & Attention Chime ==="
# Tap "Edit List"
$ADB shell input tap 929 147
sleep 1.5

# Tap "Voice" button in edit mode
$ADB shell input tap 786 147
sleep 2.0

# Tap "Attention chime before speaking" switch
$ADB shell input tap 820 1340 || true
sleep 1.0

# Tap "Test Voice" button
log_event "test_voice"
$ADB shell input tap 540 1550 || true
sleep 3.5

# Dismiss voice dialog
$ADB shell input keyevent 4
sleep 1.0

# Tap "Done" to exit edit mode
$ADB shell input tap 951 147
sleep 2.5

log_event "end"
echo "=== English Demo Flow Complete ==="
