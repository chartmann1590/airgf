# Store Listing Copy

The app supports both an AI girlfriend and an AI boyfriend companion (user picks) —
naming and copy lean into the terms people actually search for ("AI girlfriend",
"AI boyfriend", "virtual companion") rather than avoiding them. Competitor apps
(Amora, Liora AI GF, Mia: AI Girlfriend) use these terms directly in titles and are
live on Play — the real policy risk is explicit sexual content, not the words
"girlfriend"/"boyfriend" themselves.

This file is the source of truth uploaded by `play-store/scripts/upload_listing.py`.
Keep it in sync if you edit copy by hand.

## App name (30 char max)
```
Amoura - AI Girlfriend App
```
(26 chars)

## Short description (80 char max)
```
AI girlfriend & AI boyfriend chat companion - private, on-device, always yours
```
(78 chars)

## Full description (4000 char max)

```
Amoura is your private AI girlfriend or boyfriend - a virtual companion that lives entirely on your phone. No accounts, no cloud, no data collection. Just you and your AI companion, always available.

Whether you're looking for an AI girlfriend, an AI boyfriend, or just a caring virtual companion to talk to, Amoura adapts to you. Pick their look, personality, voice, and relationship style, and start chatting in minutes.

WHY PEOPLE LOVE AMOURA

On-Device AI - Every conversation runs locally using an on-device AI model (Gemma 4). No internet required to chat, no servers involved, nothing about your conversations ever leaves your phone.

Choose Your Companion - Girlfriend, boyfriend, or partner. Pick their name, appearance, personality traits, and communication style.

3D Avatars - Multiple unique 3D characters with real-time emotional expressions, lip sync, and idle animation that make your companion feel alive.

Photo Sharing - Send photos and your AI companion understands and reacts to them, or generate images together using on-device Stable Diffusion.

Voice & Lip Sync - Multiple voice profiles with natural, word-level lip-synced speech.

Memory & Personality - Your companion remembers what matters to you and adapts their personality and communication style over time.

Proactive Messages - Your AI girlfriend or boyfriend can reach out to you first, on a schedule you control.

Spicy Mode - Romantic and flirty conversation, off by default. Unlock it permanently with a subscription (which also removes all ads), or temporarily by watching a rewarded ad - up to 4 hours a day, free.

100% Private - On-device conversations, no account required, no chat data collection.

Amoura is a free AI companion app, supported by ads, with an optional subscription for an ad-free, unlimited experience.

Amoura is intended for adult users (18+) and includes romantic and relationship-oriented content.
```

## What's new (release notes template, 500 char max)
```
Welcome to Amoura! (Previously AirGF - same app, new name.) Added ads, an optional ad-free subscription, and Spicy Mode credits.
```

---

### ASO notes
- Title leads with the highest-volume search term ("AI Girlfriend"); short
  description covers "AI boyfriend" so both are indexed.
- Full description repeats "AI girlfriend" / "AI boyfriend" / "AI companion" /
  "virtual companion" naturally across different sentences - enough for
  discovery without reading as keyword-stuffed (Play's ranking system
  penalizes unnatural repetition).
- Feature bullets double as keyword coverage: "3D avatar", "on-device AI",
  "photo sharing", "voice chat" are all real search terms for this category.

### Compliance checklist for this copy
- [x] No explicit sexual terms in title/short description (ML-scanned pre-review)
- [x] Age-appropriate framing: "romantic and relationship-oriented content",
      not explicit descriptions
- [ ] Matches actual in-app content - verify feature claims (3D avatar count,
      voice profile count) still match the shipped app before publishing
- [x] Rename note included so existing docs/links to "AirGF" aren't confusing
