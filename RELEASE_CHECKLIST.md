# Release Checklist

## Install And Upgrade
- [ ] Fresh install lands on onboarding and completes without crashes.
- [ ] Existing install with completed onboarding lands in chat.
- [ ] App upgrade preserves chat history, profiles, model state, and settings.
- [ ] Reset returns to onboarding and clears profiles/messages/preferences while leaving the model available locally.

## Model Lifecycle
- [ ] Model download succeeds on a clean install.
- [ ] Re-download replaces the local model cleanly.
- [ ] Delete model removes the file and updates Settings state.
- [ ] Launch without a model shows a clear recovery path instead of crashing.
- [ ] Insufficient storage shows a specific error state.

## Notifications
- [ ] Android 13+ asks for notification permission at setup completion when needed.
- [ ] Enabling proactive messages from Settings requests permission when needed.
- [ ] Denying permission leaves proactive messages off and shows clear feedback.
- [ ] Tapping a proactive notification opens the app back into chat.

## Character And Chat
- [ ] Every visual template renders in setup, chat avatar, and character screen.
- [ ] Missing art assets fall back to the built-in placeholder renderer instead of leaving blank areas.
- [ ] Character blink, lip-sync fallback, and emotion rendering remain visible without packaged sprite assets.
- [ ] Chat handles missing model state, retry paths, and settings navigation gracefully.

## Release Build
- [x] `:app:assembleRelease` succeeds with shrinking enabled.
- [x] `app/schemas` was generated after enabling Room schema export.
- [ ] Release APK size stays under the Phase 9 target.
