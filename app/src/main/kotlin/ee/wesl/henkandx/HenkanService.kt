package ee.wesl.henkandx

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import ee.wesl.henkandx.core.Engine
import ee.wesl.henkandx.core.Key
import ee.wesl.henkandx.core.Modifier
import ee.wesl.henkandx.core.Modifiers
import ee.wesl.henkandx.core.Page
import ee.wesl.henkandx.core.Profile
import ee.wesl.henkandx.core.Snapshot
import ee.wesl.henkandx.core.Span
import ee.wesl.henkandx.core.Special
import ee.wesl.henkandx.core.Stroke
import ee.wesl.henkandx.core.Sym
import ee.wesl.henkandx.core.composable
import ee.wesl.henkandx.core.sensitive
import ee.wesl.henkandx.engine.mozc.Mozc
import ee.wesl.henkandx.engine.mozc.MozcEngine
import kotlin.concurrent.thread

class HenkanService : InputMethodService() {
    private val modifiers = Modifiers()
    private val editor = Editor(this)
    private val consumed = mutableSetOf<Int>()
    private val pressed = mutableMapOf<Modifier, Pair<Int, Int>>()
    private lateinit var profile: Profile
    private var strip: Strip? = null
    private var engine: Engine? = null
    private var loading = false
    private var japanese = true
    private var field = Field.Latin
    private var composing = false
    private var preedit = emptyList<Span>()
    private var page: Page? = null
    private var langTap = false
    private lateinit var sym: Sym

    private val kana get() = field == Field.Kana && japanese && engine != null

    private enum class Field(val label: String) { Kana("あ"), Latin("A"), Locked("⚿") }

    override fun onCreate() {
        super.onCreate()
        profile = loadProfile(this)
        sym = Sym(assets.open("sym.txt").bufferedReader().use { it.readText() }.trim().split("\n\n").map { it.lines() })
        load()
    }

    private fun load() {
        if (engine != null || loading) return
        loading = true
        thread(name = "mozc") {
            val mozc = runCatching { Mozc.get(this)?.let(::MozcEngine) }.getOrNull()
            Handler(Looper.getMainLooper()).post {
                engine = mozc
                loading = false
                refresh()
            }
        }
    }

