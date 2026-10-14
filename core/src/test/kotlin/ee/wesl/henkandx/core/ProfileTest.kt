package ee.wesl.henkandx.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ProfileTest {
    private val generic = Profile(keyCodes = mapOf(59 to Key.of("Shift"), 57 to Key.of("Alt")))
    private val titan = Profile(
        keyCodes = mapOf(60 to Key.of("Lang")),
        scanCodes = mapOf(251 to Key.of("Ctrl"), 253 to Key.of("Sym")),
        nav = mapOf(33 to 19),
    )
    private val profile = generic + titan

    @Test
    fun `scan codes win over key codes`() {
        assertEquals(Key.Mod(Modifier.Ctrl), profile.resolve(keyCode = 59, scanCode = 251))
    }

    @Test
    fun `key codes resolve on any device`() {
        assertEquals(Key.Mod(Modifier.Shift), profile.resolve(keyCode = 59, scanCode = 42))
        assertEquals(Key.Sym, profile.resolve(keyCode = 0, scanCode = 253))
    }

    @Test
    fun `everything else is not a key the profile owns`() {
        assertNull(profile.resolve(keyCode = 29, scanCode = 30))
    }

    @Test
    fun `device profile overrides generic keys and adds nav`() {
        val merged = Profile(keyCodes = mapOf(60 to Key.of("Shift"))) + titan
        assertEquals(Key.Lang, merged.resolve(keyCode = 60, scanCode = 54))
        assertEquals(mapOf(33 to 19), merged.nav)
    }
}
