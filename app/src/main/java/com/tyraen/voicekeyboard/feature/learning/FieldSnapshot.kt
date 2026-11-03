package com.tyraen.voicekeyboard.feature.learning

/**
 * Where a dictation sat in its field: the text around it serves as markers to find the user's
 * edited version after they correct it with another keyboard.
 */
data class FieldSnapshot(
    val before: String,
    val inserted: String,
    val after: String,
    /** [before] runs to the start of the field, so it must still start the field. */
    val beforeIsStart: Boolean,
    /** [after] runs to the end of the field, so it must still end the field. */
    val afterIsEnd: Boolean
) {
    /** What now sits between the markers, or null when the user changed a marker too. */
    fun locate(field: String): String? {
        val start = if (beforeIsStart) {
            if (field.startsWith(before)) before.length else return null
        } else {
            field.indexOf(before).takeIf { it >= 0 }?.plus(before.length) ?: return null
        }
        val end = if (afterIsEnd) {
            if (field.endsWith(after)) field.length - after.length else return null
        } else {
            field.indexOf(after, start).takeIf { it >= 0 } ?: return null
        }
        return if (end >= start) field.substring(start, end) else null
    }

    companion object {
        /** Characters of field text kept on each side of the dictation. */
        const val CONTEXT = 200

        /** Characters to ask the field for before the cursor, so [capture] can tell where the field starts. */
        fun charsBefore(inserted: String) = inserted.length + 1 + CONTEXT

        /**
         * Snapshot taken with the cursor right after the dictation: [beforeCursor] read with
         * [charsBefore], [afterCursor] read with [CONTEXT]. Null when the dictation is no longer there.
         */
        fun capture(beforeCursor: CharSequence?, afterCursor: CharSequence?, inserted: String): FieldSnapshot? {
            val n = LastDictation.charsToReplace(beforeCursor, inserted) ?: return null
            val text = beforeCursor.toString()
            val tail = afterCursor?.toString() ?: return null
            return FieldSnapshot(
                before = text.dropLast(n),
                inserted = inserted,
                // The space the keyboard added after the dictation is part of the right-hand marker.
                after = text.takeLast(n - inserted.length) + tail,
                beforeIsStart = text.length < charsBefore(inserted),
                afterIsEnd = tail.length < CONTEXT
            )
        }
    }
}
