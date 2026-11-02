# Handover: Personal Voice Keyboard (Android)

> **How to use this file**
> 1. Fork `https://github.com/rustemar/voice-keyboard` on GitHub and clone your fork.
> 2. Put this file in the repo root (or save it as `CLAUDE.md` so Claude Code reads it automatically every session).
> 3. Open Claude Code in that folder and say: **"Read HANDOVER.md and start Phase 0."**
> 4. Fill in the "Open questions" section at the bottom first; Claude Code needs those answers.

---

## 1. Goal

Build a personal Android voice-typing keyboard that is **more accurate for me than Gboard** because it:

- uses a modern speech model (Whisper or similar) plus LLM cleanup,
- knows my vocabulary (names, slang, technical terms, phrases I use often),
- **learns from the corrections I make** to its output, so the same mistake doesn't happen twice.

This is a personal tool. I am the only user, and I don't plan to publish it on Google Play.

## 2. About me (read this, Claude Code)

- I build apps by "vibe coding" with AI agents. I have basic IT knowledge but **I'm not familiar with code structure, Kotlin, or Android internals.**
- Before each change, explain **in plain language** what you're about to do and why. Briefly explain technical terms and tool choices when they come up.
- When something needs me to act (install a tool, plug in the phone, tap a setting), give me exact step-by-step instructions.
- If a decision is mine to make, ask. Don't guess silently.

## 3. Base project: Voice Keyboard (fork, don't rebuild)

- Upstream: `https://github.com/rustemar/voice-keyboard` (MIT license, author: rustemar)
- F-Droid page: `https://f-droid.org/packages/com.tyraen.voicekeyboard/`
- Requires Android 7.0+. It has no speech engine of its own and calls a cloud API with my own key (OpenAI, Groq, Mistral, Anthropic, or any OpenAI-compatible endpoint).

**Already exists. Reuse it, don't rebuild it:**

| Feature | Notes |
|---|---|
| Keyboard (IME) that works in any app | Punctuation keys, backspace, send key, clipboard bar |
| Voice → text via cloud API | Processing queue, offline retry, cancel latest |
| Multiple dictation languages on one key | Tap to cycle, long-press to pick |
| Custom vocabulary | Manual list of names and terms |
| LLM post-processing | Fixes punctuation, spelling, and filler words. Prompts are editable |
| Recent dictation context | Up to 5 recent dictations, **in memory only**, cleared on app switch, idle, or restart |
| Works as the voice input for another keyboard | Can hand back to e.g. HeliBoard or Gboard |
| Mic disabled in password fields | Keep this behavior |

**What's missing (my contribution):** *persistent* learning from my manual corrections, and automatic growth of the vocabulary.

## 4. New features to build

### 4.1 Correction capture: two ways

**A. Explicit "Fix last dictation" panel (build first, most reliable)**
- After a dictation is inserted, show a small "Fix" button on the keyboard.
- Tapping it opens an edit box with the last dictation. I correct it and press Save.
- The keyboard replaces the text in the field (if the field still contains it unchanged) and saves the correction pair.
- This gives clean, certain training data.

**B. Passive capture (build second, best-effort)**
- When the keyboard inserts a dictation, remember: the inserted text, its position in the field, the app package, the field ID, and a timestamp.
- Later, read the field again through `InputConnection` (`getExtractedText` / `getTextBeforeCursor` / `getTextAfterCursor`). Compare against what was inserted and save the difference.
- **When to read the field back** (investigate which hooks work reliably):
  - the Enter/Send key on *this* keyboard is pressed (read **before** performing the action, because the field usually clears after sending),
  - a new dictation starts in the same field,
  - `onFinishInput` / `onStartInput` (moving to another field),
  - an idle timeout (e.g., 60 s).
- **Known limitation:** if I hand back to Gboard to edit and then send from there, this keyboard can't see the final text. That's acceptable. Capture what we can, and don't build fragile workarounds. Explain any other limitation you find.

