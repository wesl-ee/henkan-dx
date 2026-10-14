package ee.wesl.henkandx.core

enum class Special { Space, Enter, Backspace, Escape, Left, Right, Up, Down }

sealed interface Stroke {
    data class Char(val code: Int) : Stroke

    data class Press(val key: Special, val shift: Boolean = false) : Stroke
}

interface Engine {
    fun send(stroke: Stroke): Snapshot?

    fun submit(): Snapshot

    fun pick(index: Int): Snapshot?

    fun reset()

    var incognito: Boolean
}
