package ee.wesl.henkandx.core

class Sym(symbols: List<List<String>>) {
    private val keys = ROWS.joinToString("")
    private val pages = symbols.map { rows -> rows.flatMap { row -> row.codePoints().toArray().map(Character::toString) } }
    private val offsets = ROWS.runningFold(0) { n, row -> n + row.length }

    var page: Int? = null
        private set

    val grid: List<List<Pair<Char, String>>>?
        get() = page?.let { page -> ROWS.mapIndexed { i, row -> row.toList().zip(pages[page].drop(offsets[i])) } }

    fun toggle(page: Int) {
        this.page = if (this.page == null) page else null
    }

    fun pick(letter: Char): String? {
        val page = page ?: return null
        return pages[page].getOrNull(keys.indexOf(letter.lowercaseChar()))?.also { close() }
    }

    fun close() {
        page = null
    }

    companion object {
        val ROWS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    }
}
