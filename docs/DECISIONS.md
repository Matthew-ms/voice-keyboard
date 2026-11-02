# Decisions

One line per decision and why.

- 2026-11-01 — Fork rustemar/voice-keyboard instead of building from scratch: it already has the IME, Whisper calls, LLM cleanup and vocabulary.
- 2026-11-01 — applicationId `com.tyraen.voicekeyboard.mine`, namespace unchanged: installs beside the original while keeping code paths identical to upstream for easy merges.
- 2026-11-01 — Groq for both speech and cleanup: free tier, one key, upstream default.
- 2026-11-01 — Whisper language left on auto-detect: user mixes Japanese, English, Mandarin and Cantonese in one sentence.
- 2026-11-01 — Learned terms will feed upstream's existing Recognition hints / Local replacements / Post-processing vocabulary rather than a parallel system (to confirm in Phase 2).
- 2026-11-01 — Diff will tokenize CJK per character, Latin per word: word-level diff can't see changes inside unspaced Japanese/Chinese.
- 2026-11-02 — Build with Java 21 (~/.jdks/jbr-21.0.11), not Android Studio's Java 25: upstream's Gradle 8.5 only runs on Java ≤21; not upgrading Gradle keeps us identical to upstream.
