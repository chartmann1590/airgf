# AirGF Character Asset Tools

## Frame order contract

### Mouth sheet (`mouth_sheet.webp`)

Horizontal strip, 8 equal-width frames (indices match `LipSyncBridge.MouthShape.frameIndex`):

| Index | Shape    | Description        |
|-------|----------|--------------------|
| 0     | CLOSED   | Closed lips        |
| 1     | OPEN_A   | Wide open (ah)     |
| 2     | NARROW_E | Narrow smile (ee)  |
| 3     | ROUND_O  | Round (oh)         |
| 4     | WIDE_W   | Wide (w)           |
| 5     | TEETH_F  | Teeth visible (f/v)|
| 6     | LIPS_M   | Pressed (m/b/p)    |
| 7     | TONGUE_L | Tongue (l/t/d/n)   |

### Eyes blink sheet (`eyes_blink_sheet.webp`)

4 frames: open → half → closed → half

## Scripts

- `extract_stitch_assets.py` — Download portrait URLs from Stitch HTML
- `generate_character_assets.py` — Build full asset tree (WebP + config.json) for all 10 templates
- `validate_assets.py` — Verify required files and sprite sheet dimensions

- `extend_sora_hair.py` - Lengthen Sora's existing skinned hair without
  re-exporting or changing the avatar's skin bind matrices

## Usage

```bash
pip install Pillow requests
python tools/generate_character_assets.py
python tools/validate_assets.py
```

Sora's release GLB is rebuilt from the attributed CC BY 4.0 source with:

```bash
python tools/extend_sora_hair.py \
  3d-models/alina_ip_realistic_asian_woman_animated.glb \
  app/src/main/assets/models/sora.glb
```

Output: `app/src/main/assets/characters/{assetPrefix}/`
