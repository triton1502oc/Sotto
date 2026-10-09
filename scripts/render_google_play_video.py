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
  * Main AAC phrase board with Bystander Safety Card (assets/screenshot.png)
  * Bystander hero card in fullscreen mode (assets/screenshot_fullscreen.png)
  * Sensory Color Themes & Dual-Engine TTS (assets/screenshot_voice_settings.png)
  * Private Why Log with 30-day patterns (assets/screenshot_whylog.png)
- Kinetic typography callouts, animated glowing badges, and feature highlights
- Muted autoplay accessibility with synchronized bottom subtitle pill
- Neural baritone TTS narration + Attention Chime + low-stimulus ambient pad
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
DURATION = 60.0
TOTAL_FRAMES = int(DURATION * FPS)
SAMPLE_RATE = 44100

TEMP_DIR = "/tmp/sotto_gp_render"
FRAMES_DIR = os.path.join(TEMP_DIR, "frames")
OUT_VIDEO = "demo/sotto_google_play_preview.mp4"
OUT_SOUNDTRACK = "demo/sotto_google_play_soundtrack.wav"

# Typography Setup
try:
    FONT_HERO = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 68)
    FONT_TITLE = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 42)
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
    elif icon_type == "palette":
        r = size // 2
        cx, cy = x + r, y + r
        draw.ellipse([x + 2, y + 2, x + size - 2, y + size - 2], outline=color, width=2)
        draw.ellipse([cx - 4, cy - 6, cx - 1, cy - 3], fill=color)
        draw.ellipse([cx + 3, cy - 4, cx + 6, cy - 1], fill=color)
        draw.ellipse([cx + 2, cy + 3, cx + 5, cy + 6], fill=color)

# Fit screenshot to target aspect ratio (tw x th) with subtle center crop
def fit_screen(img, tw=440, th=920):
    src_ratio = img.width / img.height
    dst_ratio = tw / th
    if src_ratio > dst_ratio:
        scale = th / img.height
        new_w = int(img.width * scale)
        scaled = img.resize((new_w, th), Image.Resampling.LANCZOS)
        left = (new_w - tw) // 2
        return scaled.crop((left, 0, left + tw, th))
    else:
        scale = tw / img.width
        new_h = int(img.height * scale)
        scaled = img.resize((tw, new_h), Image.Resampling.LANCZOS)
        top = (new_h - th) // 2
        return scaled.crop((0, top, tw, top + th))

