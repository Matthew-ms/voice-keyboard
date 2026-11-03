package com.tyraen.voicekeyboard.feature.postprocessing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptGuardTest {

    @Test fun `English words turned into Chinese are caught`() {
        assertFalse(ScriptGuard.keepsScripts(
            "我不知道為什麼這份project會上載不到,是不是monica搞錯了些什麼?",
            "我唔知點解呢個項目上載唔到，係咪佢搞錯咗啲乜？"))
    }

    @Test fun `a whole English sentence translated is caught`() {
        assertFalse(ScriptGuard.keepsScripts("I'll be there in fifteen minutes", "我十五分鐘後到"))
    }

    @Test fun `Japanese turned into Chinese is caught`() {
        assertFalse(ScriptGuard.keepsScripts("今日はとても忙しかったです", "今天非常忙"))
    }

    @Test fun `Chinese turned into English is caught`() {
        assertFalse(ScriptGuard.keepsScripts("我今天很好", "I'm fine today"))
    }

    @Test fun `Mandarin rewritten as colloquial Cantonese passes`() {
        assertTrue(ScriptGuard.keepsScripts(
            "我昨天不是跟你說了嗎?Professor說",
            "我琴日唔係同你講咗咩？Professor話"))
    }

    @Test fun `a misheard English word fixed passes`() {
        assertTrue(ScriptGuard.keepsScripts("Professor 說 這個meat term只考", "Professor話份midterm淨係考"))
    }

    @Test fun `filler words removed and punctuation added pass`() {
        assertTrue(ScriptGuard.keepsScripts("um so I think uh we should go", "So I think we should go."))
    }

    @Test fun `short bits below the threshold are not checked`() {
        assertTrue(ScriptGuard.keepsScripts("ok 好", "好"))
    }

    @Test fun `empty output fails when there was text`() {
        assertFalse(ScriptGuard.keepsScripts("hello there", ""))
    }
}
