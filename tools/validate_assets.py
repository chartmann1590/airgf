#!/usr/bin/env python3
"""Validate character asset directories under app/src/main/assets/characters/."""

from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "characters"

PREFIXES = [
    "char_anime_cute",
    "char_anime_cool",
    "char_realistic_warm",
    "char_realistic_elegant",
    "char_realistic_american",
    "char_realistic_japanese",
    "char_realistic_vietnamese",
    "char_realistic_filipino",
    "char_stylized_punk",
    "char_stylized_soft",
]

REQUIRED = [
    "thumbnail.webp",
    "portrait.webp",
    "body.webp",
    "mouth_sheet.webp",
    "eyes_blink_sheet.webp",
    "config.json",
] + [f"face_{e}.webp" for e in [
    "neutral", "happy", "sad", "flirty", "thinking", "surprised", "laughing", "shy",
]]


def validate_sheet(path: Path, frame_count: int, name: str) -> list[str]:
    errors: list[str] = []
    img = Image.open(path)
    w, h = img.size
    if w % frame_count != 0:
        errors.append(f"{name}: width {w} not divisible by {frame_count} frames")
    return errors


def main() -> int:
    errors: list[str] = []
    for prefix in PREFIXES:
        folder = ASSETS / prefix
        if not folder.is_dir():
            errors.append(f"Missing folder {prefix}")
            continue
        for fname in REQUIRED:
            if not (folder / fname).exists():
                errors.append(f"{prefix}: missing {fname}")
        config_path = folder / "config.json"
        if config_path.exists():
            try:
                json.loads(config_path.read_text(encoding="utf-8"))
            except json.JSONDecodeError as e:
                errors.append(f"{prefix}: invalid config.json — {e}")
        mouth = folder / "mouth_sheet.webp"
        if mouth.exists():
            errors.extend(validate_sheet(mouth, 8, f"{prefix}/mouth_sheet"))
        eyes = folder / "eyes_blink_sheet.webp"
        if eyes.exists():
            errors.extend(validate_sheet(eyes, 4, f"{prefix}/eyes_blink_sheet"))

    if errors:
        for e in errors:
            print(f"ERROR: {e}")
        return 1
    print(f"All {len(PREFIXES)} character asset sets valid.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
