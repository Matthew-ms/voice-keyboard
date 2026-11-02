package com.tyraen.voicekeyboard.feature.learning

/**
 * Finds what the user changed in a dictation, so it can be learned as "heard → meant" pairs.
 *
 * Text is compared token by token: a word for scripts that put spaces between words, a single
 * character for Chinese and Japanese, which do not. Punctuation is not a token, so a moved comma
 * is never learned. Script detection uses [Character.UnicodeScript] rather than regex script
 * classes, because Android's regex engine (ICU) and the JVM running the unit tests disagree on
 * those; this code must behave the same in both places.
 */
object CorrectionDiff {

    /** One changed stretch, with a few unchanged tokens of the dictation on each side. */
    data class Segment(val heard: String, val meant: String, val before: String, val after: String)

    enum class Ignored { UNCHANGED, DELETED, APPENDED, CASE_OR_PUNCTUATION, REWRITE }

    /** Either [segments] to learn from, or the reason there is nothing to learn. */
    data class Result(val segments: List<Segment>, val ignored: Ignored?)

    internal data class Token(val text: String, val start: Int, val end: Int)

    const val CONTEXT_TOKENS = 3
    const val REWRITE_RATIO = 0.5
    /** Short dictations are often fixed whole ("Jon" → "John"); the rewrite rule would drop them. */
    const val REWRITE_MIN_TOKENS = 4

    fun compare(inserted: String, final: String): Result {
        if (final.isBlank()) return ignore(Ignored.DELETED)
        if (inserted == final) return ignore(Ignored.UNCHANGED)
        val a = tokenize(inserted)
        val b = tokenize(final)
        if (a.map { it.text.lowercase() } == b.map { it.text.lowercase() }) return ignore(Ignored.CASE_OR_PUNCTUATION)
        if (final.trim().startsWith(inserted.trim())) return ignore(Ignored.APPENDED)

        // ponytail: O(n·m) table; fine for dictation-sized text, swap for Myers if fields get huge.
        val n = a.size
        val m = b.size
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
            lcs[i][j] = if (a[i].text == b[j].text) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        if (n >= REWRITE_MIN_TOKENS && (n - lcs[0][0]).toDouble() / n > REWRITE_RATIO) return ignore(Ignored.REWRITE)

        val segments = mutableListOf<Segment>()
        var i = 0
        var j = 0
        var runA = -1
        var runB = -1
        fun flush() {
            if (runA < 0) return
            val heard = span(inserted, a, runA, i)
            val meant = span(final, b, runB, j)
            if (!heard.equals(meant, ignoreCase = true)) {
                segments += Segment(
                    heard, meant,
                    before = span(inserted, a, maxOf(0, runA - CONTEXT_TOKENS), runA),
                    after = span(inserted, a, i, minOf(n, i + CONTEXT_TOKENS))
                )
            }
            runA = -1
            runB = -1
        }
        while (i < n || j < m) {
            if (i < n && j < m && a[i].text == b[j].text) {
                flush()
                i++
                j++
                continue
            }
            if (runA < 0) {
                runA = i
                runB = j
            }
            if (j >= m || (i < n && lcs[i + 1][j] >= lcs[i][j + 1])) i++ else j++
        }
        flush()
        return if (segments.isEmpty()) ignore(Ignored.CASE_OR_PUNCTUATION) else Result(segments, null)
    }

    internal fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var wordStart = -1
        fun close(end: Int) {
            if (wordStart >= 0) tokens += Token(text.substring(wordStart, end), wordStart, end)
            wordStart = -1
        }
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val len = Character.charCount(cp)
            when {
                isUnspacedScript(cp) -> {
                    close(i)
                    tokens += Token(text.substring(i, i + len), i, i + len)
                }
                Character.isLetterOrDigit(cp) -> if (wordStart < 0) wordStart = i
                // Marks (accents typed as separate characters) and apostrophes ("don't") stay in a word.
                wordStart >= 0 && (isMark(cp) || cp == '\''.code || cp == '’'.code) -> {}
                else -> close(i)
            }
            i += len
        }
        close(text.length)
        return tokens
    }

    private fun isUnspacedScript(cp: Int): Boolean {
        // The long-vowel mark in katakana words (コーヒー) is "common" script, not katakana.
        if (cp == 0x30FC) return true
        return when (Character.UnicodeScript.of(cp)) {
            Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> true
            else -> false
        }
    }

    private fun isMark(cp: Int): Boolean = when (Character.getType(cp)) {
        Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt(), Character.ENCLOSING_MARK.toInt() -> true
        else -> false
    }

    /** Original text covering tokens [from, to), with its own spacing and punctuation. */
    private fun span(text: String, tokens: List<Token>, from: Int, to: Int): String =
        if (from >= to) "" else text.substring(tokens[from].start, tokens[to - 1].end)

    private fun ignore(reason: Ignored) = Result(emptyList(), reason)
}
