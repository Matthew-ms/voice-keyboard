package com.tyraen.voicekeyboard.feature.learning

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CorrectionStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun correction(id: String, createdAt: Long, final: String = "明日は原宿に行きます。") = Correction(
        id = id,
        rawTranscript = "明日は新宿に行きます",
        insertedText = "明日は新宿に行きます。",
        finalText = final,
        segments = listOf(CorrectionDiff.Segment("新", "原", "明日は", "宿に行")),
        language = "",
        appPackage = "com.samsung.android.app.notes",
        createdAt = createdAt
    )

    private fun TestScope.store(file: File) = CorrectionStore(file, StandardTestDispatcher(testScheduler))

    @Test fun `corrections survive store recreation newest first`() = runTest {
        val file = File(temporary.newFolder(), "learning/corrections.json")
        val first = correction("a", 10L)
        val second = correction("b", 20L, final = "Send it to John 👋\nthanks")
        store(file).apply {
            add(first)
            add(second)
        }
        assertEquals(listOf(second, first), store(file).all())
    }

    @Test fun `adding after a reload keeps earlier corrections`() = runTest {
        val file = File(temporary.newFolder(), "corrections.json")
        store(file).add(correction("a", 1L))
        store(file).add(correction("b", 2L))
        assertEquals(listOf("b", "a"), store(file).all().map { it.id })
    }

    @Test fun `delete all removes the file`() = runTest {
        val file = File(temporary.newFolder(), "corrections.json")
        val store = store(file)
        store.add(correction("a", 1L))
        store.deleteAll()
        assertFalse(file.exists())
        assertEquals(emptyList<Correction>(), store.all())
        assertEquals(emptyList<Correction>(), store(file).all())
    }

    @Test fun `an unreadable file is set aside, not overwritten`() = runTest {
        val dir = temporary.newFolder()
        val file = File(dir, "corrections.json").apply { writeText("{ not json") }
        val store = store(file)
        assertEquals(emptyList<Correction>(), store.all())
        store.add(correction("a", 1L))

        val aside = dir.listFiles()!!.single { it.name.startsWith("corrections.unreadable-") }
        assertEquals("{ not json", aside.readText())
        assertEquals(listOf("a"), store(file).all().map { it.id })
    }

    @Test fun `no temporary files are left behind`() = runTest {
        val dir = temporary.newFolder()
        val store = store(File(dir, "corrections.json"))
        store.add(correction("a", 1L))
        store.add(correction("b", 2L))
        assertTrue(dir.listFiles()!!.none { it.name.endsWith(".tmp") })
    }
}
