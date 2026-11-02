package com.tyraen.voicekeyboard.feature.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastDictationTest {

    private fun count(before: String?, inserted: String) = LastDictation.charsToReplace(before, inserted)

    @Test fun `the dictation right before the cursor is replaced`() {
        assertEquals(5, count("Hello there", "there"))
        assertEquals(11, count("Hello there", "Hello there"))
    }

    @Test fun `the space the keyboard added after it is included`() {
        assertEquals(6, count("Hello there ", "there"))
    }

    @Test fun `japanese without spaces is matched exactly`() {
        assertEquals(10, count("明日は新宿に行きます", "明日は新宿に行きます"))
    }

    @Test fun `text typed after the dictation stops the replacement`() {
        assertNull(count("Hello there, friend", "there"))
        assertNull(count("Hello there  ", "there"))
    }

    @Test fun `an unreadable field or empty dictation stops the replacement`() {
        assertNull(count(null, "there"))
        assertNull(count("Hello", ""))
    }
}
