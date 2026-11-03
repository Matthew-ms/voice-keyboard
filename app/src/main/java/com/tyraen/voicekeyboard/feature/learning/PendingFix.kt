package com.tyraen.voicekeyboard.feature.learning

import android.content.Context
import org.json.JSONObject
import java.io.File

/** A dictation the user went to correct with Gboard. */
data class PendingFix(
    val rawTranscript: String,
    val language: String,
    val appPackage: String,
    val snapshot: FieldSnapshot,
    val createdAt: Long
) {
    fun expired(now: Long): Boolean = now < createdAt || now - createdAt > MAX_AGE_MS

    companion object {
        const val MAX_AGE_MS = 10 * 60_000L

        internal fun toJson(fix: PendingFix): String = JSONObject()
            .put("raw_transcript", fix.rawTranscript)
            .put("language", fix.language)
            .put("app_package", fix.appPackage)
            .put("before", fix.snapshot.before)
            .put("inserted", fix.snapshot.inserted)
            .put("after", fix.snapshot.after)
            .put("before_is_start", fix.snapshot.beforeIsStart)
            .put("after_is_end", fix.snapshot.afterIsEnd)
            .put("created_at", fix.createdAt)
            .toString()

        internal fun fromJson(json: String): PendingFix {
            val o = JSONObject(json)
            return PendingFix(
                rawTranscript = o.getString("raw_transcript"),
                language = o.getString("language"),
                appPackage = o.getString("app_package"),
                snapshot = FieldSnapshot(o.getString("before"), o.getString("inserted"), o.getString("after"),
                    o.getBoolean("before_is_start"), o.getBoolean("after_is_end")),
                createdAt = o.getLong("created_at")
            )
        }
    }
}

/**
 * At most one [PendingFix], in a file: Android usually closes this keyboard while Gboard is open,
 * so memory would not survive the trip. Private storage, excluded from backup like the corrections.
 */
class PendingFixStore internal constructor(private val file: File) {
    constructor(context: Context) : this(File(File(context.filesDir, "learning"), "pending_fix.json"))

    // ponytail: plain synchronous write of a few hundred bytes on the main thread; a torn file reads as null.
    fun save(fix: PendingFix) {
        file.parentFile?.mkdirs()
        file.writeText(PendingFix.toJson(fix), Charsets.UTF_8)
    }

    /** The waiting fix, removed from disk so it is only ever handled once. */
    fun take(): PendingFix? {
        if (!file.exists()) return null
        val fix = runCatching { PendingFix.fromJson(file.readText(Charsets.UTF_8)) }.getOrNull()
        file.delete()
        return fix
    }
}
