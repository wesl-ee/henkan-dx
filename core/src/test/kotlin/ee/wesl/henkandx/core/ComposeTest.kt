package ee.wesl.henkandx.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ComposeTest {
    private val compose = Compose(listOf("header without a tab", "'e\té", "'a\tá", "---\t—", "--.\t–", "  \t "))

    @Test
    fun `completes a sequence and closes`() {
        compose.start()
        assertNull(compose.feed("'"))
        assertEquals("é", compose.feed("e"))
        assertNull(compose.pending)
    }

    @Test
    fun `ignores keys until started`() {
        assertNull(compose.feed("'"))
        assertNull(compose.pending)
    }

    @Test
    fun `drops a sequence that cannot complete`() {
        compose.start()
        compose.feed("'")
        assertNull(compose.feed("z"))
        assertNull(compose.pending)
    }

    @Test
    fun `page lists completions with the keys left to type`() {
        compose.start()
        assertNull(compose.page)
        compose.feed("-")
        val page = compose.page!!
        assertEquals(listOf("—", "–"), page.items)
        assertEquals(listOf("--", "-."), page.labels)
        assertEquals("–", compose.pick(1))
        assertNull(compose.pending)
    }

    @Test
    fun `back removes a key then closes`() {
        compose.start()
        compose.feed("-")
        compose.feed("-")
        compose.back()
        assertEquals("-", compose.pending)
        compose.back()
        assertEquals("", compose.pending)
        compose.back()
        assertNull(compose.pending)
    }

    @Test
    fun `sequences may contain spaces`() {
        compose.start()
        compose.feed(" ")
        assertEquals(" ", compose.feed(" "))
    }
}
