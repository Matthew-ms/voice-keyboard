package com.tyraen.voicekeyboard.feature.learning

import android.view.inputmethod.EditorInfo
import com.tyraen.voicekeyboard.feature.ime.EditorField

/** The latest dictation typed into the current field. Memory only; dropped when the field changes. */
data class LastDictation(
    val rawTranscript: String,
    val insertedText: String,
    val language: String,
    val appPackage: String,
    val editorSession: Long,
    val createdAt: Long
) {
    companion object {
        /** Never learn in password fields or where the app asks keyboards not to (incognito). */
        fun allowedIn(info: EditorInfo?): Boolean = info != null &&
            !EditorField.isPassword(info.inputType) &&
            info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0

        /**
         * How many characters before the cursor are the dictation (plus the space the keyboard
         * added after it), or null when the field no longer ends with it there.
         */
        fun charsToReplace(beforeCursor: CharSequence?, inserted: String): Int? = when {
            beforeCursor == null || inserted.isEmpty() -> null
            beforeCursor.endsWith("$inserted ") -> inserted.length + 1
            beforeCursor.endsWith(inserted) -> inserted.length
            else -> null
        }
    }
}
