# Play Store Release Assets — Amoura

This folder holds everything needed for the Google Play Store listing, organized by
asset type. Each subfolder has its own `SPEC.md` with exact dimensions/format
requirements. Drop final files directly into the folder they describe — file names
don't matter to Play Console, but keep them descriptive for our own sanity.

## Structure

```
play-store/
├── graphics/
│   ├── icon/              512x512 hi-res app icon (PNG, 32-bit, no alpha)
│   ├── feature-graphic/   1024x500 banner shown at top of listing
│   └── promo-video/       YouTube URL for optional promo video
├── screenshots/
│   ├── phone/             2-8 screenshots, 16:9 or 9:16, min 320px, max 3840px
│   ├── tablet-7in/        optional, same rules, 7" tablet frame
│   └── tablet-10in/       optional, same rules, 10" tablet frame
├── listing/                app title, short/full description, what's new
├── legal/                  privacy policy, terms of service
├── content-rating/         IARC questionnaire answers/notes
└── data-safety/             Play Console "Data safety" section answers
```

## Status checklist

- [ ] App icon (512x512) — see `graphics/icon/SPEC.md`
- [ ] Feature graphic (1024x500) — see `graphics/feature-graphic/SPEC.md`
- [ ] Phone screenshots (min 2, recommend 4-8) — see `screenshots/phone/SPEC.md`
- [ ] Tablet screenshots (optional but recommended) 
- [ ] Promo video (optional)
- [ ] Store listing copy — see `listing/`
- [ ] Privacy policy URL — see `legal/`
- [ ] Content rating questionnaire — see `content-rating/`
- [ ] Data safety form — see `data-safety/`
- [ ] Target audience & content declaration (18+, romantic/companion content)

## Compliance notes specific to this app (romantic AI companion)

Google Play has specific requirements for apps in this category — treat these as
blockers, not nice-to-haves:

1. **Age rating / target audience**: Must be flagged 18+ (Mature/AO-adjacent
   depending on content). Do NOT let the default onboarding target "Everyone" or
   "Teen" — the content rating questionnaire must accurately reflect romantic/
   suggestive chat content, even if explicit content is gated or absent.
2. **User-generated/AI-generated content policy**: Since the companion generates
   freeform chat and images on-device, the listing and in-app must disclose this
   is AI-generated content, and the app needs a reporting/blocking mechanism
   (already covered by the in-app feedback reporter) and a way to reset/delete
   data.
3. **Sexual content policy**: Play prohibits apps whose primary purpose is to
   facilitate sexually explicit "hookup" content. Keep "Spicy Mode" framed as
   romantic/flirty, not explicit sexual content, in both the listing copy and
   actual generated output — this affects both approval odds and the content
   rating.
4. **Store listing text**: avoid explicit sexual terms in the title, short
   description, and screenshots (Play's ML scanners flag these independent of
   the human review). Keep screenshots free of explicit imagery.
5. **Privacy policy is mandatory** — chat itself stays on-device, but the app
   now also uses Google AdMob (ads), Google UMP (ad consent), Google Play
   Billing (subscription), and optionally Firebase Analytics/Crashlytics. The
   hosted privacy policy must disclose all of these, not just "no data
   collected" — see `legal/README.md`.
6. **Data safety form** must match reality: declare Advertising ID / device
   IDs (AdMob), purchase history (Play Billing), and Analytics/Crashlytics
   data if enabled (see `app/build.gradle.kts`) — see `data-safety/README.md`
   for the full draft answers.
7. **Ads + consent compliance**: a certified CMP (Google UMP) is required for
   personalized ads served to EEA/UK/Switzerland users — implemented in
   `app/src/main/java/com/airgf/app/ads/ConsentManager.kt`. No ad request may
   fire before consent resolves. Debug builds must use Google's test ad unit
   IDs, never the real ones (see `app/src/main/java/com/airgf/app/ads/AdUnitIds.kt`)
   — real IDs on a dev/test device risk AdMob account suspension for invalid
   traffic.
