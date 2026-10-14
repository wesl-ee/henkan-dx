package ee.wesl.henkandx

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.UnderlineSpan
import android.view.KeyEvent
import android.view.inputmethod.ExtractedTextRequest
import ee.wesl.henkandx.core.Snapshot
import ee.wesl.henkandx.core.Span

class Editor(private val ime: InputMethodService) {
    fun commit(text: CharSequence) {
        ime.currentInputConnection?.commitText(text, 1)
    }

    fun send(keyCode: Int, meta: Int) {
        val ic = ime.currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta))
    }

    fun render(snapshot: Snapshot) {
        val ic = ime.currentInputConnection ?: return
        ic.beginBatchEdit()
        snapshot.commit?.let { ic.commitText(it, 1) }
        if (snapshot.preedit.isNotEmpty() || snapshot.commit == null) {
            val text = SpannableStringBuilder()
            for (span in snapshot.preedit) {
                val start = text.length
                text.append(span.text)
                text.setSpan(UnderlineSpan(), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (span.style == Span.Style.Focused) text.setSpan(BackgroundColorSpan(FOCUS), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            ic.setComposingText(text, 1)
            val back = text.length - snapshot.cursor
            if (back > 0) {
                ic.getExtractedText(ExtractedTextRequest(), 0)?.let {
                    val at = it.startOffset + it.selectionEnd - back
                    ic.setSelection(at, at)
                }
            }
        }
        ic.endBatchEdit()
    }

    private companion object {
        const val FOCUS = 0x55808080
    }
}
