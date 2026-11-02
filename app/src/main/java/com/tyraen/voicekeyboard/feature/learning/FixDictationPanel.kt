package com.tyraen.voicekeyboard.feature.learning

import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.tyraen.voicekeyboard.R
import com.tyraen.voicekeyboard.app.ServiceLocator
import com.tyraen.voicekeyboard.core.logging.DiagnosticLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * "Fix last dictation": an edit box over the keyboard. Typing borrows the quick-correction keys,
 * as Selected-text actions do. Save replaces the dictation in the field only while the cursor
 * still sits right after it, then learns from the difference.
 */
class FixDictationPanel(
    private val root: FrameLayout,
    private val inputConnection: () -> InputConnection?,
    private val editorSession: () -> Long?,
    /** Mount [EditText] above the letter keys; call the callback when the user taps Done. */
    private val openKeys: (EditText, () -> Unit) -> Unit,
    /** The dictation was saved or replaced; the host drops it so Fix is not offered twice. */
    private val onSaved: () -> Unit,
    private val onNotice: (String) -> Unit
) {
    companion object {
        private const val TAG = "FixDictation"
        // Saving must outlive the keyboard view, which may be torn down right after Save.
        private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    private val context = root.context
    private var dictation: LastDictation? = null
    private val accessibilityState = HashMap<View, Int>()

    val isVisible: Boolean get() = overlay.visibility == View.VISIBLE

    private val editor = EditText(context).apply {
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        showSoftInputOnFocus = false
        minLines = 2
        maxLines = 5
        textSize = 16f
        setTextColor(ContextCompat.getColor(context, R.color.key_text))
        textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        // Tapping the box brings up the keys, unless they are already up: then the tap only moves the cursor.
        setOnClickListener { if (generateSequence(parent) { it.parent }.any { it === overlay }) editWithKeys() }
    }

    private val overlay = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.keyboard_bg))
        visibility = View.GONE
        elevation = dp(8).toFloat()
        isClickable = true
    }

    init {
        val header = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPaddingRelative(dp(12), 0, 0, 0)
        }
        header.addView(text(context.getString(R.string.fix_open), 18f).apply {
            minHeight = dp(48)
            gravity = Gravity.CENTER_VERTICAL
            ViewCompat.setAccessibilityHeading(this, true)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(ImageButton(context).apply {
            setImageResource(R.drawable.ic_close)
            imageTintList = ContextCompat.getColorStateList(context, R.color.key_text)
            contentDescription = context.getString(R.string.fix_close)
            setBackgroundResource(R.drawable.borderless_ripple)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener { hide() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))

        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(12), dp(12))
            addView(text(context.getString(R.string.fix_hint), 13f).apply {
                setTextColor(ContextCompat.getColor(context, R.color.status_text))
            })
            addView(editor, LinearLayout.LayoutParams(-1, -2))
            addView(button(R.string.fix_edit, primary = false) { editWithKeys() })
            addView(button(R.string.fix_save, primary = true) { save() })
        }
        overlay.addView(header, LinearLayout.LayoutParams(-1, -2))
        overlay.addView(ScrollView(context).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
    }

    fun open(last: LastDictation) {
        dictation = last
        editor.setText(last.insertedText)
        editor.setSelection(editor.text.length)
        show()
        editWithKeys()
    }

    private fun editWithKeys() {
        if (dictation == null) return
        editor.requestFocus()
        val start = editor.selectionStart
        val end = editor.selectionEnd
        openKeys(editor) { show() }
        // Opening the keys puts the cursor at the end; keep it where the user tapped.
        if (start >= 0) editor.setSelection(start, end)
    }

    private fun show() {
        if (dictation == null) return
        if (!isVisible) {
            // Screen readers should not reach the keyboard buttons hidden behind this panel.
            for (index in 0 until root.childCount) {
                val child = root.getChildAt(index)
                if (child === overlay) continue
                accessibilityState[child] = child.importantForAccessibility
                child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
        }
        overlay.visibility = View.VISIBLE
        overlay.bringToFront()
    }

    fun hide() {
        overlay.visibility = View.GONE
        accessibilityState.forEach { (view, state) -> view.importantForAccessibility = state }
        accessibilityState.clear()
        editor.clearFocus()
        dictation = null
    }

    fun destroy() {
        hide()
        root.removeView(overlay)
    }

    private fun save() {
        val last = dictation ?: return
        val final = editor.text.toString()
        // Some apps throw from InputConnection reads; then the text simply stays as it is.
        val replaced = runCatching { replaceInField(last, final) }.getOrDefault(false)
        val result = CorrectionDiff.compare(last.insertedText, final)
        val reason = when (result.ignored) {
            null -> null
            CorrectionDiff.Ignored.UNCHANGED -> R.string.fix_reason_unchanged
            CorrectionDiff.Ignored.DELETED -> R.string.fix_reason_deleted
            CorrectionDiff.Ignored.APPENDED -> R.string.fix_reason_appended
            CorrectionDiff.Ignored.CASE_OR_PUNCTUATION -> R.string.fix_reason_case
            CorrectionDiff.Ignored.REWRITE -> R.string.fix_reason_rewrite
        }
        if (reason == null) {
            val correction = Correction(
                id = UUID.randomUUID().toString(),
                rawTranscript = last.rawTranscript,
                insertedText = last.insertedText,
                finalText = final,
                segments = result.segments,
                language = last.language,
                appPackage = last.appPackage,
                createdAt = System.currentTimeMillis()
            )
            saveScope.launch {
                try {
                    ServiceLocator.correctionStore.add(correction)
                    DiagnosticLog.record(TAG, "Saved correction with ${result.segments.size} segment(s)")
                    onNotice(context.getString(if (replaced) R.string.fix_saved_replaced else R.string.fix_saved_kept))
                } catch (e: Exception) {
                    DiagnosticLog.recordFailure(TAG, "Saving correction failed", e)
                    onNotice(context.getString(R.string.fix_save_failed))
                }
            }
        } else {
            DiagnosticLog.record(TAG, "Not learned: ${result.ignored}")
            val why = context.getString(reason)
            onNotice(if (replaced) context.getString(R.string.fix_replaced_only, why)
                else context.getString(R.string.fix_not_replaced, why))
        }
        hide()
        onSaved()
    }

    /** Replace only while the same field still ends, at the cursor, with what was dictated. */
    private fun replaceInField(last: LastDictation, final: String): Boolean {
        if (final == last.insertedText || editorSession() != last.editorSession) return false
        val connection = inputConnection() ?: return false
        if (!connection.getSelectedText(0).isNullOrEmpty()) return false
        val before = connection.getTextBeforeCursor(last.insertedText.length + 1, 0)
        val count = LastDictation.charsToReplace(before, last.insertedText) ?: return false
        val trailing = if (count > last.insertedText.length) " " else ""
        connection.beginBatchEdit()
        try {
            connection.deleteSurroundingText(count, 0)
            connection.commitText(final + trailing, 1)
        } finally {
            connection.endBatchEdit()
        }
        return true
    }

    private fun button(label: Int, primary: Boolean, action: () -> Unit) = Button(context).apply {
        text = context.getString(label)
        isAllCaps = false
        textSize = 15f
        minHeight = dp(48)
        minimumHeight = dp(48)
        backgroundTintList = null
        setBackgroundResource(if (primary) R.drawable.control_primary_bg else R.drawable.control_key_bg)
        setTextColor(ContextCompat.getColorStateList(context,
            if (primary) R.color.control_primary_text else R.color.control_key_text))
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
    }

    private fun text(value: String, size: Float) = TextView(context).apply {
        text = value
        textSize = size
        setTextColor(ContextCompat.getColor(context, R.color.key_text))
        textDirection = View.TEXT_DIRECTION_FIRST_STRONG
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
