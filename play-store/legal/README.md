# Legal — Privacy Policy & Terms

Google Play requires a **hosted, publicly accessible privacy policy URL** in
Play Console before you can publish, even for a fully on-device/offline app.

## Hosted privacy policy

Live at **https://chartmann1590.github.io/airgf/privacy.html** (source:
`docs/privacy.html`, deployed automatically via `.github/workflows/pages.yml`
on push to `master`/`main`). Use this exact URL in Play Console's Data
safety / App content sections.

## Status

- [x] Privacy policy written and hosted (`docs/privacy.html`), covering
      on-device data, AdMob, UMP consent, Play Billing, Crashlytics/Analytics,
      the GitHub feedback reporter, data deletion, and age restriction (18+).
- [ ] Terms of Service (optional but recommended for a companion/relationship
      app — clarifies AI-generated content, no real relationship implied, etc.)
- [ ] In Play Console: enable the **User consent** message under
      Monetize > Privacy & messaging (UMP), matching the in-app UMP wiring in
      `app/src/main/java/com/airgf/app/ads/ConsentManager.kt`
- [ ] Paste the hosted privacy policy URL into Play Console's Data safety /
      App content sections (see URL above)
