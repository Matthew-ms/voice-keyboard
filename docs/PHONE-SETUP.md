# Phone setup (Samsung Galaxy S24+)

One-time setup so the PC can install the app on your phone over a USB cable.
Menu names are from One UI 6/7; if a name differs slightly, use the search icon (magnifier, top of Settings).

## Stage 1 — Turn off Auto Blocker (on the phone)

Do this first. On Samsung, Auto Blocker silently blocks app installs over USB; the install then fails with
an error that looks unrelated.

1. Open **Settings** (the gear icon in the app drawer).
2. Tap **Security and privacy**.
3. Tap **Auto Blocker**.
4. Turn the switch at the top **off**. If asked for your PIN, enter it.

✅ Success: the Auto Blocker screen shows **Off**.
If you don't see **Auto Blocker**, your phone doesn't have it — skip to Stage 2.

## Stage 2 — Turn on Developer options (on the phone)

1. In **Settings**, scroll to the bottom and tap **About phone**.
2. Tap **Software information**.
3. Tap **Build number** seven times in a row.
4. Enter your PIN if asked.

✅ Success: a small message says **"Developer mode has been turned on."**
A new menu **Developer options** now appears at the bottom of Settings, just below **About phone**.

## Stage 3 — Turn on USB debugging (on the phone)

"USB debugging" lets the PC install apps on the phone. It's safe to leave on while we work; turn it off later if you like.

1. Go back to the main **Settings** screen.
2. Scroll to the bottom and tap **Developer options**.
3. Scroll down to the **Debugging** heading.
4. Turn on **USB debugging**.
5. Tap **OK** on the warning.

✅ Success: the **USB debugging** switch is blue.

## Stage 4 — Connect the phone (phone + PC)

1. Unlock the phone.
2. Plug the phone into the PC with a USB-C cable that can carry data (some charging-only cables can't).
3. **On the phone**, a box appears: **"Allow USB debugging?"**
4. Tick **Always allow from this computer**.
5. Tap **Allow**.
6. **Back in Claude**, tell me "phone connected". I'll check the connection and install the app.

If no box appears:
- Swipe down from the top of the phone screen to open notifications.
- Tap the notification about USB (e.g. **"Charging this device via USB"**).
- Choose **Transferring files / Android Auto**. The box should appear now.
- Still nothing? Try another cable or USB port, then tell me.

## Stage 5 — Enable the keyboard (on the phone)

After I tell you the install succeeded:

1. Open the app drawer and tap **My Voice Keyboard**.
2. Tap **Enable keyboard**. Android's keyboard list opens.
3. Turn on **My Voice Keyboard** in that list.
4. Android warns that a keyboard can read what you type. Tap **OK**. (Normal for every keyboard; this one only reads what it needs.)
5. Press the back button to return to the app.

✅ Success: the app no longer says it isn't enabled. (Some screens still say "Voice Keyboard" — that's
upstream's text; it's the right app.)

## Stage 6 — Add your Groq key (on the phone)

A Groq API key is like a password that lets the app use Groq's speech service. It's free.

1. On the phone, open your browser and go to **console.groq.com/keys**.
2. Sign in (for example with Google).
3. Tap **Create API Key**.
4. Type any name, e.g. `keyboard`, and tap **Submit**.
5. Tap the copy icon next to the key. It starts with `gsk_`. **Copy it now — Groq only shows it once.**
6. Open **My Voice Keyboard** again.
7. Long-press the **Speech-to-text API key** field and tap **Paste**.
8. Tap **Apply**.

✅ Success: you see **"Settings saved."**
If you get an error, read the text in brackets: it's Groq's own message (usually a key copied incompletely). Copy it again.

Leave **Dictation Languages** empty. Empty means "detect automatically", which is what you need for mixing Japanese, English, Mandarin and Cantonese.

## Stage 7 — Turn on the text cleanup (on the phone)

1. In the app, tap **Post-processing settings**.
2. Turn on **Enable post-processing**.
3. Under **Provider preset**, choose **Groq**.
4. Paste the same `gsk_` key into **API Key**.
5. Tap **Apply** at the bottom.

✅ Success: **"Settings saved."**

## Stage 8 — Test it (on the phone)

1. Open any app with a text box (e.g. Samsung Notes).
2. Tap the text box so a keyboard appears.
3. Switch to My Voice Keyboard: tap the small keyboard icon at the bottom right of the screen (or long-press the space bar in Gboard) and choose **My Voice Keyboard**.
4. Allow the microphone if asked (tap **I understand, continue**, then **While using the app**).
5. Tap the mic and say one sentence in English. Tap the mic again to stop.
6. Repeat with a Japanese sentence, then a sentence that mixes languages.

✅ Success: the text appears in the box. Tell me how accurate each one was — that's our "before" picture.
If you see **"API key not set — add it in settings"**, redo Stage 6 and make sure you tapped **Apply**.
