package ee.wesl.henkandx

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import ee.wesl.henkandx.engine.mozc.Mozc
import ee.wesl.henkandx.engine.mozc.MozcDictionary
import kotlin.concurrent.thread

class SettingsActivity : Activity() {
    private lateinit var mozc: Mozc
    private lateinit var dictionary: MozcDictionary
    private lateinit var words: LinearLayout
    private lateinit var reading: EditText
    private lateinit var word: EditText
    private var missing = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            visibility = View.INVISIBLE
        }
        setContentView(
            ScrollView(this).apply {
                fitsSystemWindows = true
                addView(column)
            },
        )
        thread {
            val mozc = Mozc.get(this)
            val learning = mozc?.incognito == false
            runOnUiThread { build(column, mozc, learning) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (missing) thread { if (Mozc.get(this) != null) runOnUiThread(::recreate) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::word.isInitialized) prefill()
    }

    @Suppress("DEPRECATION")
    private fun build(column: LinearLayout, mozc: Mozc?, learning: Boolean) {
        column.addView(button(R.string.keyboard_settings) { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) })
        column.addView(button(R.string.manage_packs) { startActivity(Intent(this, PacksActivity::class.java)) })
        column.visibility = View.VISIBLE
        if (mozc == null) {
            missing = true
            column.addView(TextView(this).apply { setText(R.string.pack_missing) })
            return
        }
        this.mozc = mozc
        dictionary = MozcDictionary(mozc)
        column.addView(
            Switch(this).apply {
                setText(R.string.learn)
                isChecked = learning
                setOnCheckedChangeListener { _, on -> thread { mozc.incognito = !on } }
            },
        )
        column.addView(TextView(this).apply { setText(R.string.learn_note) })
        column.addView(
            button(R.string.forget) {
                confirm(getString(R.string.forget_confirm)) {
                    thread { mozc.forget() }
                }
            },
        )
        column.addView(TextView(this).apply { setText(R.string.dictionary) })
        reading = EditText(this).apply { setHint(R.string.reading) }
        word = EditText(this).apply { setHint(R.string.word) }
        column.addView(reading)
        column.addView(word)
        column.addView(
            button(R.string.add) {
                val entry = MozcDictionary.Word(reading.text.toString().trim(), word.text.toString().trim())
                if (entry.reading.isNotEmpty() && entry.word.isNotEmpty()) {
                    edit { dictionary.words += entry }
                    reading.text.clear()
                    word.text.clear()
                    if (intent.hasExtra(EXTRA_READING) || intent.hasExtra(EXTRA_WORD)) finish()
                }
            },
        )
        column.addView(
            LinearLayout(this).apply {
                addView(button(R.string.import_words) { open(IMPORT, Intent.ACTION_OPEN_DOCUMENT) })
                addView(button(R.string.export_words) { open(EXPORT, Intent.ACTION_CREATE_DOCUMENT) })
            },
        )
        words = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        column.addView(words)
        list()
        prefill()
    }

    private fun prefill() {
        intent.getStringExtra(EXTRA_READING)?.let(reading::setText)
        intent.getStringExtra(EXTRA_WORD)?.let(word::setText)
        (if (reading.text.isEmpty()) reading else word).requestFocus()
    }

    private fun list() {
        words.removeAllViews()
        dictionary.words.forEach { entry ->
            words.addView(
                TextView(this).apply {
                    text = "${entry.reading}　${entry.word}"
                    setPadding(0, 16, 0, 16)
                    setOnClickListener { confirm(getString(R.string.remove_confirm, entry.word)) { edit { dictionary.words -= entry } } }
                },
            )
        }
    }

    private fun edit(change: () -> Unit) = thread {
        change()
        runOnUiThread(::list)
    }

    private fun open(request: Int, action: String) = startActivityForResult(
        Intent(action).addCategory(Intent.CATEGORY_OPENABLE).setType("text/tab-separated-values")
            .putExtra(Intent.EXTRA_TITLE, "henkandx-dictionary.tsv"),
        request,
    )

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri = data?.data?.takeIf { resultCode == RESULT_OK } ?: return
        edit {
            when (requestCode) {
                IMPORT -> contentResolver.openInputStream(uri)?.use { dictionary.import(it.reader().readText()) }
                EXPORT -> contentResolver.openOutputStream(uri, "wt")?.use { it.write(dictionary.export().toByteArray()) }
            }
        }
    }

    private fun button(label: Int, click: () -> Unit) = Button(this).apply {
        setText(label)
        setOnClickListener { click() }
    }

    private fun confirm(message: String, yes: () -> Unit) {
        AlertDialog.Builder(this).setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> yes() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val EXTRA_READING = "reading"
        const val EXTRA_WORD = "word"
        private const val IMPORT = 1
        private const val EXPORT = 2
    }
}
