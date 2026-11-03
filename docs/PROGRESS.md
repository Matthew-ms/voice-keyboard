# Progress

## Done
- 2026-11-02: language key with AUTO (`auto, en, ja, zh, yue`); Groq accepts `yue` (Cantonese), checked on phone.
- Phase 0 (PC side): forked to Matthew-ms/voice-keyboard, cloned, `upstream` remote added, app builds, 439/444 upstream tests pass (see Known issues), app renamed to "My Voice Keyboard" (`com.tyraen.voicekeyboard.mine`) so it installs beside the original.

- Phase 0 (phone side) ✅ 2026-11-02: installed on Galaxy S24+ (SM-S9260, Android 16) over USB, keyboard enabled, mic granted, Groq key set for speech + post-processing, dictation works.

- Phase 1 ✅ 2026-11-02: codebase tour written to `docs/ARCHITECTURE-NOTES.md`.

- 2026-11-03: switching set up without code: Auto-start recording + Return to previous keyboard on. Route that works: long-press Gboard's space bar → My Voice Keyboard (records at once) → stop → text typed → Gboard back by itself (checked in log). Gboard's globe key only cycles Gboard's own languages.

## Next
- Redesign of the keyboard and app (decided 2026-11-03 to stop merging upstream): collect wishes → DESIGN.md → plan → small steps. Decide where the Fix button and learning features sit in the new layout.
- Phase 2 (plan approved 2026-11-02), 5 steps: ✅ 1 diff + tests · ✅ 2 JSON storage + tests · ✅ 3 keep raw text for last dictation (checked on phone via log) · ✅ 4 Learning screen (switch, list, delete all; checked on phone) · 5 Fix button + panel (editing in the box checked on phone 2026-11-02: tap places the cursor, delete works; still to check: Save → correction appears in the Learning list).
- Fix panel can't type Japanese/Chinese: its letter keys are Latin/Cyrillic/etc. only (upstream on purpose). Next: plan a "Fix in Gboard" button — edit in the real text box with Gboard, switch back, the keyboard reads the box and saves what changed.
- Accuracy test 2026-11-02 (5 Cantonese+English sentences, Samsung Notes):
  - Whisper alone (AUTO or YUE): always writes formal written Chinese, not colloquial Cantonese; English words inside Cantonese get misheard (midterm → "meat term", "sem會學" → 社會學, 影低相 → 營地相). YUE was worse (stray "7", "1Drive", "Farmid Time").
  - Plus AI cleanup (Fix on, custom mixed-language prompt, gpt-oss-120b): colloquial Cantonese came back in 2 of 5 sentences, partly in 1; misheard English words not recovered, some translated (project → 項目) or made worse (OneDrive → 運載).
  - Next idea to test: Whisper prompt written in Hong Kong code-mixed Cantonese, and a "never translate English words" rule.

## Open questions for later phases
- Phase 3: CJK segments can be a single character (新 → 原 rather than 新宿 → 原宿); the stored before/after context carries the rest. Decide whether matching needs to widen segments.
- Confirm: Gboard stays the typing keyboard, this one is for voice only.

## Known issues
- Speech came out in Japanese for English speech (2026-11-02). Cause: the Whisper "Prompt (formatting hint)" was upstream's Japanese example sentence (interface language is Japanese), and with Dictation Languages empty Whisper follows the prompt's language. Fix: a custom prompt mixing all four languages, or an empty prompt. `TranscriptionLocale.promptFor` keeps a built-in prompt when no language is set.
- 5 upstream tests in `HallucinationFilterTest` fail on this PC. Cause: we test on Java 21, upstream CI uses Java 17, and Java 19+ changed regex `\b` to only treat English letters as word letters, so Russian patterns like `\bсубтитр` stop matching. Tests-only: Android uses its own regex engine, so the app on the phone is unaffected. Fix if needed: install JDK 17 and build with it.
- Some screens still say "Voice Keyboard" (upstream hard-codes it in a few strings). Cosmetic; left alone to keep upstream merges clean.
- Delete key in the keyboard's own edit boxes did nothing (upstream bug, also in its instruction box): it sent a key press, and Android drops key presses for the keyboard window because that window never has focus (phone log: "Cancelling event (no window focus)"). Fixed 2026-11-02 by deleting through the box's text directly.
- Leave the in-app "check for updates" off: it would offer the original app's releases.
