package ee.wesl.henkandx.core

sealed interface Key {
    data class Mod(val modifier: Modifier) : Key

    data object Sym : Key

    data object Lang : Key

    companion object {
        fun of(name: String): Key = when (name) {
            "Sym" -> Sym
            "Lang" -> Lang
            else -> Mod(Modifier.valueOf(name))
        }
    }
}

data class Profile(
    val keyCodes: Map<Int, Key> = emptyMap(),
    val scanCodes: Map<Int, Key> = emptyMap(),
    val nav: Map<Int, Int> = emptyMap(),
    val register: Int? = null,
) {
    fun resolve(keyCode: Int, scanCode: Int): Key? = scanCodes[scanCode] ?: keyCodes[keyCode]

    operator fun plus(other: Profile) = Profile(keyCodes + other.keyCodes, scanCodes + other.scanCodes, nav + other.nav, other.register ?: register)
}
