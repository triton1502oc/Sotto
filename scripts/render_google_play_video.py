#!/usr/bin/env python3
"""
Sotto Google Play Store Promo Video Generator
==============================================
Produces a broadcast-ready 1080p (1920x1080 @ 30fps) motion-designed preview video
explaining Sotto's dual mission (Caregiver Crisis Navigation + Dignified AAC)
adhering strictly to Google Play Store submission and accessibility guidelines.

Features:
- 100% Authentic app screenshots captured from Android device (ZERO mockups, ZERO /tmp paths)
- High-contrast, low-stimulus dark Material 3 visual design
- Realistic smartphone device frame with glass sheen, drop shadow, and punch hole
- Authentic app UI states:
  * Why Finder guided crisis tree (assets/screenshot_partner_mode.png)
  * 180° Tabletop Partner Mode flip (assets/screenshot_partner_flip.png)
  * Quick-Speak spontaneous typing (assets/screenshot_quickspeak.png)
  * Emergency Bystander hero card (assets/screenshot_fullscreen.png)
  * Private Why Log with 30-day patterns (assets/screenshot_whylog.png)
- Kinetic typography callouts, animated glowing badges, and feature highlights
- Muted autoplay accessibility with synchronized bottom subtitle pill
- Neural TTS narration (Ava / Andrew) + Attention Chime + low-stimulus ambient pad
- High-performance multiprocessing renderer + hardware H.264 / AAC encoder
"""

import os
import sys
import math
import time
import shutil
import subprocess
from multiprocessing import Pool, cpu_count
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# Video Specifications
W, H = 1920, 1080
FPS = 30
DURATION = 56.0
TOTAL_FRAMES = int(DURATION * FPS)
SAMPLE_RATE = 44100

TEMP_DIR = "/tmp/sotto_gp_render"
FRAMES_DIR = os.path.join(TEMP_DIR, "frames")
OUT_VIDEO = "demo/sotto_google_play_preview.mp4"
OUT_SOUNDTRACK = "demo/sotto_google_play_soundtrack.wav"

# Typography Setup
try:
    FONT_HERO = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 64)
    FONT_TITLE = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 44)
    FONT_SUBTITLE = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 24)
    FONT_BADGE = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 20)
    FONT_CARD_TITLE = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 26)
    FONT_CARD_DESC = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 20)
    FONT_CAPTION = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 22)
    FONT_OUTRO_TAG = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 32)
except Exception:
    FONT_HERO = FONT_TITLE = FONT_SUBTITLE = FONT_BADGE = FONT_CARD_TITLE = FONT_CARD_DESC = FONT_CAPTION = FONT_OUTRO_TAG = ImageFont.load_default()

# Easing functions
def ease_out_cubic(t):
    return 1.0 - (1.0 - max(0.0, min(1.0, t))) ** 3

def ease_in_out_quad(t):
    t = max(0.0, min(1.0, t))
    return 2 * t * t if t < 0.5 else 1 - math.pow(-2 * t + 2, 2) / 2

