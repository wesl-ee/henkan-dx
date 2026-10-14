package ee.wesl.henkandx.core

data class Snapshot(
    val preedit: List<Span> = emptyList(),
    val cursor: Int = 0,
    val candidates: Page? = null,
    val commit: String? = null,
)

data class Span(val text: String, val style: Style, val reading: String = text) {
    enum class Style { Raw, Converted, Focused }
}

data class Page(
    val items: List<String>,
    val focus: Int?,
    val index: Int,
    val count: Int,
)
