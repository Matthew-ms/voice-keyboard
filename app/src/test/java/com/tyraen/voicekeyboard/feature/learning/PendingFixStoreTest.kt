package com.tyraen.voicekeyboard.feature.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PendingFixStoreTest {

    @get:Rule val folder = TemporaryFolder()

    private val fix = PendingFix(
        rawTranscript = "你幫我check下有冇營地相",
        language = "yue",
        appPackage = "jp.naver.line.android",
        snapshot = FieldSnapshot("Hi! ", "你幫我check下有冇營地相", " ", beforeIsStart = true, afterIsEnd = true),
        createdAt = 1_000_000L
    )

    private fun file() = File(folder.root, "learning/pending_fix.json")

    @Test fun `a saved fix comes back unchanged`() {
        val store = PendingFixStore(file())
        store.save(fix)
        assertEquals(fix, store.take())
    }

    @Test fun `taking it removes it so it is handled once`() {
        val store = PendingFixStore(file())
        store.save(fix)
        store.take()
        assertFalse(file().exists())
        assertNull(store.take())
    }

    @Test fun `an unreadable file is dropped instead of crashing`() {
        file().parentFile!!.mkdirs()
        file().writeText("{not json")
        assertNull(PendingFixStore(file()).take())
        assertFalse(file().exists())
    }

    @Test fun `a fix expires after ten minutes or when the clock went back`() {
        assertFalse(fix.expired(fix.createdAt + PendingFix.MAX_AGE_MS))
        assertTrue(fix.expired(fix.createdAt + PendingFix.MAX_AGE_MS + 1))
        assertTrue(fix.expired(fix.createdAt - 1))
    }
}
