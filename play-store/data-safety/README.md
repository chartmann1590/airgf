# Data Safety Form

Filled out in Play Console under Store presence > Data safety. Track the
source-of-truth answers here so they stay consistent with the privacy policy
and don't drift as the app changes.

## Audit before filling out
Grep the codebase for anything that leaves the device before answering "no
data collected":
- `app/build.gradle.kts` — Firebase/Analytics/Crashlytics dependencies, plus
  `play-services-ads`, `user-messaging-platform`, `billing-ktx` (AdMob/UMP/
  Play Billing, added for the ads + subscription feature)
- `app/google-services.json` — confirms Firebase project is wired in
- `app/src/main/java/com/airgf/app/data/feedback/` — GithubApi.kt,
  ImageUploadHelper.kt: in-app bug reporter uploads text + images to GitHub
- `app/src/main/java/com/airgf/app/ads/` — AdMob banner/interstitial/rewarded
  ads and the UMP consent flow; `SubscriptionRepositoryImpl.kt` (Play Billing)

## Draft answers
- **Data collected**:
  - Crash logs / diagnostics (if Crashlytics enabled)
  - App interactions (if Analytics enabled)
  - User-submitted content (bug report text/images sent to GitHub when user
    opts into feedback)
  - **Advertising ID / Device or other IDs** (collected by AdMob for ad
    delivery and, if the user consents via the UMP form, ad personalization)
  - **Purchase history** (subscription status, via Google Play Billing)
- **Data NOT collected**: chat conversations, personal identifiers, precise
  location, contacts — verify none of these are transmitted anywhere before
  asserting this
- **Data shared with third parties**: GitHub (feedback reports only, user-
  initiated), Firebase/Google (analytics/crash data, if enabled), **Google
  AdMob** (advertising ID and ad interaction data, for serving/measuring
  banner, interstitial, and rewarded ads), **Google Play Billing** (purchase/
  subscription status only — no payment details touch the app)
- **Data collection is user-controllable**: ad personalization is gated by the
  UMP consent form for EEA/UK/Switzerland users (`ConsentManager.kt`); a
  "Privacy Options" row in Settings lets users change that choice later
- **Data deletion**: In-app "Reset Everything" clears local data; note that
  data already sent via feedback reports (GitHub) or already collected by
  AdMob/Google is not covered by local reset — call this out explicitly to
  avoid a compliance gap
- **Encryption in transit**: confirm feedback upload uses HTTPS (check
  `GithubApi.kt`); AdMob/UMP/Billing traffic is HTTPS by default via the SDKs
