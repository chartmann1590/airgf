# Performance Validation

Use this sheet for each release candidate so the roadmap targets in `PLAN.md` are checked consistently.

## Target Metrics

| Metric | Target | Validation Method | Latest Result |
| --- | --- | --- | --- |
| LLM decode speed | 40-55 tokens/sec (GPU), 10-15 tokens/sec (CPU fallback) | Time a fixed prompt/response run on a representative device | Pending |
| First token latency | <2 seconds | Measure send -> first streamed token on device | Pending |
| Model load time | <15 seconds | Cold-start chat with model not yet initialized | Pending |
| App cold start (no model load) | <1 second | Measure splash -> first interactive screen | Pending |
| Character animation FPS | 60 fps | Observe character screen with Profile GPU Rendering / frame stats | Pending |
| RAM during chat | <2 GB | Check Android Studio profiler while chatting with model loaded | Pending |
| Model file size | ~2.5 GB | Inspect downloaded file on device | Pending |
| APK size (without model) | <30 MB | Measure release APK output after `:app:assembleRelease` | 56.81 MB (currently over target) |

## Repeatable Validation Steps

1. Build `:app:assembleRelease`.
2. Install the release build on a representative Android 13+ device.
3. Run through onboarding once with notification permission allowed, then again with permission denied.
4. Capture cold start, model load, and first-token latency with timestamps.
5. Open the character screen and confirm smooth fallback rendering for any template missing packaged art.
6. Record APK size and any notable deviations from the targets above.

## Notes

- The new procedural character fallback is intended to keep UI validation unblocked while production sprite packs are completed.
- If any metric misses target, track it as a Phase 9 bug rather than treating it as later polish.
- Connected smoke coverage passed on two attached devices during Phase 9 verification.