    override fun onCreateInputView() = Strip(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_DayNight) else this,
    ).also {
        strip = it
        it.onPick = { index ->
            engine?.pick(index)?.let(::render)
            refresh()
        }
        refresh()
    }

    // Android hides the input view when a hardware keyboard is attached; our bar should always show
    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    override fun onEvaluateFullscreenMode() = false

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (!restarting) {
            modifiers.reset()
            pressed.clear()
        }
        val type = attribute?.inputType ?: InputType.TYPE_NULL
        val options = attribute?.imeOptions ?: 0
        field = when {
            sensitive(type, options) -> Field.Locked
            composable(type, options) -> Field.Kana
            else -> Field.Latin
        }
        if (type != InputType.TYPE_NULL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) requestShowSelf(0)
        load()
        refresh()
    }

    override fun onFinishInput() {
        if (composing) {
            currentInputConnection?.finishComposingText()
            drop()
        }
        super.onFinishInput()
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (composing && candidatesStart == -1) drop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val key = profile.resolve(keyCode, event.scanCode)
        if (key != Key.Lang) langTap = false
        val handled = when (key) {
            is Key.Mod -> true.also {
                modifiers.down(key.modifier, event.eventTime)
                metas[keyCode]?.let { pressed[key.modifier] = event.deviceId to it }
            }

            Key.Sym -> true.also { if (event.repeatCount == 0) sym.toggle(if (kana) 1 else 0) }

            Key.Lang -> true.also {
                if (event.repeatCount == 0) langTap = true
                modifiers.hold(Modifier.Shift, true)
            }

            null -> {
                pressed.filterValues { (device, meta) -> device == event.deviceId && event.metaState and meta == 0 }.keys.forEach {
                    modifiers.up(it, event.eventTime)
                    pressed -= it
                }
                type(keyCode, event) || super.onKeyDown(keyCode, event)
            }
        }
        refresh()
        return handled
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val key = profile.resolve(keyCode, event.scanCode)
        when (key) {
            is Key.Mod -> if (!event.isCanceled) {
                modifiers.up(key.modifier, event.eventTime)
                pressed -= key.modifier
            }

            Key.Lang -> {
                modifiers.hold(Modifier.Shift, false)
                if (langTap && !event.isCanceled) toggle()
            }

            else -> {}
        }
        refresh()
        return key != null || consumed.remove(keyCode) || super.onKeyUp(keyCode, event)
    }

    private fun type(keyCode: Int, event: KeyEvent): Boolean {
        if (currentInputConnection == null) return false
        val caps = modifiers.state(Modifier.Shift) == Modifiers.State.Locked
        val active = modifiers.use()
        val shift = Modifier.Shift in active
        val nav = profile.nav[keyCode]?.takeIf { Modifier.Ctrl in active }
        val code = nav ?: keyCode
        val ctrl = Modifier.Ctrl in active && nav == null
        if (ctrl && code == KeyEvent.KEYCODE_SPACE) {
            toggle()
            return consume(keyCode)
        }
        if (ctrl && code == profile.register && field != Field.Locked && engine != null) {
            register()
            return consume(keyCode)
        }

        var meta = 0
        if (shift) meta = meta or if (caps) KeyEvent.META_CAPS_LOCK_ON else KeyEvent.META_SHIFT_ON
        if (Modifier.Alt in active) meta = meta or KeyEvent.META_ALT_ON
        val char = event.keyCharacterMap.get(code, meta).takeUnless {
            ctrl || nav != null || it == 0 || it and KeyCharacterMap.COMBINING_ACCENT != 0 || Character.isISOControl(it) ||
                Character.getType(it) == Character.PRIVATE_USE.toInt()
        }

        if (sym.page != null) {
            val handled = when (code) {
                KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_DEL -> true.also { sym.close() }

                else -> sym.pick(event.keyCharacterMap.get(keyCode, 0).toChar())?.also {
                    flush()
                    editor.commit(it)
                } != null
            }
            if (handled) return consume(keyCode)
            sym.close()
        }

        val resize = specials[profile.nav[keyCode]]?.takeIf {
            it in listOf(Special.Left, Special.Right) && shift && nav == null && !ctrl && preedit.any { span -> span.style != Span.Style.Raw }
        }
        val digit = char?.let { Character.getNumericValue(it) }?.takeIf { it in 1..9 && page != null && preedit.any { span -> span.style != Span.Style.Raw } }
        if (digit != null && engine != null && !ctrl) {
            engine?.pick(digit - 1)?.let(::render)
            return consume(keyCode)
        }
        // Space picks from the suggestions on the bar. Shift+Space converts clause by clause instead.
        val suggesting = code == KeyEvent.KEYCODE_SPACE && page != null && preedit.all { it.style == Span.Style.Raw }
        val stroke = when {
            resize != null -> Stroke.Press(resize, shift = true)
            suggesting && !shift -> Stroke.Press(Special.Down)
            suggesting -> Stroke.Press(Special.Space)
            else -> specials[code]?.let { Stroke.Press(it, shift) } ?: char?.let(Stroke::Char)
        }
        val snapshot = stroke?.let { engine?.takeIf { kana && !ctrl }?.send(it) }
        when {
            snapshot != null -> render(snapshot)
            ctrl -> editor.send(code, meta or KeyEvent.META_CTRL_ON)
            nav != null -> editor.send(code, if (shift) KeyEvent.META_SHIFT_ON else 0)
            char != null -> editor.commit(Character.toChars(char).concatToString())
            else -> return false
        }
        return consume(keyCode)
    }

    private fun consume(keyCode: Int): Boolean {
        consumed += keyCode
        return true
    }

    private fun toggle() {
        if (engine == null) return
        flush()
        japanese = !japanese
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val imm = getSystemService(InputMethodManager::class.java)
        val ime = imm.enabledInputMethodList.find { it.packageName == packageName } ?: return
        imm.getEnabledInputMethodSubtypeList(ime, true).find { it.japanese == japanese }?.let { switchInputMethod(ime.id, it) }
    }

    override fun onCurrentInputMethodSubtypeChanged(subtype: InputMethodSubtype) {
        super.onCurrentInputMethodSubtypeChanged(subtype)
        if (subtype.japanese != japanese) {
            flush()
            japanese = subtype.japanese
            refresh()
        }
    }

    private val InputMethodSubtype.japanese get() = languageTag.startsWith("ja")

    private fun register() {
        val intent = Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (composing) {
            intent.putExtra(SettingsActivity.EXTRA_READING, preedit.joinToString("") { it.reading })
            if (preedit.any { it.style != Span.Style.Raw }) intent.putExtra(SettingsActivity.EXTRA_WORD, preedit.joinToString("") { it.text })
            currentInputConnection?.finishComposingText()
            drop()
        } else {
            intent.putExtra(SettingsActivity.EXTRA_WORD, currentInputConnection?.getSelectedText(0)?.toString())
        }
        startActivity(intent)
    }

    private fun flush() {
        engine?.takeIf { composing }?.let { render(it.submit()) }
    }

    private fun render(snapshot: Snapshot) {
        editor.render(snapshot)
        composing = snapshot.preedit.isNotEmpty()
        preedit = snapshot.preedit
        page = snapshot.candidates
    }

    private fun drop() {
        engine?.reset()
        composing = false
        page = null
        refresh()
    }

    private fun refresh() {
        strip?.show(modifiers, if (field == Field.Kana && !kana) Field.Latin.label else field.label, page, sym.grid)
    }

    private companion object {
        val metas = mapOf(
            KeyEvent.KEYCODE_SHIFT_LEFT to KeyEvent.META_SHIFT_ON,
            KeyEvent.KEYCODE_SHIFT_RIGHT to KeyEvent.META_SHIFT_ON,
            KeyEvent.KEYCODE_ALT_LEFT to KeyEvent.META_ALT_ON,
            KeyEvent.KEYCODE_ALT_RIGHT to KeyEvent.META_ALT_ON,
            KeyEvent.KEYCODE_CTRL_LEFT to KeyEvent.META_CTRL_ON,
            KeyEvent.KEYCODE_CTRL_RIGHT to KeyEvent.META_CTRL_ON,
        )
        val specials = mapOf(
            KeyEvent.KEYCODE_SPACE to Special.Space,
            KeyEvent.KEYCODE_ENTER to Special.Enter,
            KeyEvent.KEYCODE_DEL to Special.Backspace,
            KeyEvent.KEYCODE_ESCAPE to Special.Escape,
            KeyEvent.KEYCODE_DPAD_LEFT to Special.Left,
            KeyEvent.KEYCODE_DPAD_RIGHT to Special.Right,
            KeyEvent.KEYCODE_DPAD_UP to Special.Up,
            KeyEvent.KEYCODE_DPAD_DOWN to Special.Down,
        )
    }
}
