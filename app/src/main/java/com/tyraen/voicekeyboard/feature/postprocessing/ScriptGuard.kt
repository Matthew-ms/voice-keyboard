package com.tyraen.voicekeyboard.feature.postprocessing

/**
 * Catches a cleanup result that translated the dictation: English letters, Japanese kana or
 * Chinese characters that mostly vanished. A model can ignore "never translate" in its prompt;
 * this check cannot be ignored. It cannot tell Mandarin from Cantonese (both are Han), so
 * rewriting 不要 as 唔好 still passes.
 */
object ScriptGuard {
    /** A script needs this many characters in the input before it is checked at all. */
    private const val MIN_CHARS = 3
    /** The result must keep at least this share of each checked script. */
    private const val MIN_KEPT = 0.5

    private enum class Script { LATIN, KANA, HAN }

    fun keepsScripts(input: String, output: String): Boolean {
        val before = count(input)
        val after = count(output)
        return before.all { (script, n) -> n < MIN_CHARS || (after[script] ?: 0) >= n * MIN_KEPT }
    }

    private fun count(text: String): Map<Script, Int> {
        val counts = HashMap<Script, Int>()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            i += Character.charCount(cp)
            val script = when (Character.UnicodeScript.of(cp)) {
                Character.UnicodeScript.LATIN -> if (Character.isLetter(cp)) Script.LATIN else null
                Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> Script.KANA
                Character.UnicodeScript.HAN -> Script.HAN
                else -> if (cp == 0x30FC) Script.KANA else null // ー is "common" script
            } ?: continue
            counts[script] = (counts[script] ?: 0) + 1
        }
        return counts
    }
}
