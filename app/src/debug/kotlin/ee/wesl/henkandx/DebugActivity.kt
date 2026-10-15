package ee.wesl.henkandx

import android.app.Activity
import android.os.Bundle
import android.text.InputType.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import ee.wesl.henkandx.engine.mozc.Mozc
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input
import kotlin.concurrent.thread

class DebugActivity : Activity() {
    private val fields = listOf(
        "text" to TYPE_CLASS_TEXT,
        "multi-line" to (TYPE_CLASS_TEXT or TYPE_TEXT_FLAG_MULTI_LINE),
        "email" to (TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_EMAIL_ADDRESS),
        "uri" to (TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_URI),
        "no suggestions" to (TYPE_CLASS_TEXT or TYPE_TEXT_FLAG_NO_SUGGESTIONS),
        "password" to (TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD),
        "visible password" to (TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_VISIBLE_PASSWORD),
        "web password" to (TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_WEB_PASSWORD),
        "number" to TYPE_CLASS_NUMBER,
        "number password" to (TYPE_CLASS_NUMBER or TYPE_NUMBER_VARIATION_PASSWORD),
        "phone" to TYPE_CLASS_PHONE,
    )

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val status = TextView(this).apply { text = "mozc loading" }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(status)
            fields.map { (label, type) ->
                EditText(context).apply {
                    hint = label
                    inputType = type
                }
            }.forEach(::addView)
        }
        setContentView(
            ScrollView(this).apply {
                fitsSystemWindows = true
                addView(column)
            },
        )

        thread {
            val text = runCatching {
                val mozc = checkNotNull(Mozc.get(this)) { "no ja pack" }
                if (intent.hasExtra("incognito")) mozc.incognito = intent.getBooleanExtra("incognito", false)
                "mozc ${mozc.dataVersion} · session ${mozc.eval(Input.newBuilder().setType(Input.CommandType.CREATE_SESSION)).id.toULong()}"
            }.getOrElse { "mozc failed: $it" }
            runOnUiThread { status.text = text }
        }
    }
}
