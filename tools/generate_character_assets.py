#!/usr/bin/env python3
"""Generate full character asset sets for all 10 VisualTemplate prefixes."""

from __future__ import annotations

import json
import math
import sys
from io import BytesIO
from pathlib import Path

try:
    import requests
    from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageOps
except ImportError:
    print("Install dependencies: pip install Pillow requests", file=sys.stderr)
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[1]
OUT_ROOT = ROOT / "app" / "src" / "main" / "assets" / "characters"

CANVAS_W, CANVAS_H = 768, 1024
THUMB_MAX = 512
WEBP_QUALITY = 82

# assetPrefix -> image URL
TEMPLATE_URLS: dict[str, str] = {
    "char_anime_cute": "https://lh3.googleusercontent.com/aida-public/AB6AXuBalALnbJBtnMwmE052C4FPeSClb5MGZ1NuPwrSQpXiVypsHQmzMtQX5ETgpBmX5bJWLqjNv1P26O1nFjujLUPZYkcs5ayZ-iXIg9mDT-hch5Wb2qloCHSCdvZN3goAoJ4ZiSOPCWkYoqM2mKL27J5ySuW6fCpWMAa8g5TKIB4WOjbCxMJ0VeVYL4VcU1vGSeJ7z7GN5n4zx83GiWQpfP6i0vMuLanOgrjRUmRPN5VMsWUMhBDx_HGo_B4sIriiKoSEatzfHfSjetTw",
    "char_anime_cool": "https://lh3.googleusercontent.com/aida-public/AB6AXuCK8t5_Stom5A88rMvoCX-djqixUJsNUg1dcU-bHr3BqNNvil75Dvaee_EmN0XOGPplp4Ivq8eVR_pSDS_OjYK2nR4x3H-t4Y-ou6MPoIPrZuLlcfGE0fUoEjrupNJw2lQXEomzWIUk2cAmdbC0liwZnDlEhca_mKOr5mki-uQNnHKabwkSw9IghWvK2jpVi5JyNBj9APfEXQPXYkGOQBYPCg1N8kp7LbZLQPmHSBQ9wNzc0Or7Mqci84EYVnuCejeXF8B_c28IZTKs",
    "char_realistic_warm": "https://lh3.googleusercontent.com/aida-public/AB6AXuD2D5KUPPkQdnwGVxcyz82mQt7LQUJcbdnIBDD5fmnuF7lxJR7Ia2PobBRd0x9IrtKXNZNSn6_YJcG6qei_-mYQcrpQWJkDw2UwocIF84poFIjp-QQbXxUKHKxcN9bhZN5g7EX4RXwOGVPpbe3GOY1D-vF7YPSlBtrxBpoSkXIAJH5N34zRs3mIeXKxpNz9COYQN-wmMpeZXHZOcfVC-DkwBPvhs9ziTXd0X_kTHYU8rwy8MKZ8KEehEdYsEXCzGe5uyo-oHN7emQmE",
    "char_realistic_elegant": "https://lh3.googleusercontent.com/aida-public/AB6AXuBTNUkIPMBrFAqzfT4aaya9B8nCce5OYMV1LH-eJ65m6BDd2nR98IwNNFDrtl8IrbkAxu5zjEGNY5_HgTqJTBkZJmPIAWw26hKayy3NQ8pp0FZDNzkVbTlCz6pofSsVxjk8J33BNbVARtvqrn8PVJUzQMI46d4u9FHZpJqfO0EcKi0Pn83108ex4rgypfL9nr4U4vWrgH9h_Qsl0QdFgIEjAWrS4QLveSD5hFrljl8rI4FMe1L1wPJqxpu21R5Bi5lgDBv47JLfxc7T",
    "char_realistic_japanese": "https://lh3.googleusercontent.com/aida-public/AB6AXuAxlele4mg-Amukh2z5wb-4b7JX34wJItzEHI9GEXFr-W_6MO4BPhKbxTYCm4kS6WySA-AriGuoCFOj06FJygln0m6__F_bra40kpAzS6tRaSt8rompNDf5uuy83Z1aMZzPJXmuXv5R4_tO74DHvq12YbiPf4tqDOKPjVYgKtszItUAF5kdjg8oW1us4_1C_hGsox-luk6eXoV0muK-Xz_HE2SfKWyjqdJr88AKYouHpHU11W0qv0YmmDpnH8h107qkFYUB5cgSZBFL",
    "char_realistic_american": "https://lh3.googleusercontent.com/aida-public/AB6AXuDdAIOJvX_c3227QVv03V1cGSHpvKBBXm9TP7grxwunNpPJEv27q6yBhQ3ouWr5TevbrbHryn1y0hrak79ihGVfcTpbePQVk2y3kJcv5EH8zRxjPHp9zBj7td_1Yva8o__lTGvJ9UBQPfTT6raZfCR53StEwsNUWP8-5le6OrYhliPc0EoNxi_fjPR0GWZohrYEDLaXtM0LA041f9CY-bwtWlQaCCnZe46nWpwy2TQnP6AWINabof8otzydItPBCQjhCWmlx5Bn2DxP",
    "char_realistic_vietnamese": "https://lh3.googleusercontent.com/aida-public/AB6AXuAo7O3nbIkAJK2Uf9JS9x2E7BqISZWkmR5VkFk6kqFiFXHpO9nmpZ6bfYWQ5_OpZc0R2OTEvQXOo4Ec83CjT2lPRQsr1UNmrF4pS5OjTly0WAMxMB0Pitvw9vaKpSc6elltBIzjqs_CH1PORO9Vg-0nZjCBgOm3NudgxCZqYVTSWGqaJKVKmf5TyYC7QQjyx47BXGm6Ov5WccLI-D2D8dxCzEkFE_Xei1WwFnRRNJxtD6MVPkIx_zrqTab7UfqA-5Tze5nRbqoZ_nk4",
    "char_realistic_filipino": "https://lh3.googleusercontent.com/aida-public/AB6AXuClhQx3zSu3p_jT9sSHwDUaIb5a3DYvA6-63DMw2O9XthfyrT1vQYpGY6on9Fy4HNf-e0UUYnnA-ql1JDZNnv42QWs85dwTww4doO-qiBP_qeYTMYub_F1Xhr4XIen-qLViWk5KGbQh8FVS6iQUxZ1FMq8nXA8iH2p7EarNrUmZSHMoHeLYSGCVqPrZqqBZr2w0QOfw-E6QBgwFoxgGEzhqp3FgDGD6jTYshCZO15yLeQ6bD77jch6UJqrxv5jpqT_TYESGgh_IbMiV",
    "char_stylized_punk": "https://lh3.googleusercontent.com/aida-public/AB6AXuD69Dhel-t8rPurN8wvPH2yokBNQwwqMVKxdqCUEdKXmJq-OVpC8YbJqU8UinkAFUHFjpbT5bx_YlJji53XsPgNtFX9UfHwTtQ_4hYQoF8O05_faeyxlRE4TSewvxGwpBJQCuja9kbradKkrwufcoWrMwqnWZGYHJmfQ5eGPnOSXj_IUPAxHYvKfty_AuLPTgTXCFUeMoHPRe7cinzTO9_YTMT3UJe330-OseoE_pKU7faYptbUxhH4W6NHs-QoB3bdzg2O8t-JkDen",
    "char_stylized_soft": "https://lh3.googleusercontent.com/aida-public/AB6AXuAlFVLnN1WtefI3kwtdRq439A-CcXxzh7P4vZkwPECkJ7_cJLPN1xEa2i_c4IAIe2VP1N_9C5ZJCygenSfkkVPVPoi5dCKctaLew-mGO4-QP5UGaEiOlNRsAXNqzCVtRQqLd8h0JLtLxDIPsMW7aZf8QqrLtiL2xToX3rqaaRb1OEfbMXKsQY4x4cDd__gBIzePxLotcX-h66Fu1Xk2A0Pl2vgeZnCrFyS91lrGLdbu-0X6tkBrAvNEgHp3r4YkvoZh91yeMq0087fS",
}

