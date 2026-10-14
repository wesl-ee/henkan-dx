package ee.wesl.henkandx.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SymTest {
    private val sym = Sym(
        listOf(
            listOf("~`|\\^{}[]=", "&%$€£¥°<>", ";¡¿§¶…•"),
            listOf("「」『』【】〈〉《》", "・〜ー…※♪☆★♡", "→←↑↓○△×"),
        ),
    )

    @Test
    fun `sym toggles one page`() {
        sym.toggle(1)
        assertEquals(1, sym.page)
        sym.toggle(1)
        assertNull(sym.page)
    }

    @Test
    fun `pick maps letters by keyboard position and closes`() {
        sym.toggle(0)
        assertEquals("~", sym.pick('q'))
        assertNull(sym.page)
        sym.toggle(1)
        assertEquals("×", sym.pick('M'))
    }

    @Test
    fun `grid lays the page out in keyboard rows`() {
        sym.toggle(0)
        val grid = sym.grid!!
        assertEquals(listOf(10, 9, 7), grid.map { it.size })
        assertEquals('a' to "&", grid[1][0])
    }
}
