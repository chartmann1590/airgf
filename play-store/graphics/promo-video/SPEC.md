# Promo Video

- **File**: `amoura-promo.mp4` — done, 1080x1920, 19.6s, ~1.1 MB
- Voiceover: Microsoft Edge neural TTS (`en-US-AvaNeural`), burned-in captions
  synced word-for-word to the narration
- Built from the real icon, feature graphic, and actual app screenshots
  (ad banners stripped so it reflects real UI, not a test ad)
- Regenerate via the PIL/edge-tts/ffmpeg pipeline used to build it if the
  screenshots or copy change — the scene compositor and narration script
  aren't checked in as reusable tooling yet, just the final output

## Status

Live at https://youtube.com/shorts/Ed7_5IiEsg4 (see `youtube-url.txt`).
Wired into `play-store/scripts/upload_listing.py` (`PROMO_VIDEO_URL`), which
sets the `video` field on the app's Play Console listing on every run.
