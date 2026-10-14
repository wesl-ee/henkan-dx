package ee.wesl.henkandx.core

import java.util.EnumMap
import java.util.EnumSet

enum class Modifier { Shift, Alt, Ctrl }

class Modifiers(private val doubleTapMillis: Long = 500) {
    enum class State { Off, OneShot, Locked }

    private val states = EnumMap<Modifier, State>(Modifier::class.java)
    private val held = EnumSet.noneOf(Modifier::class.java)
    private val cancelled = EnumSet.noneOf(Modifier::class.java)
    private val marks = EnumMap<Modifier, Long>(Modifier::class.java)
    private var last: Modifier? = null

    fun state(m: Modifier): State = states[m] ?: State.Off

    fun shown(m: Modifier): State = if (m in held && m !in cancelled && state(m) == State.Off) State.OneShot else state(m)

    fun down(m: Modifier, now: Long) {
        if (!held.add(m)) return
        val double = last == m && marks[m]?.let { now - it < doubleTapMillis } == true
        last = m
        if (m == Modifier.Shift) marks[m] = now
        states[m] = when {
            state(m) == State.Locked -> State.Off
            double -> State.Locked
            state(m) == State.OneShot -> State.Off
            else -> State.OneShot
        }
        if (states[m] == State.Off) cancelled += m
    }

    fun up(m: Modifier, now: Long) {
        cancelled -= m
        if (held.remove(m) && m != Modifier.Shift) marks[m] = now
    }

    fun hold(m: Modifier, down: Boolean) {
        if (down) held += m else held -= m
    }

    fun use(): Set<Modifier> {
        last = null
        val active = Modifier.entries.filterTo(EnumSet.noneOf(Modifier::class.java)) { it in held || state(it) != State.Off }
        states.replaceAll { _, s -> if (s == State.OneShot) State.Off else s }
        return active
    }

    fun reset() {
        states.clear()
        held.clear()
        cancelled.clear()
        marks.clear()
        last = null
    }
}
