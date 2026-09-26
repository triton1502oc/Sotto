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

echo "=== Scene 1: Modern Two-Tier Quick-Speak Bar ==="
# Focus Quick Speak input field
$ADB shell input tap 303 2105
sleep 0.8

# Type phrase
$ADB shell input text "I%sneed%sa%smoment,%splease"
sleep 1.0

# Dismiss keyboard
$ADB shell input keyevent 4
sleep 0.8

# Tap 🔊 Speak button
log_event "speak_moment"
$ADB shell input tap 986 2253
sleep 2.5

# Tap ⛶ Fullscreen button
$ADB shell input tap 95 2253
sleep 2.0

# Dismiss fullscreen dialog
$ADB shell input keyevent 4
sleep 1.0

echo "=== Scene 2: Emergency Bystander Card & Dual-Language Fullscreen ==="
# Tap "Needs"
$ADB shell input tap 574 305
sleep 1.5

# Tap "Emergency"
$ADB shell input tap 325 305
sleep 1.5

# Long-press emergency card to open full-screen emergency
$ADB shell input swipe 540 620 540 620 800
sleep 2.0

# Tap Speak (English) button
log_event "speak_emergency_en"
$ADB shell input tap 294 2200
sleep 3.5

# Tap Speak (Indonesia) button
log_event "speak_emergency_id"
$ADB shell input tap 787 2200
sleep 3.5

# Dismiss emergency dialog
$ADB shell input keyevent 4
sleep 1.0

# Return to "All"
$ADB shell input tap 107 305
sleep 1.2

echo "=== Scene 3: Two-Way Caregiver Receptive Mode (180° Flip & Rapid Reply) ==="
# Tap 👂 icon in top bar
$ADB shell input tap 607 147
sleep 1.8

# Tap 📋 Questions sheet
$ADB shell input tap 364 159
sleep 1.5

# Select "Are you in pain?"
$ADB shell input tap 540 926
sleep 1.5

# Tap 🔄 Flip button (180-degree rotation for face-to-face partner)
$ADB shell input tap 126 159
sleep 2.0

# Tap ✓ Yes rapid response button (facing partner at top-right)
log_event "speak_caregiver_yes"
$ADB shell input tap 795 345
sleep 2.2

# Dismiss caregiver dialog
$ADB shell input keyevent 4
sleep 1.2

echo "=== Scene 4: Dual-Language TopBar Switcher & Card Speech ==="
# Tap ID in top bar switcher
$ADB shell input tap 491 147
sleep 1.5

# Tap "Please give me time." card while ID is active -> Speaks Indonesian
log_event "speak_time_id"
$ADB shell input tap 800 1010
sleep 2.5

# Switch back to EN
$ADB shell input tap 410 147
sleep 1.2

echo "=== Scene 5: Voice Customization & Two-Tone Attention Chime ==="
# Tap Settings gear icon in top bar
$ADB shell input tap 733 147
sleep 1.8

# Tap "Attention chime before speaking" switch
$ADB shell input tap 820 1024
sleep 1.2

# Scroll down to reveal Test Voice button
$ADB shell input swipe 540 1800 540 1200 400
sleep 1.2

# Tap "Test Voice" button
log_event "test_voice"
$ADB shell input tap 540 1743
sleep 3.5

# Tap "Done" to close settings
$ADB shell input tap 821 2211
sleep 1.5

log_event "end"
echo "=== English Demo Flow Complete ==="
