package ee.wesl.henkandx.core

import ee.wesl.henkandx.core.Modifier.Alt
import ee.wesl.henkandx.core.Modifier.Ctrl
import ee.wesl.henkandx.core.Modifier.Shift
import ee.wesl.henkandx.core.Modifiers.State.Locked
import ee.wesl.henkandx.core.Modifiers.State.Off
import ee.wesl.henkandx.core.Modifiers.State.OneShot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ModifiersTest {
    private val mods = Modifiers()

    private fun Modifiers.tap(m: Modifier, at: Long) {
        down(m, at)
        up(m, at + 50)
    }

    @ParameterizedTest
    @EnumSource(Modifier::class)
    fun `tap is one-shot and spent by the next key`(m: Modifier) {
        mods.tap(m, 0)
        assertEquals(OneShot, mods.state(m))
        assertEquals(setOf(m), mods.use())
        assertEquals(Off, mods.state(m))
        assertEquals(emptySet<Modifier>(), mods.use())
    }

    @ParameterizedTest
    @EnumSource(Modifier::class)
    fun `double tap locks until tapped again`(m: Modifier) {
        mods.tap(m, 0)
        mods.tap(m, 200)
        assertEquals(Locked, mods.state(m))
        repeat(3) { assertEquals(setOf(m), mods.use()) }
        mods.tap(m, 2000)
        assertEquals(Off, mods.state(m))
    }

    @ParameterizedTest
    @EnumSource(Modifier::class)
    fun `slow second tap cancels the one-shot`(m: Modifier) {
        mods.tap(m, 0)
        mods.tap(m, 900)
        assertEquals(Off, mods.state(m))
    }

    @ParameterizedTest
    @EnumSource(Modifier::class)
    fun `a key between taps is not a double tap`(m: Modifier) {
        mods.tap(m, 0)
        mods.use()
        mods.tap(m, 200)
        assertEquals(OneShot, mods.state(m))
    }

    @ParameterizedTest
    @EnumSource(Modifier::class)
    fun `four quick taps lock again`(m: Modifier) {
        listOf(0L, 150, 300, 450).forEach { mods.tap(m, it) }
        assertEquals(Locked, mods.state(m))
    }

    @Test
    fun `another modifier between taps is not a double tap`() {
        mods.tap(Shift, 0)
        mods.tap(Alt, 100)
        mods.tap(Shift, 200)
        assertEquals(Off, mods.state(Shift))
        assertEquals(OneShot, mods.state(Alt))
    }

    @Test
    fun `held modifier applies to every key until released`() {
        mods.down(Shift, 0)
        assertEquals(setOf(Shift), mods.use())
        assertEquals(setOf(Shift), mods.use())
        mods.up(Shift, 300)
        assertEquals(emptySet<Modifier>(), mods.use())
    }

    @ParameterizedTest
    @EnumSource(Modifier::class)
    fun `held modifier stays shown after its one-shot is spent`(m: Modifier) {
        mods.down(m, 0)
        mods.use()
        assertEquals(Off, mods.state(m))
        assertEquals(OneShot, mods.shown(m))
        mods.up(m, 300)
        assertEquals(Off, mods.shown(m))
    }

    @ParameterizedTest
    @EnumSource(Modifier::class)
    fun `pressing a locked modifier hides it straight away`(m: Modifier) {
        mods.tap(m, 0)
        mods.tap(m, 100)
        mods.down(m, 2000)
        assertEquals(Off, mods.shown(m))
        assertEquals(setOf(m), mods.use())
        mods.up(m, 2050)
        assertEquals(Off, mods.shown(m))
    }

    @ParameterizedTest
    @EnumSource(Modifier::class)
    fun `pressing a one-shot modifier again hides it straight away`(m: Modifier) {
        mods.tap(m, 0)
        mods.down(m, 900)
        assertEquals(Off, mods.shown(m))
    }

    @Test
    fun `held modifiers combine`() {
        mods.down(Ctrl, 0)
        mods.down(Shift, 10)
        assertEquals(setOf(Ctrl, Shift), mods.use())
    }

    @Test
    fun `key repeat on a held modifier is ignored`() {
        mods.down(Shift, 0)
        mods.down(Shift, 100)
        mods.down(Shift, 200)
        mods.up(Shift, 250)
        assertEquals(OneShot, mods.state(Shift))
    }

    @Test
    fun `shift measures the double tap from the previous press`() {
        mods.down(Shift, 0)
        mods.up(Shift, 400)
        mods.down(Shift, 600)
        assertEquals(Off, mods.state(Shift))
    }

    @ParameterizedTest
    @EnumSource(value = Modifier::class, names = ["Ctrl", "Alt"])
    fun `ctrl and alt measure the double tap from the previous release`(m: Modifier) {
        mods.down(m, 0)
        mods.up(m, 400)
        mods.down(m, 600)
        assertEquals(Locked, mods.state(m))
    }

    @Test
    fun `hold applies without a one-shot or lock`() {
        mods.hold(Shift, true)
        assertEquals(setOf(Shift), mods.use())
        mods.hold(Shift, false)
        assertEquals(Off, mods.state(Shift))
        assertEquals(emptySet<Modifier>(), mods.use())
    }

    @Test
    fun `reset clears everything`() {
        mods.tap(Shift, 0)
        mods.tap(Shift, 100)
        mods.down(Alt, 200)
        mods.reset()
        Modifier.entries.forEach { assertEquals(Off, mods.state(it)) }
        assertEquals(emptySet<Modifier>(), mods.use())
    }
}