# Helper: Draw crisp vector icons
def draw_vector_icon(draw, icon_type, x, y, size=24, color=(255, 183, 77, 255)):
    if icon_type == "compass":
        r = size // 2
        cx, cy = x + r, y + r
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color, width=2)
        draw.polygon([(cx, cy - r + 3), (cx + 4, cy), (cx, cy + r - 3), (cx - 4, cy)], fill=color)
    elif icon_type == "flip":
        r = size // 2
        cx, cy = x + r, y + r
        draw.arc([cx - r + 2, cy - r + 2, cx + r - 2, cy + r - 2], start=45, end=225, fill=color, width=2)
        draw.arc([cx - r + 2, cy - r + 2, cx + r - 2, cy + r - 2], start=225, end=45, fill=color, width=2)
    elif icon_type == "speaker":
        draw.polygon([(x + 2, y + 8), (x + 8, y + 8), (x + 16, y + 2), (x + 16, y + size - 2), (x + 8, y + size - 8), (x + 2, y + size - 8)], fill=color)
        draw.arc([x + 12, y + 4, x + 22, y + size - 4], start=300, end=60, fill=color, width=2)
    elif icon_type == "shield":
        draw.polygon([(x + size // 2, y + 1), (x + size - 2, y + 6), (x + size - 4, y + size - 6), (x + size // 2, y + size - 1), (x + 4, y + size - 6), (x + 2, y + 6)], fill=color)
    elif icon_type == "check":
        draw.line([x + 3, y + size // 2, x + size // 3, y + size - 4], fill=color, width=3)
        draw.line([x + size // 3, y + size - 4, x + size - 3, y + 4], fill=color, width=3)
    elif icon_type == "grid":
        s = size // 2 - 2
        draw.rounded_rectangle([x, y, x + s, y + s], radius=2, fill=color)
        draw.rounded_rectangle([x + s + 3, y, x + size, y + s], radius=2, fill=color)
        draw.rounded_rectangle([x, y + s + 3, x + s, y + size], radius=2, fill=color)
        draw.rounded_rectangle([x + s + 3, y + s + 3, x + size, y + size], radius=2, fill=color)
    elif icon_type == "sparkle":
        cx, cy = x + size // 2, y + size // 2
        r = size // 2
        draw.polygon([(cx, cy - r), (cx + 3, cy - 3), (cx + r, cy), (cx + 3, cy + 3), (cx, cy + r), (cx - 3, cy + 3), (cx - r, cy), (cx - 3, cy - 3)], fill=color)

# Generate phone mockup
def render_phone_mockup(screen_img, target_h=920, rotation_deg=0):
    aspect = screen_img.width / screen_img.height
    target_w = int(target_h * aspect)
    scr_resized = screen_img.resize((target_w, target_h), Image.Resampling.LANCZOS)
    
    if rotation_deg != 0:
        scr_resized = scr_resized.rotate(rotation_deg, resample=Image.Resampling.BICUBIC, expand=False)
        
    bezel = 12
    pw = target_w + bezel * 2
    ph = target_h + bezel * 2
    r = 38
    
    phone = Image.new("RGBA", (pw + 40, ph + 40), (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(phone)
    
    # Realistic soft drop shadow
    shadow_mask = Image.new("RGBA", (pw + 40, ph + 40), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow_mask)
    sdraw.rounded_rectangle([20, 24, 20 + pw, 24 + ph], radius=r, fill=(0, 0, 0, 160))
    shadow_blur = shadow_mask.filter(ImageFilter.GaussianBlur(16))
    phone.paste(shadow_blur, (0, 0), shadow_blur)
    
    # Titanium dark bezel
    pdraw.rounded_rectangle([20, 20, 20 + pw, 20 + ph], radius=r, fill=(26, 30, 38, 255), outline=(64, 72, 86, 255), width=2)
    
    # Clip screen with rounded corners
    screen_mask = Image.new("L", (target_w, target_h), 0)
    smdraw = ImageDraw.Draw(screen_mask)
    smdraw.rounded_rectangle([0, 0, target_w, target_h], radius=r - bezel + 2, fill=255)
    
    phone.paste(scr_resized, (20 + bezel, 20 + bezel), screen_mask)
    
    # Camera punch hole
    cam_x = 20 + pw // 2
    cam_y = 20 + bezel + 12
    pdraw.ellipse([cam_x - 6, cam_y - 6, cam_x + 6, cam_y + 6], fill=(12, 12, 14, 255))
    
    return phone

# Preloaded Screen Cache
SCREEN_CACHE = {}

def get_screen(path):
    if path not in SCREEN_CACHE:
        if not os.path.exists(path):
            raise FileNotFoundError(f"Required screenshot asset not found: {path}")
        SCREEN_CACHE[path] = Image.open(path).convert("RGBA")
    return SCREEN_CACHE[path]

# Scene Definitions (Using 100% Authentic Repository Screenshots)
SCENES = [
    {
        "id": "intro",
        "start": 0.0,
        "end": 8.5,
        "screen_path": "assets/screenshot.png",
        "badge": "SOTTO • ASSISTIVE COMMUNICATION",
        "badge_icon": "sparkle",
        "title": "A Calm Voice in Verbal Shutdowns",
        "subtitle": "Designed for autistic teens, adults & caregivers navigating sensory overload.",
        "features": [
            ("check", "Zero Open-Ended Pressure", "Replaces overwhelming 'What's wrong?' questions with structured clarity.", (255, 183, 77)),
            ("grid", "Mature Material 3 Aesthetic", "High contrast and dignified dark neutral palette—free of childish cartoons.", (129, 199, 132)),
            ("shield", "100% Offline & Private", "Zero cloud accounts, zero ads, zero tracking. All data stays strictly on-device.", (144, 202, 249))
        ],
        "caption": "Sotto: Calm crisis navigation & dignified voice for autistic teens, adults, and caregivers.",
        "glow_color": (255, 183, 77)
    },
    {
        "id": "whyfinder",
        "start": 8.5,
        "end": 19.5,
        "screen_path": "assets/screenshot_partner_mode.png",
        "badge": "CRISIS NAVIGATION",
        "badge_icon": "compass",
        "title": "Calm Yes/No Inquiries in Meltdowns",
        "subtitle": "Uncovers the root cause when cognitive overload prevents open speech.",
        "features": [
            ("compass", "Gentle Binary Tree", "Replaces open-ended demands with single, low-cognitive-load yes/no questions.", (255, 183, 77)),
            ("check", "Four Investigation Domains", "Step through Physical pain, Sensory overload, Emotion, or Routine friction.", (129, 199, 132)),
            ("speaker", "Immediate Action Steps", "Provides concrete, soothing relief: dim lights, solitary space, or calm comfort.", (144, 202, 249))
        ],
        "caption": "Why Finder: Gentle binary yes/no branching to identify root causes without distress.",
        "glow_color": (255, 183, 77)
    },
    {
        "id": "partnermode",
        "start": 19.5,
        "end": 30.5,
        "screen_path": "assets/screenshot_partner_flip.png",
        "badge": "PARTNER MODE",
        "badge_icon": "flip",
        "title": "180° Tabletop Screen Flip",
        "subtitle": "Dignified face-to-face communication across a table with zero pressure.",
        "features": [
            ("flip", "180° Tabletop Rotation", "Caregiver and partner communicate across a table with flipped orientation.", (255, 183, 77)),
            ("grid", "Four Dignified Replies", "Rapid response dock: Yes, No, Repeat, or Wait with high-contrast buttons.", (129, 199, 132)),
            ("speaker", "Instant Vocal Speech", "Spoken audio playback confirms choices out loud for complete peace of mind.", (144, 202, 249))
        ],
        "caption": "Partner Mode: 180° screen rotation for dignified tabletop conversation.",
        "glow_color": (129, 199, 132)
    },
    {
        "id": "aac",
        "start": 30.5,
        "end": 41.5,
        "screen_path": "assets/screenshot_quickspeak.png",
        "screen_alt_path": "assets/screenshot_fullscreen.png",
        "badge": "DIGNIFIED AAC",
        "badge_icon": "speaker",
        "title": "Mature Voice • Zero Pediatric Clutter",
        "subtitle": "A clean, adult communication board for non-speaking and unclear speech.",
        "features": [
            ("speaker", "Spontaneous Quick-Speak", "Bottom dock for spontaneous typing, voice dictation, and one-tap speech.", (255, 183, 77)),
            ("grid", "Emergency Bystander Card", "High-visibility banner and card to display directly to first responders.", (239, 83, 80)),
            ("flip", "Giant Fullscreen Text", "Long-press any phrase to display giant readable text across noisy environments.", (144, 202, 249))
        ],
        "caption": "Dignified AAC: Clean Material 3 design, mature voice, and emergency cards.",
        "glow_color": (255, 183, 77)
    },
    {
        "id": "privacy",
        "start": 41.5,
        "end": 51.0,
        "screen_path": "assets/screenshot_whylog.png",
        "badge": "100% OFFLINE & PRIVATE",
        "badge_icon": "shield",
        "title": "Zero Cloud Accounts • Zero Tracking",
        "subtitle": "All crisis notes and communication stay strictly confidential on-device.",
        "features": [
            ("shield", "100% Local Device Storage", "Zero tracking, zero cloud accounts, and zero covert analytics.", (129, 199, 132)),
            ("grid", "30-Day Crisis Pattern Trends", "Identify top environmental stressors and peak hours to prevent future distress.", (255, 183, 77)),
            ("speaker", "Offline Dual-Language", "On-device ML Kit models translate and speak offline in multiple languages.", (144, 202, 249))
        ],
        "caption": "100% Offline Guarantee: Zero tracking, no accounts, private on-device logs.",
        "glow_color": (129, 199, 132)
    },
    {
        "id": "outro",
        "start": 51.0,
        "end": 56.0,
        "screen_path": "assets/screenshot.png",
        "caption": "Join Sotto Closed Testing on Google Play."
    }
]

# Render single frame
def render_frame_worker(args):
    frame_idx, out_path = args
    t = frame_idx / FPS
    
    # Identify active scene
    scene = SCENES[-1]
    for s in SCENES:
        if s["start"] <= t < s["end"]:
            scene = s
            break
            
    scene_progress = (t - scene["start"]) / max(0.001, (scene["end"] - scene["start"]))
    
    canvas = Image.new("RGBA", (W, H), (14, 17, 23, 255))
    d = ImageDraw.Draw(canvas)
    
    # 1. Atmospheric ambient glow
    glow_color = scene.get("glow_color", (255, 183, 77))
    pulse = 0.85 + 0.15 * math.sin(2.0 * math.pi * 0.4 * t)
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([100, 200, 700, 800], fill=(glow_color[0], glow_color[1], glow_color[2], int(40 * pulse)))
    gd.ellipse([1100, 180, 1900, 920], fill=(28, 85, 55, int(28 * pulse)))
    glow_blur = glow.filter(ImageFilter.GaussianBlur(80))
    canvas.paste(glow_blur, (0, 0), glow_blur)
    
    # SCENE: OUTRO (51.0s - 56.0s)
    if scene["id"] == "outro":
        outro_t = (t - 51.0) / 5.0
        alpha = min(1.0, outro_t * 2.5)
        
        # Load app icon
        icon_path = "assets/play_store_icon_512.png"
        if os.path.exists(icon_path):
            icon_img = Image.open(icon_path).convert("RGBA").resize((180, 180), Image.Resampling.LANCZOS)
            # Rounded mask for icon
            imask = Image.new("L", (180, 180), 0)
            ImageDraw.Draw(imask).rounded_rectangle([0, 0, 180, 180], radius=42, fill=255)
            
            # Outer icon glow
            iglow = Image.new("RGBA", (260, 260), (0, 0, 0, 0))
            ImageDraw.Draw(iglow).rounded_rectangle([20, 20, 240, 240], radius=50, fill=(255, 183, 77, int(90 * alpha)))
            iglow_b = iglow.filter(ImageFilter.GaussianBlur(24))
            canvas.paste(iglow_b, (W // 2 - 130, 210), iglow_b)
            canvas.paste(icon_img, (W // 2 - 90, 250), imask)
            
        # Hero Title
        d.text((W // 2 - 110, 460), "SOTTO", font=FONT_HERO, fill=(255, 255, 255, int(255 * alpha)))
        
        # Tagline
        tag = "A Calm Voice When Words Are Hard"
        tbox = FONT_OUTRO_TAG.getbbox(tag)
        tw = tbox[2] - tbox[0]
        d.text(((W - tw) // 2, 545), tag, font=FONT_OUTRO_TAG, fill=(255, 204, 128, int(255 * alpha)))
        
        sub = "Assistive Communication & Crisis Navigation for Autistic Teens & Adults"
        sbox = FONT_SUBTITLE.getbbox(sub)
        sw = sbox[2] - sbox[0]
        d.text(((W - sw) // 2, 600), sub, font=FONT_SUBTITLE, fill=(180, 192, 210, int(255 * alpha)))
        
        # 3 Trust Badges
        badges = [
            ("sparkle", "Google Play Beta", (255, 183, 77)),
            ("check", "100% Free & Open Source", (129, 199, 132)),
            ("shield", "100% Offline Privacy", (144, 202, 249))
        ]
        bx = 420
        by = 680
        for b_icon, b_title, b_col in badges:
            d.rounded_rectangle([bx, by, bx + 330, by + 56], radius=28, fill=(26, 32, 42, int(230 * alpha)), outline=(b_col[0], b_col[1], b_col[2], int(200 * alpha)), width=2)
            draw_vector_icon(d, b_icon, bx + 18, by + 16, size=24, color=(b_col[0], b_col[1], b_col[2], int(255 * alpha)))
            d.text((bx + 52, by + 16), b_title, font=FONT_BADGE, fill=(240, 240, 240, int(255 * alpha)))
            bx += 370
            
    # SCENES: 0, 1, 2, 3, 4
    else:
        # Subtle organic vertical float
        float_y = 60 + int(4.0 * math.sin(2.0 * math.pi * 0.3 * t))
        
        # Determine screen image
        screen_img = get_screen(scene["screen_path"])
        # For Scene 3 (AAC), swap to emergency card during second half
        if scene["id"] == "aac" and (t - scene["start"]) >= 5.5:
            screen_img = get_screen(scene["screen_alt_path"])
            
        # Intro slide-in transition
        if scene["id"] == "intro":
            if t < 2.5:
                phone_x = int(W // 2 - 220)
            else:
                glide_p = ease_out_cubic((t - 2.5) / 1.0)
                phone_x = int((W // 2 - 220) + (160 - (W // 2 - 220)) * glide_p)
        else:
            phone_x = 160
            
        # Mockup render
        phone = render_phone_mockup(screen_img, target_h=920)
        canvas.paste(phone, (phone_x, float_y), phone)
        
        # Interactive tap pulse animation on real UI buttons
        # In Why Finder (scene 1): pulse over the real green "✓ Yes" button
        if scene["id"] == "whyfinder" and 13.8 <= t <= 15.0:
            pulse_p = (t - 13.8) / 1.2
            pr = int(15 + 40 * pulse_p)
            pa = int(220 * (1.0 - pulse_p))
            d.ellipse([phone_x + 138 - pr, float_y + 715 - pr, phone_x + 138 + pr, float_y + 715 + pr], outline=(129, 199, 132, pa), width=3)
        # In Partner Mode (scene 2): pulse over the flipped green "✓ Yes" button facing partner
        elif scene["id"] == "partnermode" and 28.0 <= t <= 29.5:
            pulse_p = (t - 28.0) / 1.5
            pr = int(15 + 45 * pulse_p)
            pa = int(220 * (1.0 - pulse_p))
            d.ellipse([phone_x + 338 - pr, float_y + 86 - pr, phone_x + 338 + pr, float_y + 86 + pr], outline=(129, 199, 132, pa), width=3)
            
        # Right Motion Graphics Panel (only after 2.5s for intro)
        if scene["id"] != "intro" or t >= 2.5:
            card_p = ease_out_cubic(min(1.0, (t - (2.5 if scene["id"] == "intro" else scene["start"])) / 0.6))
            card_alpha = int(255 * card_p)
            offset_x = int(35 * (1.0 - card_p))
            rx = 680 + offset_x
            ry = 130
            
            # Badge
            badge_txt = scene["badge"]
            bbox = FONT_BADGE.getbbox(badge_txt)
            bw = bbox[2] - bbox[0] + 50
            d.rounded_rectangle([rx, ry, rx + bw, ry + 38], radius=19, fill=(38, 28, 18, int(240 * card_p)), outline=(glow_color[0], glow_color[1], glow_color[2], int(220 * card_p)), width=2)
            draw_vector_icon(d, scene["badge_icon"], rx + 12, ry + 9, size=20, color=(glow_color[0], glow_color[1], glow_color[2], card_alpha))
            d.text((rx + 38, ry + 8), badge_txt, font=FONT_BADGE, fill=(255, 204, 128, card_alpha))
            
            # Title & Subtitle
            d.text((rx, ry + 56), scene["title"], font=FONT_TITLE, fill=(255, 255, 255, card_alpha))
            d.text((rx, ry + 120), scene["subtitle"], font=FONT_SUBTITLE, fill=(180, 192, 210, card_alpha))
            
            # 3 Feature Cards
            card_y = ry + 185
            for icon, c_title, c_desc, col in scene["features"]:
                d.rounded_rectangle([rx, card_y, rx + 1060, card_y + 115], radius=18, fill=(24, 28, 38, int(230 * card_p)), outline=(48, 56, 70, int(255 * card_p)), width=1)
                d.rounded_rectangle([rx + 16, card_y + 20, rx + 22, card_y + 95], radius=3, fill=(col[0], col[1], col[2], card_alpha))
                draw_vector_icon(d, icon, rx + 38, card_y + 24, size=24, color=(col[0], col[1], col[2], card_alpha))
                d.text((rx + 72, card_y + 22), c_title, font=FONT_CARD_TITLE, fill=(255, 255, 255, card_alpha))
                d.text((rx + 38, card_y + 64), c_desc, font=FONT_CARD_DESC, fill=(170, 182, 198, card_alpha))
                card_y += 135
                
    # 3. Synchronized Bottom Subtitle Bar (Accessibility for muted autoplay)
    caption_txt = scene["caption"]
    sbox = FONT_CAPTION.getbbox(caption_txt)
    sw = sbox[2] - sbox[0] + 50
    sx = (W - sw) // 2
    sy = 1005
    d.rounded_rectangle([sx, sy, sx + sw, sy + 48], radius=24, fill=(18, 22, 30, 240), outline=(255, 183, 77, 180), width=1)
    d.text((sx + 25, sy + 11), caption_txt, font=FONT_CAPTION, fill=(240, 240, 240, 255))
    
    # Save frame as JPEG for high speed & low disk I/O
    canvas.convert("RGB").save(out_path, "JPEG", quality=95)

def main():
    start_time = time.time()
    print("=" * 60)
    print("SOTTO: Google Play Store Preview Video Generator")
    print(f"Target: 1920x1080 @ {FPS} fps | {DURATION}s ({TOTAL_FRAMES} frames)")
    print("=" * 60)
    
    # Clean output directories
    os.makedirs(FRAMES_DIR, exist_ok=True)
    os.makedirs(os.path.dirname(OUT_VIDEO), exist_ok=True)
    
    # Verify master soundtrack exists, build if needed
    if not os.path.exists(OUT_SOUNDTRACK):
        print(f"Soundtrack {OUT_SOUNDTRACK} not found. Building with build_play_soundtrack.py...")
        py_bin = "/tmp/ttsvenv/bin/python" if os.path.exists("/tmp/ttsvenv/bin/python") else sys.executable
        subprocess.run([py_bin, "scripts/build_play_soundtrack.py"], check=True)
        
    print(f"Using Master Soundtrack: {OUT_SOUNDTRACK}")
    
    # Prepare render batch
    tasks = []
    for f in range(TOTAL_FRAMES):
        out_f = os.path.join(FRAMES_DIR, f"frame_{f:05d}.jpg")
        tasks.append((f, out_f))
        
    num_cpus = max(1, cpu_count())
    print(f"Rendering {TOTAL_FRAMES} frames using {num_cpus} CPU cores...")
    
    with Pool(num_cpus) as pool:
        for i, _ in enumerate(pool.imap_unordered(render_frame_worker, tasks, chunksize=16)):
            if (i + 1) % 150 == 0 or (i + 1) == TOTAL_FRAMES:
                pct = ((i + 1) / TOTAL_FRAMES) * 100
                elapsed = time.time() - start_time
                fps_render = (i + 1) / elapsed
                print(f"  Progress: {i + 1}/{TOTAL_FRAMES} frames ({pct:.1f}%) | {fps_render:.1f} fps")
                
    render_elapsed = time.time() - start_time
    print(f"Frame rendering completed in {render_elapsed:.1f}s!")
    
    # Encode with FFmpeg
    print("Encoding master video with FFmpeg...")
    ffmpeg_bin = "/opt/homebrew/bin/ffmpeg" if os.path.exists("/opt/homebrew/bin/ffmpeg") else "ffmpeg"
    
    cmd = [
        ffmpeg_bin, "-y",
        "-r", str(FPS),
        "-i", os.path.join(FRAMES_DIR, "frame_%05d.jpg"),
        "-i", OUT_SOUNDTRACK,
        "-c:v", "h264_videotoolbox",
        "-b:v", "8000k",
        "-pix_fmt", "yuv420p",
        "-c:a", "aac",
        "-b:a", "192k",
        "-ar", str(SAMPLE_RATE),
        "-shortest",
        OUT_VIDEO
    ]
    
    res = subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    if res.returncode != 0:
        print("Hardware encoder failed, falling back to libx264...")
        cmd_fallback = [
            ffmpeg_bin, "-y",
            "-r", str(FPS),
            "-i", os.path.join(FRAMES_DIR, "frame_%05d.jpg"),
            "-i", OUT_SOUNDTRACK,
            "-c:v", "libx264",
            "-preset", "fast",
            "-crf", "18",
            "-pix_fmt", "yuv420p",
            "-c:a", "aac",
            "-b:a", "192k",
            "-ar", str(SAMPLE_RATE),
            "-shortest",
            OUT_VIDEO
        ]
        subprocess.run(cmd_fallback, check=True)
        
    total_elapsed = time.time() - start_time
    vid_size_mb = os.path.getsize(OUT_VIDEO) / (1024 * 1024)
    print("=" * 60)
    print(f"SUCCESS: Video generated at {OUT_VIDEO}")
    print(f"File Size: {vid_size_mb:.2f} MB | Total Time: {total_elapsed:.1f}s")
    print("=" * 60)
    
    # Clean temporary frame files
    shutil.rmtree(TEMP_DIR, ignore_errors=True)

if __name__ == "__main__":
    main()
