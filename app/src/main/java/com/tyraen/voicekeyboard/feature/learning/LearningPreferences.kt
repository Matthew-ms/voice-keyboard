package com.tyraen.voicekeyboard.feature.learning

import android.content.Context

/** The master "Learn from my corrections" switch. Off until the user turns it on. */
class LearningPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("learning", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = preferences.getBoolean(ENABLED, false)
        set(value) { preferences.edit().putBoolean(ENABLED, value).apply() }

    private companion object {
        const val ENABLED = "enabled"
    }
}
