package com.tyraen.voicekeyboard.feature.learning

import com.tyraen.voicekeyboard.feature.learning.CorrectionDiff.Ignored
import com.tyraen.voicekeyboard.feature.learning.CorrectionDiff.Segment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CorrectionDiffTest {

    private fun learned(inserted: String, final: String): List<Segment> {
        val result = CorrectionDiff.compare(inserted, final)
        assertNull("expected something to learn, got ${result.ignored}", result.ignored)
        return result.segments
    }

    private fun ignored(inserted: String, final: String) = CorrectionDiff.compare(inserted, final).ignored

    private fun tokens(text: String) = CorrectionDiff.tokenize(text).map { it.text }

    @Test fun `one misheard word is learned with three words of context`() {
        assertEquals(
            listOf(Segment("Jon", "John", before = "I met with", after = "at the café")),
            learned("Yesterday I met with Jon at the café today.", "Yesterday I met with John at the café today.")
        )
    }

    @Test fun `a misheard phrase becomes one segment`() {
        assertEquals(
            listOf(Segment("cube control", "kubectl", before = "Please run", after = "get pods now")),
            learned("Please run cube control get pods now", "Please run kubectl get pods now")
        )
    }

    @Test fun `two separate fixes become two segments`() {
        val segments = learned("send it to Jon and Mery tomorrow morning", "send it to John and Mary tomorrow morning")
        assertEquals(listOf("Jon" to "John", "Mery" to "Mary"), segments.map { it.heard to it.meant })
    }

    @Test fun `an inserted missing word is learned with an empty heard side`() {
        val segments = learned("I want go home now please", "I want to go home now please")
        assertEquals(listOf(Segment("", "to", before = "I want", after = "go home now")), segments)
    }

    @Test fun `japanese is compared per character`() {
        assertEquals(
            listOf(Segment("新", "原", before = "明日は", after = "宿に行")),
            learned("明日は新宿に行きます。", "明日は原宿に行きます。")
        )
    }

    @Test fun `mixed chinese and english finds the english word`() {
        assertEquals(
            listOf(Segment("coffee", "咖啡", before = "去Starbucks買", after = "")),
            learned("我想去Starbucks買coffee", "我想去Starbucks買咖啡")
        )
    }

    @Test fun `a short dictation fixed whole is still learned`() {
        assertEquals(listOf(Segment("Jon", "John", "", "")), learned("Jon", "John"))
    }

    @Test fun `deleting the dictation is not learned`() {
        assertEquals(Ignored.DELETED, ignored("hello there", ""))
        assertEquals(Ignored.DELETED, ignored("hello there", "   "))
    }

    @Test fun `no change is not learned`() {
        assertEquals(Ignored.UNCHANGED, ignored("hello there", "hello there"))
    }

    @Test fun `only adding text after the dictation is not learned`() {
        assertEquals(Ignored.APPENDED, ignored("I'll be late", "I'll be late, sorry about that"))
    }

    @Test fun `capitalization or punctuation alone is not learned`() {
        assertEquals(Ignored.CASE_OR_PUNCTUATION, ignored("hello there how are you", "Hello there, how are you?"))
        assertEquals(Ignored.CASE_OR_PUNCTUATION, ignored("東京に行きます", "東京に、行きます。"))
    }

    @Test fun `a case-only change next to a real fix is left out`() {
        assertEquals(
            listOf("Jon" to "John"),
            learned("tell paris that Jon is here", "tell Paris that John is here").map { it.heard to it.meant }
        )
    }

    @Test fun `changing more than half the words is a rewrite`() {
        assertEquals(Ignored.REWRITE, ignored("can we meet at five today", "let's do lunch tomorrow instead today"))
    }

    @Test fun `tokens split unspaced scripts per character and keep words whole`() {
        assertEquals(listOf("Hello", "世", "界"), tokens("Hello世界"))
        assertEquals(listOf("don't", "stop"), tokens("don't stop!"))
        assertEquals(listOf("コ", "ー", "ヒ", "ー", "を"), tokens("コーヒーを"))
        assertEquals(listOf("café", "naïve"), tokens("café, naïve."))
        assertEquals(listOf("été"), tokens("été"))
    }
}
