package com.tyraen.voicekeyboard.feature.learning

import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tyraen.voicekeyboard.R
import com.tyraen.voicekeyboard.app.ServiceLocator
import com.tyraen.voicekeyboard.core.locale.InterfaceLanguageManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Date

/** The learning switch and a plain list of saved corrections, newest first. */
class LearningActivity : AppCompatActivity() {
    private val scope = MainScope()
    private val store get() = ServiceLocator.correctionStore
    private var count = 0

    override fun attachBaseContext(newBase: Context) =
        super.attachBaseContext(InterfaceLanguageManager.applyTo(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_learning)
        val preferences = LearningPreferences(this)
        findViewById<Switch>(R.id.switchLearning).apply {
            isChecked = preferences.enabled
            setOnCheckedChangeListener { _, enabled -> preferences.enabled = enabled }
        }
        findViewById<Button>(R.id.btnLearningDeleteAll).setOnClickListener { confirmDeleteAll() }
    }

    override fun onResume() {
        super.onResume()
        scope.launch { render(store.all()) }
    }

    private fun render(corrections: List<Correction>) {
        count = corrections.size
        findViewById<TextView>(R.id.learningListTitle).text = getString(R.string.learning_list_title, count)
        findViewById<View>(R.id.learningEmpty).visibility = if (count == 0) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btnLearningDeleteAll).isEnabled = count > 0
        val list = findViewById<LinearLayout>(R.id.learningList)
        list.removeAllViews()
        val missing = getString(R.string.learning_missing_word)
        corrections.forEach { c ->
            val pairs = c.segments.joinToString("\n") { s ->
                "${s.heard.ifEmpty { missing }}  →  ${s.meant.ifEmpty { missing }}"
            }
            val details = buildList {
                add("${DateFormat.getDateFormat(this@LearningActivity).format(Date(c.createdAt))} " +
                    "${DateFormat.getTimeFormat(this@LearningActivity).format(Date(c.createdAt))} · ${c.appPackage}")
                // Shows whether the speech model or the cleanup made the mistake.
                if (c.rawTranscript != c.insertedText) add(getString(R.string.learning_speech_heard, c.rawTranscript))
            }.joinToString("\n")
            list.addView(text(pairs, 16f, R.color.key_text, topMargin = 16))
            list.addView(text(details, 13f, R.color.status_text, topMargin = 4))
        }
    }

    private fun text(value: String, sizeSp: Float, color: Int, topMargin: Int) = TextView(this).apply {
        text = value
        textSize = sizeSp
        setTextColor(ContextCompat.getColor(context, color))
        setTextIsSelectable(true)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            this.topMargin = (topMargin * resources.displayMetrics.density).toInt()
        }
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.learning_delete_confirm, count))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.learning_delete) { _, _ ->
                scope.launch {
                    store.deleteAll()
                    render(emptyList())
                    Toast.makeText(this@LearningActivity, R.string.learning_delete_done, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
