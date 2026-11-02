# Architecture notes

A map of the upstream code and where our learning features plug in. Paths are under
`app/src/main/java/com/tyraen/voicekeyboard/`. Written 2026-11-02 against upstream `742208c`.

## Glossary
- **IME** (input method editor): Android's name for a keyboard app. Android starts our IME service when a text field wants a keyboard.
- **InputConnection**: the "pipe" from the keyboard into the text field of the app you're typing in. Everything we insert, read back or replace goes through it.
- **Coroutine**: Kotlin's way of doing slow work (network calls) without freezing the screen.

## The map

| Folder | What lives there | Key files |
|---|---|---|
| `feature/ime/` | The keyboard itself: buttons, panels, inserting text, the processing queue | `DictationInputMethod.kt` (the Android keyboard service), `InputOrchestrator.kt` (record → queue), `ProcessingQueue.kt` (speech → cleanup → insert), `KeystrokeDispatcher.kt` (writes into the field) |
| `feature/audio/` | Microphone recording, silence detection | `MicrophoneCaptureSession.kt` |
| `feature/transcription/` | Calls to the speech API (Groq/Whisper) | `WhisperApiClient.kt`, `SpeechToTextClient.kt` (`WhisperPromptBuilder`), `HallucinationFilter.kt` |
| `feature/postprocessing/` | Calls to the LLM that cleans the text, and its prompts | `PostProcessingPrompts.kt`, `PostProcessingClient.kt`, `RecentDictationContext.kt` |
| `feature/vocabulary/` | Custom vocabulary + local "heard → meant" replacements, and their settings screen | `TextDictionary.kt`, `TextReplacementRules.kt`, `VocabularyActivity.kt` |
| `feature/setup/` | Settings screens | `SetupActivity.kt` (main), `AdvancedSettingsActivity.kt` (experimental switches) |
| `core/config/` | Saved settings | `PreferenceStore.kt`, `TextDictionaryPreferences.kt` |
| `app/` | App start-up and shared services | `ServiceLocator.kt` (one place that creates the queue, API clients, stores) |
| `feature/recognition/` | Voice input for *other* apps (not the keyboard). We don't touch this. | |

## What happens when you dictate

1. **Tap mic**: `DictationInputMethod` → `InputOrchestrator.beginCapture()` starts recording.
2. **Tap again**: `InputOrchestrator.finishCaptureAndEnqueue()` → `enqueue()` takes a snapshot of your settings. That includes the **Whisper prompt** built by `WhisperPromptBuilder.build(style, vocabulary)` (`InputOrchestrator.kt:420`).
3. **`ProcessingQueue.processItem()`** (`ProcessingQueue.kt:366`) runs in the background:
   - `speechClient.transcribe()` → **`rawText`**: exactly what the speech model heard.
   - `TextDictionary.prepare()` → local replacements applied → **`cleanedText`**.
   - `maybePostProcess()` → the LLM fixes it, prompt from `PostProcessingPrompts.build()` → **`output`**.
   - `deliver()` → `listener.onTextReady(output, …)`.
4. **Insert**: `DictationInputMethod.kt:125` checks it's still the same field, then `KeystrokeDispatcher.insertDictation()` commits it. If you've moved to another field, it goes to "Saved texts" instead.

## Where each new feature plugs in

**Correction capture data (all phases).** `processItem()` knows `rawText`, `cleanedText` and `output`, but only `output` reaches the keyboard. We need one small hook: after a successful insert, hand all three (plus app name, field, language) to our new `learning/` code. That's the main upstream file we'll touch. Keep it to a few lines.

**Fix panel (Phase 2).**
- Button: next to the existing keys in `res/layout/input_panel.xml`, wired in `DictationInputMethod.wireControls()`.
- Edit box: upstream already solved typing into a box *inside* the keyboard. `QuickCorrectionPanel.showForInstruction(editText, onDone)` is used by Selected-text actions. We reuse it.
- Safe replace: `SelectedTextSnapshot` + `SelectedTextActionsPanel.applyResult()` already do "replace only if the field still holds the exact text". We copy that pattern.
- Note: the letter keys come from the *Quick corrections · Beta* layout, which you'll need to switch on (Advanced settings).

**Storage (Phase 2).** Upstream stores its own data as JSON files in the app's private folder (`PendingTranscriptStore` uses `org.json`). We do the same for corrections, which means no new database library. Registered in `ServiceLocator`.

**Using corrections (Phase 3).**
- Whisper side: extend the `vocabulary` passed to `WhisperPromptBuilder.build()` with approved learned terms. Whisper's prompt limit is ~224 tokens, shared with your custom vocabulary.
- LLM side: `PostProcessingPrompts.build()` already adds a vocabulary hint and recent context. We add one more part: "known corrections (heard → meant)".
- Debug view: `DiagnosticLog.record()` writes to the app log, and settings already has **Save Logs**. We log the prompt we sent there.

**Passive capture (Phase 4).**
- Enter/Send: `btnEnter` / `btnSend` click handlers in `DictationInputMethod.wireControls()` (around line 583–611). Read the field *before* calling `pressEnter`/`sendCtrlEnter`.
- Leaving the field: `onStartInput` / `onFinishInput` (`DictationInputMethod.kt:194`, `:216`).
- Privacy check already exists at `DictationInputMethod.kt:208`: password fields and `IME_FLAG_NO_PERSONALIZED_LEARNING` are detected there. Reuse the same condition.

**Auto-vocabulary + review (Phase 5).** Approved terms can be written into upstream's existing lists (Recognition hints / Local replacements, `TextDictionaryPreferences`). The review screen is a new activity linked from `SetupActivity` next to Custom vocabulary (`SetupActivity.kt:306`).

**Toggles.** Experimental switches live in `AdvancedSettingsActivity`. "Learn from my corrections" goes there, off by default.

## Things already handled by upstream
- **Cloud backup is off for all app data** (`allowBackup=false`, `data_extraction_rules.xml` excludes everything). Our learning files are covered automatically.
- Mic is blocked in password fields (`EditorField.blocksVoice`).

## Gotchas
- The keyboard can only read the field while it's the active keyboard. Edits you make in Gboard are invisible to it.
- `onTextReady` refuses to insert into a different field than the one you started recording in. Correction tracking must use the same `editorSessionToken` idea.
- Japanese/Chinese: no spaces between words, so the diff must tokenize CJK per character.
- 5 `HallucinationFilterTest` tests fail on Java 21 (see PROGRESS.md). Not caused by us.
