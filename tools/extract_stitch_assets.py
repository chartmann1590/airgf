#!/usr/bin/env python3
"""Download character portrait URLs referenced in Stitch HTML files."""

from __future__ import annotations

import re
import sys
from pathlib import Path

try:
    import requests
except ImportError:
    print("Install requests: pip install requests", file=sys.stderr)
    sys.exit(1)

ROOT = Path(__file__).resolve().parents[1]
STITCH_DIR = ROOT / "stitch_airgf_ai_virtual_companion"
OUT_DIR = ROOT / "tools" / "downloads"

# VisualTemplate.assetPrefix -> Stitch label in expanded/customization HTML
TEMPLATE_SOURCES = {
    "char_anime_cute": ("girlfriend_customization", "Anime Cute"),
    "char_anime_cool": ("girlfriend_customization", "Anime Cool"),
    "char_realistic_warm": ("girlfriend_customization", "Realistic Warm"),
    "char_realistic_elegant": ("girlfriend_customization", "Realistic Elegant"),
    "char_realistic_japanese": ("girlfriend_customization_expanded", "Realistic Japanese"),
    "char_realistic_american": ("girlfriend_customization_expanded", "Realistic American"),
    "char_realistic_vietnamese": ("girlfriend_customization_expanded", "Realistic Vietnamese"),
    "char_realistic_filipino": ("girlfriend_customization_expanded", "Realistic Filipino"),
    "char_stylized_punk": ("girlfriend_customization", "Stylized Punk"),
    "char_stylized_soft": ("girlfriend_customization", "Stylized Soft"),
}

URL_PATTERN = re.compile(r'src="(https://lh3\.googleusercontent\.com/[^"]+)"')
LABEL_PATTERN = re.compile(
    r"<!--\s*([^>]+?)\s*-->\s*<div[^>]*>.*?<img[^>]+src=\"(https://lh3\.googleusercontent\.com/[^\"]+)\"",
    re.DOTALL,
)


def parse_template_urls(html: str) -> dict[str, str]:
    """Map comment label -> image URL from customization grid."""
    result: dict[str, str] = {}
    for match in LABEL_PATTERN.finditer(html):
        label = match.group(1).strip()
        url = match.group(2)
        result[label] = url
    return result


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for prefix, (folder, label) in TEMPLATE_SOURCES.items():
        html_path = STITCH_DIR / folder / "code.html"
        if not html_path.exists():
            print(f"Missing {html_path}")
            continue
        html = html_path.read_text(encoding="utf-8")
        urls = parse_template_urls(html)
        url = urls.get(label)
        if not url:
            print(f"No URL for {label} in {folder}")
            continue
        dest = OUT_DIR / f"{prefix}.jpg"
        if dest.exists():
            print(f"Skip existing {dest.name}")
            continue
        print(f"Downloading {label} -> {dest.name}")
        resp = requests.get(url, timeout=120)
        resp.raise_for_status()
        dest.write_bytes(resp.content)
    print("Done.")


if __name__ == "__main__":
    main()
