#!/usr/bin/env python3
"""
Generate Google Play Store Feature Graphic (1024x500 RGBA)
===========================================================
Generates a promo banner aligned with Sotto's dual mission:
Caregiver Crisis Navigation (Why Finder) + Dignified Assistive AAC.
"""

import sys
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W, H = 1024, 500

def generate_feature_graphic(
    mockup_mode="guided",  # 'guided' or 'main'
    subtitle="Calm Crisis Navigation & AAC",
    headline="Calm yes/no connection during\nmeltdowns & verbal shutdowns.",
    body="A gentle binary tool for caregivers & families, paired with dignified AAC.",
    output_path="assets/play_store_feature_graphic.png"
):
    canvas = Image.new("RGBA", (W, H), (18, 21, 25, 255))

    # 1. Ambient dark-neutral background glows
    bg_glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(bg_glow)
    gdraw.ellipse([(-140, -140), (660, 640)], fill=(34, 40, 42, 255))
    gdraw.ellipse([(120, -100), (920, 600)], fill=(28, 33, 36, 180))
    canvas.alpha_composite(bg_glow)

    draw = ImageDraw.Draw(canvas)

    # 2. Typography
    try:
        font_title = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 52)
        font_subtitle = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 20)
        font_headline = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 28)
        font_body = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 17)
        font_pill = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 14)
    except Exception:
        font_title = font_subtitle = font_headline = font_body = font_pill = ImageFont.load_default()

    # 3. Sotto App Icon
    icon_path = "assets/play_store_icon_512.png"
    if os.path.exists(icon_path):
        icon_src = Image.open(icon_path).convert("RGBA")
        icon_size = 110
        icon_resized = icon_src.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
        icon_mask = Image.new("L", (icon_size, icon_size), 0)
        im_draw = ImageDraw.Draw(icon_mask)
        im_draw.rounded_rectangle([(0, 0), (icon_size, icon_size)], radius=24, fill=255)
        canvas.paste(icon_resized, (64, 75), icon_mask)

    # 4. Brand Title & Mission Subtitle
    draw.text((194, 90), "Sotto", font=font_title, fill=(255, 255, 255, 255))
    draw.text((196, 150), subtitle, font=font_subtitle, fill=(142, 178, 146, 255))

    # 5. Core Value Headline
    draw.text((64, 222), headline, font=font_headline, fill=(245, 247, 250, 255), spacing=6)

    # 6. Supporting Audience & Purpose
    draw.text((64, 308), body, font=font_body, fill=(175, 184, 192, 255))

    # 7. Feature Pills with crisp vector icons
    def draw_pill_icon(pdraw, icon_type, x, y, color):
        if icon_type == "compass":
            pdraw.ellipse([x, y, x + 14, y + 14], outline=color, width=2)
            pdraw.polygon([(x + 7, y + 3), (x + 10, y + 7), (x + 7, y + 11), (x + 4, y + 7)], fill=color)
        elif icon_type == "partner":
            pdraw.ellipse([x, y + 3, x + 6, y + 9], fill=color)
            pdraw.ellipse([x + 8, y + 3, x + 14, y + 9], fill=color)
            pdraw.line([x + 3, y + 6, x + 11, y + 6], fill=color, width=2)
        elif icon_type == "lock":
            pdraw.rounded_rectangle([x + 2, y + 5, x + 12, y + 14], radius=2, fill=color)
            pdraw.arc([x + 4, y, x + 10, y + 8], start=180, end=0, fill=color, width=2)

    pills = [
        ("compass", "Guided Crisis Tree", (255, 183, 77)),
        ("partner", "Caregiver Partner Mode", (129, 199, 132)),
        ("lock", "100% Offline & Private", (144, 202, 249))
    ]

    pill_x = 64
    pill_y = 365
    for icon_type, label_txt, icon_color in pills:
        bbox = font_pill.getbbox(label_txt)
        txt_w = bbox[2] - bbox[0]
        pw = txt_w + 48
        ph = 36
        draw.rounded_rectangle([(pill_x, pill_y), (pill_x + pw, pill_y + ph)], radius=18,
                               fill=(36, 42, 48, 220), outline=(58, 66, 76, 255), width=1)
        draw_pill_icon(draw, icon_type, pill_x + 13, pill_y + 11, icon_color)
        draw.text((pill_x + 34, pill_y + 9), label_txt, font=font_pill, fill=(220, 226, 232, 255))
        pill_x += pw + 12

    # 8. Phone Mockup
    screen_file = "assets/screenshot_partner_mode.png" if mockup_mode == "guided" else "assets/screenshot.png"
    if not os.path.exists(screen_file):
        screen_file = "assets/screenshot.png"

    if os.path.exists(screen_file):
        screen_img = Image.open(screen_file).convert("RGBA")
        mock_w, mock_h = 222, 475
        mock_crop = screen_img.resize((mock_w, mock_h), Image.Resampling.LANCZOS)

        bezel = 7
        pw = mock_w + bezel * 2
        ph = mock_h + bezel * 2
        r = 28

        phone_frame = Image.new("RGBA", (pw + 30, ph + 30), (0, 0, 0, 0))
        pf_draw = ImageDraw.Draw(phone_frame)

        # Drop shadow
        smask = Image.new("RGBA", (pw + 30, ph + 30), (0, 0, 0, 0))
        sdraw = ImageDraw.Draw(smask)
        sdraw.rounded_rectangle([15, 18, 15 + pw, 18 + ph], radius=r, fill=(0, 0, 0, 180))
        phone_frame.paste(smask.filter(ImageFilter.GaussianBlur(12)), (0, 0))

        # Titanium bezel
        pf_draw.rounded_rectangle([15, 15, 15 + pw, 15 + ph], radius=r, fill=(24, 27, 33, 255), outline=(62, 70, 82, 255), width=2)

        # Screen mask
        scr_mask = Image.new("L", (mock_w, mock_h), 0)
        smdraw = ImageDraw.Draw(scr_mask)
        smdraw.rounded_rectangle([(0, 0), (mock_w, mock_h)], radius=r - bezel, fill=255)
        phone_frame.paste(mock_crop, (15 + bezel, 15 + bezel), scr_mask)

        # Punch hole camera
        cx = 15 + pw // 2
        cy = 15 + bezel + 10
        pf_draw.ellipse([cx - 4, cy - 4, cx + 4, cy + 4], fill=(12, 14, 16, 255))

        canvas.paste(phone_frame, (710, 8), phone_frame)

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    canvas.save(output_path)
    print(f"Generated {output_path} (1024x500 RGBA) with {mockup_mode} mockup.")

if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "guided"
    generate_feature_graphic(mockup_mode=mode)