# Generate phone mockup
def render_phone_mockup(screen_img, tw=440, th=920):
    bezel = 12
    pw = tw + bezel * 2
    ph = th + bezel * 2
    r = 38
    
    phone = Image.new("RGBA", (pw + 40, ph + 40), (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(phone)
    
    # Layer 1: Ambient deep elevation shadow
    shadow_mask = Image.new("RGBA", (pw + 40, ph + 40), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow_mask)
    sdraw.rounded_rectangle([20, 24, 20 + pw, 24 + ph], radius=r, fill=(0, 0, 0, 140))
    shadow_blur = shadow_mask.filter(ImageFilter.GaussianBlur(18))
    phone.paste(shadow_blur, (0, 0), shadow_blur)

    # Layer 2: Tight contact shadow
    shadow_contact = Image.new("RGBA", (pw + 40, ph + 40), (0, 0, 0, 0))
    scdraw = ImageDraw.Draw(shadow_contact)
    scdraw.rounded_rectangle([20, 22, 20 + pw, 22 + ph], radius=r, fill=(0, 0, 0, 110))
    shadow_contact_b = shadow_contact.filter(ImageFilter.GaussianBlur(6))
    phone.paste(shadow_contact_b, (0, 0), shadow_contact_b)
    
    # Titanium dark bezel
    pdraw.rounded_rectangle([20, 20, 20 + pw, 20 + ph], radius=r, fill=(24, 28, 36, 255), outline=(58, 66, 80, 255), width=2)
    
    # Clip screen with rounded corners
    screen_mask = Image.new("L", (tw, th), 0)
    smdraw = ImageDraw.Draw(screen_mask)
    smdraw.rounded_rectangle([0, 0, tw, th], radius=r - bezel + 2, fill=255)
    
    phone.paste(screen_img, (20 + bezel, 20 + bezel), screen_mask)
    
    # Camera punch hole
    cam_x = 20 + pw // 2
    cam_y = 20 + bezel + 12
    pdraw.ellipse([cam_x - 6, cam_y - 6, cam_x + 6, cam_y + 6], fill=(10, 10, 12, 255))
    pdraw.ellipse([cam_x - 2, cam_y - 2, cam_x + 2, cam_y + 2], fill=(25, 30, 42, 255))
    
    return phone

# Preloaded Screen Cache (Pre-fitted to tw x th)
SCREEN_CACHE = {}

def get_screen(path, tw=440, th=920):
    key = (path, tw, th)
    if key not in SCREEN_CACHE:
        if not os.path.exists(path):
            raise FileNotFoundError(f"Required screenshot asset not found: {path}")
        raw = Image.open(path).convert("RGBA")
        SCREEN_CACHE[key] = fit_screen(raw, tw, th)
    return SCREEN_CACHE[key]

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
        "subtitle": "Low-stimulus, dignified communication for autistic teens & adults.",
        "features": [
            ("check", "Dignified Adult Interface", "High-contrast Material 3 design—free of childish cartoons and clutter.", (255, 183, 77)),
            ("speaker", "One-Tap Spoken Voice", "Instant vocal speech with two-tone attention chime for quiet settings.", (129, 199, 132)),
            ("shield", "Bystander Safety Hero Card", "Always-ready emergency card for immediate reassurance and contact.", (144, 202, 249))
        ],
        "caption": "Sotto: Low-stimulus assistive communication and calm crisis de-escalation.",
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
            ("compass", "Gentle Binary Tree", "Replaces stressful “What's wrong?” with one simple question at a time.", (255, 183, 77)),
            ("grid", "4 Distress Domains", "Systematically explores Physical, Sensory, Emotional, or Routine triggers.", (129, 199, 132)),
            ("sparkle", "Actionable Relief Steps", "Suggests immediate de-escalation: dim lights, quiet space, or comfort.", (144, 202, 249))
        ],
        "caption": "Why Finder: Low-cognitive-load binary questions to identify distress calmly.",
        "glow_color": (129, 199, 132)
    },
    {
        "id": "partnermode",
        "start": 19.5,
        "end": 31.0,
        "screen_path": "assets/screenshot_partner_flip.png",
        "badge": "PARTNER MODE",
        "badge_icon": "flip",
        "title": "180° Tabletop Screen Flip",
        "subtitle": "Dignified face-to-face conversation across any table with zero pressure.",
        "features": [
            ("flip", "180° Tabletop Rotation", "Rest the phone between you; communicator interface flips upside down.", (255, 183, 77)),
            ("grid", "4-Card Rapid Response Dock", "Large high-contrast touch targets: Yes, No, Repeat, and Wait.", (129, 199, 132)),
            ("speaker", "Audible Speech Feedback", "Tapping any response speaks clearly aloud for complete mutual certainty.", (144, 202, 249))
        ],
        "caption": "Partner Mode: 180° tabletop screen flip for calm face-to-face communication.",
        "glow_color": (144, 202, 249)
    },
    {
        "id": "aac",
        "start": 31.0,
        "end": 42.0,
        "screen_path": "assets/screenshot.png",
        "screen_alt_path": "assets/screenshot_fullscreen.png",
        "crossfade_at": 35.5,
        "badge": "DIGNIFIED AAC & SAFETY",
        "badge_icon": "speaker",
        "title": "One-Tap Speech & Safety Hero Card",
        "subtitle": "Express needs in noisy environments or display safety cards to bystanders.",
        "features": [
            ("speaker", "Spontaneous Quick-Speak", "Bottom dock for spontaneous typing, voice dictation, and saved phrases.", (255, 183, 77)),
            ("shield", "Bystander Safety Card", "High-contrast card for first responders when words cannot be formed.", (239, 83, 80)),
            ("flip", "Giant Fullscreen Display", "Show giant high-contrast text across crowded rooms or noisy spaces.", (129, 199, 132))
        ],
        "caption": "Dignified AAC: Quick-Speak typing, attention chimes, and giant safety cards.",
        "glow_color": (255, 183, 77)
    },
    {
        "id": "sensory_privacy",
        "start": 42.0,
        "end": 53.0,
        "screen_path": "assets/screenshot_voice_settings.png",
        "screen_alt_path": "assets/screenshot_whylog.png",
        "crossfade_at": 47.5,
        "badge": "SENSORY THEMES & 100% OFFLINE",
        "badge_icon": "palette",
        "title": "Sensory Themes • Zero Cloud • Zero Ads",
        "subtitle": "Low-stimulus palettes and absolute privacy—all data stays on your phone.",
        "features": [
            ("palette", "4 Sensory Color Themes", "Default, OLED Pure Black, Soft Sage, and Warm Amber for photophobia.", (129, 199, 132)),
            ("shield", "100% Offline & Private", "Zero cloud accounts, zero ads, zero analytics. Strictly local on-device.", (144, 202, 249)),
            ("grid", "30-Day Stressor Trends", "Private Why Log uncovers environmental patterns to prevent future distress.", (255, 183, 77))
        ],
        "caption": "Sensory Comfort & Privacy: Low-stimulus color themes and 100% offline privacy.",
        "glow_color": (129, 199, 132)
    },
    {
        "id": "outro",
        "start": 53.0,
        "end": 60.0,
        "glow_color": (255, 183, 77),
        "caption": "Download Sotto on Google Play. 100% Free & Open Source."
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
            
    canvas = Image.new("RGBA", (W, H), (14, 17, 23, 255))
    d = ImageDraw.Draw(canvas)
    
    # 1. Atmospheric ambient glow
    glow_color = scene.get("glow_color", (255, 183, 77))
    pulse = 0.85 + 0.15 * math.sin(2.0 * math.pi * 0.3 * t)
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([100, 180, 750, 820], fill=(glow_color[0], glow_color[1], glow_color[2], int(42 * pulse)))
    gd.ellipse([1150, 180, 1900, 920], fill=(28, 85, 55, int(26 * pulse)))
    glow_blur = glow.filter(ImageFilter.GaussianBlur(80))
    canvas.paste(glow_blur, (0, 0), glow_blur)
    
    # SCENE: OUTRO (53.0s - 60.0s)
    if scene["id"] == "outro":
        outro_t = (t - 53.0) / 7.0
        alpha = min(1.0, outro_t * 2.2)
        
        # End fade to black for the final 1.0s
        if t >= 59.0:
            fade_out = max(0.0, (60.0 - t) / 1.0)
            alpha *= fade_out
            
        # App Icon
        icon_path = "assets/play_store_icon_512.png"
        if os.path.exists(icon_path):
            icon_img = Image.open(icon_path).convert("RGBA").resize((170, 170), Image.Resampling.LANCZOS)
            imask = Image.new("L", (170, 170), 0)
            ImageDraw.Draw(imask).rounded_rectangle([0, 0, 170, 170], radius=40, fill=255)
            
            # Outer icon glow
            iglow = Image.new("RGBA", (260, 260), (0, 0, 0, 0))
            ImageDraw.Draw(iglow).rounded_rectangle([20, 20, 240, 240], radius=50, fill=(255, 183, 77, int(85 * alpha)))
            iglow_b = iglow.filter(ImageFilter.GaussianBlur(24))
            canvas.paste(iglow_b, (W // 2 - 130, 205), iglow_b)
            canvas.paste(icon_img, (W // 2 - 85, 250), imask)
            
        # Hero Title
        tbox = FONT_HERO.getbbox("SOTTO")
        tw = tbox[2] - tbox[0]
        d.text(((W - tw) // 2, 450), "SOTTO", font=FONT_HERO, fill=(255, 255, 255, int(255 * alpha)))
        
        # Tagline
        tag = "A Calm Voice When Words Are Hard"
        tbox = FONT_OUTRO_TAG.getbbox(tag)
        tw = tbox[2] - tbox[0]
        d.text(((W - tw) // 2, 535), tag, font=FONT_OUTRO_TAG, fill=(255, 204, 128, int(255 * alpha)))
        
        # Subtitle
        sub = "Dignified Assistive Communication & Crisis Navigation"
        sbox = FONT_SUBTITLE.getbbox(sub)
        sw = sbox[2] - sbox[0]
        d.text(((W - sw) // 2, 590), sub, font=FONT_SUBTITLE, fill=(180, 192, 210, int(255 * alpha)))
        
        # 3 Trust Badges (dynamically centered with snug padding)
        badges = [
            ("sparkle", "Google Play", (255, 183, 77)),
            ("check", "100% Free & Open Source", (129, 199, 132)),
            ("shield", "100% Offline Privacy", (144, 202, 249))
        ]
        badge_data = []
        total_w = 0
        gap = 24
        for b_icon, b_title, b_col in badges:
            bbox = FONT_BADGE.getbbox(b_title)
            bw = (bbox[2] - bbox[0]) + 74
            badge_data.append((b_icon, b_title, b_col, bw))
            total_w += bw
        total_w += gap * (len(badges) - 1)
        bx = (W - total_w) // 2
        by = 675
        for b_icon, b_title, b_col, bw in badge_data:
            d.rounded_rectangle([bx, by, bx + bw, by + 52], radius=26, fill=(24, 28, 38, int(230 * alpha)), outline=(b_col[0], b_col[1], b_col[2], int(200 * alpha)), width=2)
            draw_vector_icon(d, b_icon, bx + 18, by + 14, size=24, color=(b_col[0], b_col[1], b_col[2], int(255 * alpha)))
            d.text((bx + 50, by + 14), b_title, font=FONT_BADGE, fill=(240, 240, 240, int(255 * alpha)))
            bx += bw + gap
            
    # SCENES: 0, 1, 2, 3, 4
    else:
        float_y = 42 + int(4.0 * math.sin(2.0 * math.pi * 0.25 * t))
        
        # Crossfade between screen_path and screen_alt_path if defined
        if "screen_alt_path" in scene and "crossfade_at" in scene:
            cf_t = scene["crossfade_at"]
            if t < cf_t:
                screen_img = get_screen(scene["screen_path"])
            elif t >= cf_t + 0.5:
                screen_img = get_screen(scene["screen_alt_path"])
            else:
                blend_p = (t - cf_t) / 0.5
                img1 = get_screen(scene["screen_path"])
                img2 = get_screen(scene["screen_alt_path"])
                screen_img = Image.blend(img1, img2, blend_p)
        else:
            screen_img = get_screen(scene["screen_path"])
            
        # Intro slide-in transition: starts center then glides left
        if scene["id"] == "intro":
            if t < 2.0:
                phone_x = int(W // 2 - 240)
            elif t < 3.0:
                glide_p = ease_out_cubic((t - 2.0) / 1.0)
                phone_x = int((W // 2 - 240) + (140 - (W // 2 - 240)) * glide_p)
            else:
                phone_x = 140
        else:
            phone_x = 140
            
        # Render phone mockup with standardized dimensions
        phone = render_phone_mockup(screen_img, tw=440, th=920)
        canvas.paste(phone, (phone_x, float_y), phone)
        
        # Interactive touch pulses
        bezel_offset = 20 + 12
        # Scene 1 (Why Finder): pulse on green Yes button (115, 668)
        if scene["id"] == "whyfinder" and 14.8 <= t <= 16.2:
            p_prog = (t - 14.8) / 1.4
            pr = int(14 + 38 * p_prog)
            pa = int(220 * (1.0 - p_prog))
            px = phone_x + bezel_offset + 115
            py = float_y + bezel_offset + 668
            d.ellipse([px - pr, py - pr, px + pr, py + pr], outline=(129, 199, 132, pa), width=3)
        # Scene 2 (Partner Mode): pulse on flipped green Yes button (315, 101)
        elif scene["id"] == "partnermode" and 28.5 <= t <= 29.8:
            p_prog = (t - 28.5) / 1.3
            pr = int(14 + 40 * p_prog)
            pa = int(220 * (1.0 - p_prog))
            px = phone_x + bezel_offset + 315
            py = float_y + bezel_offset + 101
            d.ellipse([px - pr, py - pr, px + pr, py + pr], outline=(129, 199, 132, pa), width=3)
        # Scene 3 (AAC & Safety): pulse on speech phrase at 38.6s - 39.8s
        elif scene["id"] == "aac" and 38.6 <= t <= 39.8:
            p_prog = (t - 38.6) / 1.2
            pr = int(16 + 45 * p_prog)
            pa = int(220 * (1.0 - p_prog))
            px = phone_x + bezel_offset + 220
            py = float_y + bezel_offset + 460
            d.ellipse([px - pr, py - pr, px + pr, py + pr], outline=(255, 183, 77, pa), width=3)

        # Right Motion Graphics Panel (only after 2.0s in intro)
        if scene["id"] != "intro" or t >= 2.0:
            panel_t = t - (2.0 if scene["id"] == "intro" else scene["start"])
            card_p = ease_out_cubic(min(1.0, panel_t / 0.55))
            card_alpha = int(255 * card_p)
            offset_x = int(32 * (1.0 - card_p))
            rx = 670 + offset_x
            ry = 120
            
            # Badge
            badge_txt = scene["badge"]
            bbox = FONT_BADGE.getbbox(badge_txt)
            bw = bbox[2] - bbox[0] + 50
            d.rounded_rectangle([rx, ry, rx + bw, ry + 38], radius=19, fill=(38, 28, 18, int(240 * card_p)), outline=(glow_color[0], glow_color[1], glow_color[2], int(220 * card_p)), width=2)
            draw_vector_icon(d, scene["badge_icon"], rx + 12, ry + 9, size=20, color=(glow_color[0], glow_color[1], glow_color[2], card_alpha))
            d.text((rx + 38, ry + 8), badge_txt, font=FONT_BADGE, fill=(255, 204, 128, card_alpha))
            
            # Title & Subtitle
            d.text((rx, ry + 54), scene["title"], font=FONT_TITLE, fill=(255, 255, 255, card_alpha))
            d.text((rx, ry + 115), scene["subtitle"], font=FONT_SUBTITLE, fill=(180, 192, 210, card_alpha))
            
            # 3 Staggered Feature Cards
            card_y = ry + 175
            for idx, (icon, c_title, c_desc, col) in enumerate(scene["features"]):
                # Staggered entrance
                stagger_t = max(0.0, panel_t - 0.12 * idx)
                c_anim = ease_out_cubic(min(1.0, stagger_t / 0.45))
                c_alpha = int(255 * c_anim)
                c_off_x = int(20 * (1.0 - c_anim))
                cx_cur = rx + c_off_x
                
                d.rounded_rectangle([cx_cur, card_y, cx_cur + 1170, card_y + 118], radius=18, fill=(24, 28, 38, int(230 * c_anim)), outline=(48, 56, 72, int(255 * c_anim)), width=1)
                d.rounded_rectangle([cx_cur + 16, card_y + 18, cx_cur + 22, card_y + 100], radius=3, fill=(col[0], col[1], col[2], c_alpha))
                draw_vector_icon(d, icon, cx_cur + 38, card_y + 24, size=24, color=(col[0], col[1], col[2], c_alpha))
                d.text((cx_cur + 72, card_y + 22), c_title, font=FONT_CARD_TITLE, fill=(255, 255, 255, c_alpha))
                d.text((cx_cur + 38, card_y + 64), c_desc, font=FONT_CARD_DESC, fill=(170, 182, 198, c_alpha))
                card_y += 136
                
    # 3. Synchronized Bottom Subtitle Bar (Accessibility for muted autoplay)
    caption_txt = scene["caption"]
    sbox = FONT_CAPTION.getbbox(caption_txt)
    sw = sbox[2] - sbox[0] + 50
    sx = (W - sw) // 2
    sy = 1015
    d.rounded_rectangle([sx, sy, sx + sw, sy + 46], radius=23, fill=(18, 22, 30, 240), outline=(255, 183, 77, 180), width=1)
    d.text((sx + 25, sy + 10), caption_txt, font=FONT_CAPTION, fill=(240, 240, 240, 255))
    
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
        "-b:v", "8500k",
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