EMOTION_FILTERS: dict[str, tuple[float, float, float, float | None]] = {
    # brightness, color, contrast, optional blur radius
    "neutral": (1.0, 1.0, 1.0, None),
    "happy": (1.08, 1.12, 1.05, None),
    "sad": (0.88, 0.85, 0.95, None),
    "flirty": (1.05, 1.18, 1.08, None),
    "thinking": (0.95, 0.92, 1.0, None),
    "surprised": (1.1, 1.05, 1.12, None),
    "laughing": (1.12, 1.15, 1.1, None),
    "shy": (0.92, 1.08, 0.98, 1.2),
}

MOUTH_FRAME_W, MOUTH_FRAME_H = 96, 64
EYE_FRAME_W, EYE_FRAME_H = 200, 72


def download_image(url: str) -> Image.Image:
    resp = requests.get(url, timeout=120)
    resp.raise_for_status()
    return Image.open(BytesIO(resp.content)).convert("RGB")


def crop_portrait(img: Image.Image, aggressive_center: bool = False) -> Image.Image:
    w, h = img.size
    target_ratio = CANVAS_W / CANVAS_H
    current_ratio = w / h
    if current_ratio > target_ratio:
        new_w = int(h * target_ratio)
        left = (w - new_w) // 2
        box = (left, 0, left + new_w, h)
    else:
        new_h = int(w / target_ratio)
        if aggressive_center:
            # Tighter vertical crop for chibi-style assets
            top = int((h - new_h) * 0.35)
        else:
            top = (h - new_h) // 2
        box = (0, top, w, top + new_h)
    cropped = img.crop(box)
    return cropped.resize((CANVAS_W, CANVAS_H), Image.Resampling.LANCZOS)


