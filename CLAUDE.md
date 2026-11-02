# My Voice Keyboard (personal fork of rustemar/voice-keyboard)

@docs/HANDOVER.md

Read before working: `docs/PROGRESS.md` (where we are), `docs/DECISIONS.md` (why things are the way they are).

## Build (Windows)
- Java 21, **not** Android Studio's bundled Java 25 (Gradle 8.5 can't run on 25; the error is just "25.0.3").
  In Git Bash: `export JAVA_HOME="$USERPROFILE/.jdks/jbr-21.0.11"`
- Build + tests: `./gradlew assembleDebug testDebugUnitTest`
- Install on phone: `"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/VoiceKeyboard.apk`

## Git
- `origin` = Matthew-ms/voice-keyboard (our fork), `upstream` = rustemar/voice-keyboard.
- Since 2026-11-03 we no longer merge upstream (see DECISIONS.md): change any file freely. The `upstream` remote stays
  so a specific upstream fix can still be looked at and copied by hand.
- Learning code still lives in `.../learning/`.
