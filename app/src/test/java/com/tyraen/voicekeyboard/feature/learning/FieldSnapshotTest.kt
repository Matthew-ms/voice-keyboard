package com.tyraen.voicekeyboard.feature.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldSnapshotTest {

    private val dictation = "你幫我check下有冇營地相"

    @Test fun `the corrected dictation is found in a field that held only it`() {
        val snap = FieldSnapshot.capture(dictation, "", dictation)!!
        assertEquals("你幫我check下有冇影低相", snap.locate("你幫我check下有冇影低相"))
    }

    @Test fun `the trailing space stays outside the dictation`() {
        val snap = FieldSnapshot.capture("$dictation ", "", dictation)!!
        assertEquals(" ", snap.after)
        assertEquals("你幫我check下有冇影低相", snap.locate("你幫我check下有冇影低相 "))
    }

    @Test fun `text before and after the dictation are markers`() {
        val snap = FieldSnapshot.capture("Hi! $dictation", " 唔該", dictation)!!
        assertEquals("Hi! ", snap.before)
        assertEquals("你幫我check下有冇影低相", snap.locate("Hi! 你幫我check下有冇影低相 唔該"))
    }

    @Test fun `editing outside the dictation means it cannot be found`() {
        val snap = FieldSnapshot.capture("Hi! $dictation", " 唔該", dictation)!!
        assertNull(snap.locate("Hello! 你幫我check下有冇影低相 唔該"))
        assertNull(snap.locate("Hi! 你幫我check下有冇影低相 thanks"))
    }

    @Test fun `a sent message leaves an empty field that cannot match`() {
        val snap = FieldSnapshot.capture("Hi! $dictation", "", dictation)!!
        assertNull(snap.locate(""))
    }

    @Test fun `deleting the whole dictation is found as empty`() {
        val snap = FieldSnapshot.capture("Hi! $dictation ", "", dictation)!!
        assertEquals("", snap.locate("Hi!  "))
    }

    @Test fun `long text before the dictation is matched anywhere, not only at the start`() {
        val earlier = (1..60).joinToString(" ") { "word$it" } + " "
        val read = earlier.takeLast(FieldSnapshot.charsBefore(dictation) - dictation.length) + dictation
        val snap = FieldSnapshot.capture(read, "", dictation)!!
        assertFalse(snap.beforeIsStart)
        assertTrue(snap.afterIsEnd)
        assertEquals("改好咗", snap.locate(earlier + "改好咗"))
    }

    @Test fun `long text after the dictation does not have to end the field`() {
        val later = "y".repeat(FieldSnapshot.CONTEXT)
        val snap = FieldSnapshot.capture(dictation, later, dictation)!!
        assertFalse(snap.afterIsEnd)
        assertEquals("改好咗", snap.locate("改好咗" + later + "more typed later"))
    }

    @Test fun `no snapshot when the dictation is not before the cursor`() {
        assertNull(FieldSnapshot.capture("something else", "", dictation))
        assertNull(FieldSnapshot.capture(dictation, null, dictation))
    }
}
