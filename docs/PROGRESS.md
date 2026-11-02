# Progress

## Done
- Phase 0 (PC side): forked to Matthew-ms/voice-keyboard, cloned, `upstream` remote added, app builds, 439/444 upstream tests pass (see Known issues), app renamed to "My Voice Keyboard" (`com.tyraen.voicekeyboard.mine`) so it installs beside the original.

- Phase 0 (phone side) ✅ 2026-11-02: installed on Galaxy S24+ (SM-S9260, Android 16) over USB, keyboard enabled, mic granted, Groq key set for speech + post-processing, dictation works.

## Next
- Phase 1: codebase tour + `docs/ARCHITECTURE-NOTES.md`.
- Collect "before" accuracy examples (what was said vs. typed, per language) to compare against later.

## Open questions for later phases
- Phase 2: store corrections in a JSON file instead of Room? (No Room in the project today.)
- Confirm: Gboard stays the typing keyboard, this one is for voice only.

## Known issues
- 5 upstream tests in `HallucinationFilterTest` fail on this PC. Cause: we test on Java 21, upstream CI uses Java 17, and Java 19+ changed regex `\b` to only treat English letters as word letters, so Russian patterns like `\bсубтитр` stop matching. Tests-only: Android uses its own regex engine, so the app on the phone is unaffected. Fix if needed: install JDK 17 and build with it.
- Some screens still say "Voice Keyboard" (upstream hard-codes it in a few strings). Cosmetic; left alone to keep upstream merges clean.
- Leave the in-app "check for updates" off: it would offer the original app's releases.
