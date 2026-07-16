# Promo Video

- **File**: `amoura-promo.mp4` — done, 1080x1920, 19.6s, ~1.1 MB
- Voiceover: Microsoft Edge neural TTS (`en-US-AvaNeural`), burned-in captions
  synced word-for-word to the narration
- Built from the real icon, feature graphic, and actual app screenshots
  (ad banners stripped so it reflects real UI, not a test ad)
- Regenerate via the PIL/edge-tts/ffmpeg pipeline used to build it if the
  screenshots or copy change — the scene compositor and narration script
  aren't checked in as reusable tooling yet, just the final output

## Still needed before this appears on the Play listing

Play Store promo videos are hosted on YouTube - you don't upload a video
file to Play Console, just paste the URL:

1. Upload `amoura-promo.mp4` to a YouTube channel (unlisted or public)
2. Put the resulting URL in `youtube-url.txt` in this folder
3. Paste that URL into Play Console's store listing "Promo video" field

I don't have YouTube upload access, so this step is manual.