**Diff rules (for B, and to extract pairs in A):**
- Word-level diff (LCS/Myers) between inserted text and final text.
- Save each changed segment with ~3 words of context on each side.
- **Ignore** (don't learn from) cases where:
  - the whole dictation was deleted (I gave up, not a correction),
  - more than ~50% of the words changed (a rewrite, not a fix),
  - I only appended new text after the dictation,
  - the change is only capitalization or punctuation (configurable).
- Put the diff logic in a pure Kotlin class with **unit tests**. It's the easiest part to test and the easiest to get subtly wrong.

### 4.2 Storage (local only)

Use Room (SQLite) on the device. Suggested tables:

- `corrections`: id, raw_transcript (speech model output), inserted_text (after LLM cleanup), final_text, changed_segments (JSON), language, app_package, source (`explicit` / `passive`), created_at, times_used.
- `learned_terms`: term, replaces (common mishearings), count, status (`suggested` / `approved` / `rejected`), created_at.

Saving both the raw transcript and the cleaned-up text matters. It tells us whether the speech model or the LLM caused the error.

### 4.3 Using what was learned

**Before speech recognition:**
- If the API supports a prompt/hint parameter (Whisper's `prompt`), include approved learned terms along with the existing custom vocabulary. Check how upstream already passes custom vocabulary and extend that path.

**Before LLM post-processing:**
- Select up to ~10 relevant past corrections, scored by:
  - word overlap / fuzzy similarity between the current transcript and each correction's *wrong* segment,
  - frequency and recency.
- Add them to the post-processing prompt as a section like: *"This user's known corrections (heard → meant): …"*
- v1: simple string similarity, **no embeddings or vector DB**.
- Keep the added prompt short (cap the token budget). Never send the whole database.

**Auto-growing vocabulary:**
- When the same replacement happens ≥3 times, create a `suggested` learned term.
- I approve or reject suggestions in settings (default). Add a setting to auto-approve.

### 4.4 Review screen (settings)

- List learned corrections and terms: search, edit, delete, enable/disable.
- Approve/reject suggested terms.
- "Delete all learning data" and "Export / import" (JSON).
- A simple stat: corrections this week. Helps me see whether accuracy is improving.

### 4.5 Later (not now)

- Per-app style (casual in chat apps, formal in email).
- Offline/on-device speech model.
- Smarter retrieval (embeddings) if simple matching isn't enough.

## 5. Privacy rules (must follow)

- **Never** capture or learn in password fields, or in fields flagged `IME_FLAG_NO_PERSONALIZED_LEARNING` / incognito.
- All learning data stays **on the phone**. Exclude it from cloud backup, as upstream already does for API keys.
- Only the few selected correction pairs are ever sent to the API, inside the prompt.
- Add a master toggle: "Learn from my corrections" (on/off).

## 6. Phases & done criteria

**Phase 0: Setup and build the unmodified fork**
- Walk me through installing Android Studio and enabling USB debugging (or wireless debugging) on my phone.
- Change `applicationId` and app name (e.g., "My Voice Keyboard") so it installs **alongside** the original.
- Add the original repo as an `upstream` git remote.
- ✅ Done when: my build installs on my phone, I can select it as a keyboard, and dictation works with my API key.

**Phase 1: Codebase tour**
- Explain the project structure in plain language: where the IME service, recording, transcription call, post-processing, custom vocabulary, and settings live.
- Write a short `docs/ARCHITECTURE-NOTES.md` for future sessions.
- ✅ Done when: I understand roughly where each new feature will go.

**Phase 2: Explicit "Fix" panel + storage**
- ✅ Done when: I fix a dictation, it's saved, and I can see it in a basic list.

**Phase 3: Use corrections in prompts**
- ✅ Done when: after I correct a word once, dictating a similar sentence produces the corrected word (show me the prompt that was sent, in a debug view or log).

**Phase 4: Passive capture**
- ✅ Done when: edits I make in this keyboard before pressing Send are captured, and the ignore rules work (unit tests pass).

**Phase 5: Auto-vocabulary + review screen**
- ✅ Done when: repeated corrections show up as suggestions I can approve, and everything can be deleted or exported.

## 7. Working rules

- Small steps. One feature per commit, with clear commit messages. Build and install on my phone after each step so I can test.
- Put new code in its own package (e.g., `.../learning/`) and keep changes to upstream files minimal. This keeps future upstream updates easy to merge.
- New features sit behind settings toggles, **off by default**, until tested.
- Write unit tests for the diff and scoring logic.
- If something breaks, explain the cause before fixing it.
- Keep a running `docs/PROGRESS.md`: what's done, what's next, known issues. I may continue in a new session.

## 8. Answers (filled in 2026-11-01)

- Speech API provider: Groq (whisper-large-v3-turbo)
- LLM for post-processing: Groq, same key
- Languages: Japanese, English, Mandarin, Cantonese — **mixed inside one sentence**. Leave Whisper's
  language on auto-detect; the diff must tokenize CJK per character (no spaces between words).
- Phone: Samsung Galaxy S24+ (Android 14+, One UI). Samsung "Auto Blocker" must be off for USB installs.
- Keep Gboard for typing, this keyboard for voice: yes (assumed, not yet confirmed)
- Auto-approve learned terms: no, ask me (default)
