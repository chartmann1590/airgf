# Hi-res App Icon

- **File**: `icon-512.png`
- **Size**: 512x512 px exactly
- **Format**: 32-bit PNG (no alpha channel — Play Console rejects icons with
  transparency in the corners; flatten onto a solid/gradient background)
- **Max file size**: 1024 KB

Source to derive from: `app/src/main/res/drawable/ic_launcher_foreground.xml`
and `stitch_airgf_ai_virtual_companion/airgf_app_icon/screen.png` — regenerate
at 512x512 with the Amoura branding (no "AirGF" text baked into any artwork).

Also remember to regenerate the in-app adaptive icon
(`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` + foreground/background
drawables) to match — the Play Store icon and the installed app icon should be
the same mark.