def save_webp(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if img.mode != "RGBA":
        img = img.convert("RGBA")
    img.save(path, "WEBP", quality=WEBP_QUALITY, method=6)


def apply_emotion(base: Image.Image, emotion: str) -> Image.Image:
    b, c, k, blur = EMOTION_FILTERS[emotion]
    out = base.copy()
    if b != 1.0:
        out = ImageEnhance.Brightness(out).enhance(b)
    if c != 1.0:
        out = ImageEnhance.Color(out).enhance(c)
    if k != 1.0:
        out = ImageEnhance.Contrast(out).enhance(k)
    if blur:
        out = out.filter(ImageFilter.GaussianBlur(radius=blur))
    if emotion == "happy":
        # Warm pink tint overlay
        tint = Image.new("RGB", out.size, (255, 200, 220))
        out = Image.blend(out, tint, 0.08)
    elif emotion == "sad":
        tint = Image.new("RGB", out.size, (160, 170, 200))
        out = Image.blend(out, tint, 0.12)
    elif emotion == "flirty":
        tint = Image.new("RGB", out.size, (255, 140, 200))
        out = Image.blend(out, tint, 0.1)
    elif emotion == "shy":
        tint = Image.new("RGB", out.size, (255, 180, 200))
        out = Image.blend(out, tint, 0.15)
    return out


def draw_mouth_frame(draw: ImageDraw.ImageDraw, frame: int, w: int, h: int) -> None:
    cx, cy = w // 2, h // 2
    lip_color = (220, 120, 150, 230)
    outline = (180, 80, 110, 255)
    if frame == 0:  # CLOSED
        draw.line([(cx - 28, cy), (cx + 28, cy)], fill=outline, width=3)
        draw.ellipse([cx - 30, cy - 6, cx + 30, cy + 8], fill=lip_color)
    elif frame == 1:  # OPEN_A
        draw.ellipse([cx - 22, cy - 18, cx + 22, cy + 22], fill=(80, 40, 50, 255))
        draw.ellipse([cx - 26, cy - 22, cx + 26, cy + 24], outline=outline, width=3)
    elif frame == 2:  # NARROW_E
        draw.arc([cx - 24, cy - 10, cx + 24, cy + 14], 200, 340, fill=outline, width=3)
        draw.ellipse([cx - 20, cy - 4, cx + 20, cy + 8], fill=lip_color)
    elif frame == 3:  # ROUND_O
        draw.ellipse([cx - 16, cy - 14, cx + 16, cy + 16], fill=(70, 35, 45, 255))
        draw.ellipse([cx - 20, cy - 18, cx + 20, cy + 20], outline=outline, width=3)
    elif frame == 4:  # WIDE_W
        draw.ellipse([cx - 30, cy - 8, cx + 30, cy + 12], fill=lip_color)
        draw.line([(cx - 32, cy + 2), (cx + 32, cy + 2)], fill=outline, width=2)
    elif frame == 5:  # TEETH_F
        draw.rectangle([cx - 20, cy - 8, cx + 20, cy + 6], fill=(245, 245, 240, 255))
        draw.ellipse([cx - 24, cy - 12, cx + 24, cy + 10], outline=outline, width=3)
    elif frame == 6:  # LIPS_M
        draw.ellipse([cx - 26, cy - 10, cx + 26, cy + 4], fill=lip_color)
        draw.line([(cx - 26, cy - 2), (cx + 26, cy - 2)], fill=outline, width=4)
    elif frame == 7:  # TONGUE_L
        draw.ellipse([cx - 18, cy - 10, cx + 18, cy + 14], fill=(80, 40, 50, 255))
        draw.ellipse([cx - 10, cy, cx + 10, cy + 12], fill=(230, 130, 150, 255))


def build_mouth_sheet() -> Image.Image:
    sheet_w = MOUTH_FRAME_W * 8
    sheet = Image.new("RGBA", (sheet_w, MOUTH_FRAME_H), (0, 0, 0, 0))
    for i in range(8):
        frame = Image.new("RGBA", (MOUTH_FRAME_W, MOUTH_FRAME_H), (0, 0, 0, 0))
        draw = ImageDraw.Draw(frame)
        draw_mouth_frame(draw, i, MOUTH_FRAME_W, MOUTH_FRAME_H)
        sheet.paste(frame, (i * MOUTH_FRAME_W, 0))
    return sheet


def build_eye_frame(base: Image.Image, blink: float) -> Image.Image:
    """blink: 0=open, 1=closed"""
    w, h = CANVAS_W, CANVAS_H
    eye_y = int(h * 0.36)
    eye_h = int(h * 0.08)
    eye_x1 = int(w * 0.28)
    eye_x2 = int(w * 0.62)
    region = base.crop((0, eye_y, w, eye_y + eye_h * 2))
    frame = Image.new("RGBA", (EYE_FRAME_W, EYE_FRAME_H), (0, 0, 0, 0))
    scaled = region.resize((EYE_FRAME_W, EYE_FRAME_H), Image.Resampling.LANCZOS).convert("RGBA")
    if blink > 0.05:
        overlay = Image.new("RGBA", scaled.size, (40, 25, 35, int(220 * blink)))
        scaled = Image.alpha_composite(scaled, overlay)
        # Eyelid line
        draw = ImageDraw.Draw(scaled)
        lid_y = int(EYE_FRAME_H * (0.35 + 0.45 * blink))
        draw.rectangle([0, lid_y, EYE_FRAME_W, EYE_FRAME_H], fill=(40, 25, 35, int(200 * blink)))
    frame.paste(scaled, (0, 0))
    return frame


def build_eyes_sheet(portrait: Image.Image) -> Image.Image:
    blinks = [0.0, 0.45, 1.0, 0.45]
    sheet_w = EYE_FRAME_W * 4
    sheet = Image.new("RGBA", (sheet_w, EYE_FRAME_H), (0, 0, 0, 0))
    for i, b in enumerate(blinks):
        frame = build_eye_frame(portrait, b)
        sheet.paste(frame, (i * EYE_FRAME_W, 0))
    return sheet


def default_config() -> dict:
    return {
        "canvasWidth": CANVAS_W,
        "canvasHeight": CANVAS_H,
        "faceOffset": {"x": 0, "y": 0},
        "eyeOffset": {"x": int(CANVAS_W * 0.28), "y": int(CANVAS_H * 0.34)},
        "eyeSize": {"width": EYE_FRAME_W, "height": EYE_FRAME_H},
        "mouthOffset": {"x": int(CANVAS_W * 0.38), "y": int(CANVAS_H * 0.68)},
        "mouthSize": {"width": MOUTH_FRAME_W, "height": MOUTH_FRAME_H},
    }


def generate_template(prefix: str, url: str, mouth_sheet: Image.Image) -> None:
    out_dir = OUT_ROOT / prefix
    out_dir.mkdir(parents=True, exist_ok=True)
    aggressive = prefix == "char_stylized_soft"

    print(f"Processing {prefix}...")
    raw = download_image(url)
    portrait = crop_portrait(raw, aggressive_center=aggressive)

    # Thumbnail
    thumb = portrait.copy()
    thumb.thumbnail((THUMB_MAX, THUMB_MAX), Image.Resampling.LANCZOS)
    save_webp(thumb, out_dir / "thumbnail.webp")

    save_webp(portrait, out_dir / "portrait.webp")
    save_webp(portrait, out_dir / "body.webp")

    for emotion in EMOTION_FILTERS:
        face = apply_emotion(portrait, emotion)
        save_webp(face, out_dir / f"face_{emotion}.webp")

    save_webp(mouth_sheet, out_dir / "mouth_sheet.webp")
    save_webp(build_eyes_sheet(portrait), out_dir / "eyes_blink_sheet.webp")

    config = default_config()
    (out_dir / "config.json").write_text(json.dumps(config, indent=2), encoding="utf-8")


def main() -> None:
    mouth_sheet = build_mouth_sheet()
    for prefix, url in TEMPLATE_URLS.items():
        try:
            generate_template(prefix, url, mouth_sheet)
        except Exception as e:
            print(f"Failed {prefix}: {e}", file=sys.stderr)
            raise
    print(f"Generated assets under {OUT_ROOT}")


if __name__ == "__main__":
    main()
