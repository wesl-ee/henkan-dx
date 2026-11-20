package ee.wesl.henkandx.core

class Compose(lines: List<String>) {
    private val table = lines.filter { '\t' in it }.associate { it.substringBefore('\t') to it.substringAfter('\t') }
    private var hits = emptyList<Map.Entry<String, String>>()

    var pending: String? = null
        private set

    val page: Page?
        get() = pending?.let { p -> hits.takeIf { it.isNotEmpty() }?.let { Page(it.map { e -> e.value }, null, 0, 1, it.map { e -> e.key.drop(p.length) }) } }

    private fun go(sequence: String?) {
        pending = sequence
        hits = sequence?.takeIf(String::isNotEmpty)?.let { s -> table.entries.filter { it.key.startsWith(s) }.sortedBy { it.key.length } }.orEmpty()
    }

    fun start() = go("")

    fun close() = go(null)

    fun back() = go(pending?.takeIf(String::isNotEmpty)?.dropLast(1))

    // A sequence that cannot complete is dropped, like X does
    fun feed(key: String): String? {
        val sequence = (pending ?: return null) + key
        table[sequence]?.let { return it.also { close() } }
        go(sequence)
        if (hits.isEmpty()) close()
        return null
    }

    fun pick(index: Int): String? = hits.getOrNull(index)?.value?.also { close() }
}
