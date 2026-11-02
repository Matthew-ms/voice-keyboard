# Decisions

One line per decision and why.

- 2026-11-01 — Fork rustemar/voice-keyboard instead of building from scratch: it already has the IME, Whisper calls, LLM cleanup and vocabulary.
- 2026-11-01 — applicationId `com.tyraen.voicekeyboard.mine`, namespace unchanged: installs beside the original while keeping code paths identical to upstream for easy merges.
- 2026-11-01 — Groq for both speech and cleanup: free tier, one key, upstream default.
- 2026-11-01 — Whisper language left on auto-detect: user mixes Japanese, English, Mandarin and Cantonese in one sentence.
- 2026-11-01 — Learned terms will feed upstream's existing Recognition hints / Local replacements / Post-processing vocabulary rather than a parallel system (to confirm in Phase 2).
- 2026-11-01 — Diff will tokenize CJK per character, Latin per word: word-level diff can't see changes inside unspaced Japanese/Chinese.
- 2026-11-02 — Build with Java 21 (~/.jdks/jbr-21.0.11), not Android Studio's Java 25: upstream's Gradle 8.5 only runs on Java ≤21; not upgrading Gradle keeps us identical to upstream.
- 2026-11-02 — Corrections stored in a JSON file (org.json, like upstream), not Room: no new library or build plugin; easy JSON export later.
- 2026-11-02 — Diff detects scripts with Character.UnicodeScript, not regex script classes: Android (ICU) and the JVM test runner disagree on regex Unicode behaviour (see the \b test failures).
- 2026-11-02 — Rewrite rule (>50% changed) only applies to dictations of 4+ tokens: short dictations are often fixed whole ("Jon" → "John") and those are the most useful pairs.
- 2026-11-02 — Dictation languages "auto, en, ja, zh, yue" with an AUTO key position: Whisper picks one language per recording and often guesses wrong on short or mixed sentences, so the user forces a language and returns to AUTO for mixed ones. Whisper prompt is a custom mixed-language sentence (a built-in one pulls output toward its language).
- 2026-11-02 — Delete in the keyboard's own boxes edits the text directly (deleteSurroundingTextInCodePoints), not a Delete key press: Android drops key presses for the keyboard window. Normal fields keep the key press, which apps expect.
- 2026-11-02 — AI cleanup ("Fix errors") on, with a custom mixed-language prompt and recent-dictation context: in testing it restored colloquial Cantonese that Whisper turns into formal Chinese. Language key stays on AUTO: YUE made Whisper worse.
- 2026-11-03 — Stop merging upstream updates and redesign the keyboard and app freely: the redesign rewrites files upstream keeps changing, so merges would conflict constantly. Upstream fixes can still be copied by hand.
