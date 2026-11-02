package com.tyraen.voicekeyboard.feature.learning

import android.content.Context
import com.tyraen.voicekeyboard.core.logging.DiagnosticLog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** A dictation the user fixed: what each stage produced and what they kept. */
data class Correction(
    val id: String,
    /** Speech model output, before local replacements and the LLM. */
    val rawTranscript: String,
    /** What the keyboard typed into the field. */
    val insertedText: String,
    val finalText: String,
    val segments: List<CorrectionDiff.Segment>,
    val language: String,
    val appPackage: String,
    val createdAt: Long
)

/**
 * All corrections in one JSON file in private app storage, which upstream already excludes from
 * cloud backup and device transfer. Operations are serialized by [mutex] on [dispatcher].
 */
class CorrectionStore internal constructor(
    private val file: File,
    private val dispatcher: CoroutineDispatcher
) {
    constructor(context: Context) : this(File(File(context.filesDir, "learning"), "corrections.json"), Dispatchers.IO)

    private val mutex = Mutex()
    private var items: MutableList<Correction>? = null

    /** Newest first. */
    suspend fun all(): List<Correction> = withContext(dispatcher) {
        mutex.withLock { loaded().reversed() }
    }

    suspend fun add(correction: Correction) = withContext(dispatcher) {
        mutex.withLock {
            val next = loaded() + correction
            write(next)
            items = next.toMutableList()
        }
    }

    suspend fun deleteAll() = withContext(dispatcher) {
        mutex.withLock {
            if (file.exists() && !file.delete()) throw IOException("Could not delete learning data")
            items = mutableListOf()
        }
    }

    private fun loaded(): List<Correction> {
        items?.let { return it }
        val list = if (!file.exists()) mutableListOf() else try {
            parse(file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            // Never overwrite data we could not read: set it aside and start a fresh file.
            val aside = File(file.parentFile, "corrections.unreadable-${System.currentTimeMillis()}.json")
            file.renameTo(aside)
            DiagnosticLog.recordFailure(TAG, "Corrections file unreadable, moved to ${aside.name}", e)
            mutableListOf()
        }
        items = list
        return list
    }

    private fun write(list: List<Correction>) {
        file.parentFile?.let { if (!it.isDirectory && !it.mkdirs()) throw IOException("Could not create ${it.name}") }
        val temporary = File.createTempFile("corrections-", ".tmp", file.parentFile)
        try {
            FileOutputStream(temporary).use { output ->
                output.write(serialize(list).toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            // Android replaces the target atomically; the JVM on Windows (unit tests) refuses to
            // rename over an existing file, so only there fall back to delete-then-rename.
            if (!temporary.renameTo(file) && !(file.delete() && temporary.renameTo(file))) {
                throw IOException("Could not save corrections")
            }
        } finally {
            // If the fallback deleted the old file but could not rename, the temp file is the only copy.
            if (file.exists()) temporary.delete()
        }
    }

    companion object {
        private const val TAG = "CorrectionStore"
        private const val VERSION = 1

        internal fun serialize(list: List<Correction>): String = JSONObject()
            .put("version", VERSION)
            .put("corrections", JSONArray(list.map { c ->
                JSONObject()
                    .put("id", c.id)
                    .put("raw_transcript", c.rawTranscript)
                    .put("inserted_text", c.insertedText)
                    .put("final_text", c.finalText)
                    .put("segments", JSONArray(c.segments.map { s ->
                        JSONObject().put("heard", s.heard).put("meant", s.meant)
                            .put("before", s.before).put("after", s.after)
                    }))
                    .put("language", c.language)
                    .put("app_package", c.appPackage)
                    .put("created_at", c.createdAt)
            }))
            .toString(2)

        internal fun parse(json: String): MutableList<Correction> {
            val array = JSONObject(json).getJSONArray("corrections")
            return MutableList(array.length()) { i ->
                val c = array.getJSONObject(i)
                val segments = c.getJSONArray("segments")
                Correction(
                    id = c.getString("id"),
                    rawTranscript = c.getString("raw_transcript"),
                    insertedText = c.getString("inserted_text"),
                    finalText = c.getString("final_text"),
                    segments = List(segments.length()) { k ->
                        val s = segments.getJSONObject(k)
                        CorrectionDiff.Segment(s.getString("heard"), s.getString("meant"),
                            s.getString("before"), s.getString("after"))
                    },
                    language = c.getString("language"),
                    appPackage = c.getString("app_package"),
                    createdAt = c.getLong("created_at")
                )
            }
        }
    }
}
