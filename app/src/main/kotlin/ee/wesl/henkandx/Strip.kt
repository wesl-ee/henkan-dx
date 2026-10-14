package ee.wesl.henkandx

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.TypedValue
import android.view.MotionEvent
import android.view.RoundedCorner
import android.view.View
import android.view.WindowInsets
import ee.wesl.henkandx.core.Modifier
import ee.wesl.henkandx.core.Modifiers
import ee.wesl.henkandx.core.Page
import kotlin.math.sqrt

@SuppressLint("ResourceType")
class Strip(context: Context) : View(context) {
    private var lit = emptyMap<Modifier, Modifiers.State>()
    private var label = ""
    private var page: Page? = null
    private var grid: List<List<Pair<Char, String>>>? = null
    private val hits = mutableListOf<ClosedFloatingPointRange<Float>>()
    var onPick: ((Int) -> Unit)? = null
    private val dp = resources.displayMetrics.density
    private var radius = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, resources.displayMetrics)
    }
    private val small = Paint(paint).apply { textSize = paint.textSize * 0.6f }
    private val ink: Int
    private val dim: Int
    private val accent: Int
    private val glyphs = mapOf(Modifier.Shift to "⇧", Modifier.Alt to "⌥", Modifier.Ctrl to "⌃")
        .mapValues { (m, glyph) -> if (paint.hasGlyph(glyph)) glyph else m.name.lowercase() }

    init {
        context.obtainStyledAttributes(
            intArrayOf(
                android.R.attr.colorBackground,
                android.R.attr.textColorPrimary,
                android.R.attr.textColorSecondary,
                android.R.attr.colorAccent,
            ),
        ).run {
            setBackgroundColor(getColor(0, 0))
            ink = getColor(1, 0)
            dim = getColor(2, 0)
            accent = getColor(3, 0)
            recycle()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setOnApplyWindowInsetsListener { _, insets ->
                setPadding(0, 0, 0, insets.getInsets(WindowInsets.Type.navigationBars()).bottom)
                insets
            }
        }
    }

    fun show(modifiers: Modifiers, label: String, page: Page?, grid: List<List<Pair<Char, String>>>?) {
        val lit = Modifier.entries.associateWith(modifiers::shown).filterValues { it != Modifiers.State.Off }
        if (lit != this.lit || label != this.label || page != this.page || grid != this.grid) {
            if (grid?.size != this.grid?.size) requestLayout()
            this.lit = lit
            this.label = label
            this.page = page
            this.grid = grid
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            radius = listOf(RoundedCorner.POSITION_BOTTOM_LEFT, RoundedCorner.POSITION_BOTTOM_RIGHT)
                .maxOf { display?.getRoundedCorner(it)?.radius ?: 0 }.toFloat()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val rows = grid?.size ?: 0
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec), ((BAR + rows * ROW) * dp).toInt() + paddingBottom)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val bar = event.y > height - paddingBottom - BAR * dp
        if (event.action == MotionEvent.ACTION_UP && bar && page != null) {
            hits.indexOfFirst { event.x in it }.takeIf { it >= 0 }?.let { onPick?.invoke(it) }
        }
        return page != null
    }

    override fun onDraw(canvas: Canvas) {
        hits.clear()
        grid?.let { drawGrid(canvas, it) }
        val baseline = height - paddingBottom - (BAR * dp + paint.ascent() + paint.descent()) / 2
        // The distance the screen's rounded corner extends into the bar at its vertical midpoint.
        val rise = radius - paddingBottom - BAR * dp / 2
        val corner = if (rise > 0) radius - sqrt(radius * radius - rise * rise) else 0f
        val edge = corner + 12 * dp
        var x = edge
        paint.color = dim
        canvas.drawText(label, x, baseline, paint)
        x += paint.measureText(label) + 12 * dp
        for ((m, state) in lit) {
            val glyph = glyphs.getValue(m)
            val width = paint.measureText(glyph)
            val locked = state == Modifiers.State.Locked
            paint.color = if (locked) accent else ink
            canvas.drawText(glyph, x, baseline, paint)
            if (locked) canvas.drawRect(x, baseline + 3 * dp, x + width, baseline + 4.5f * dp, paint)
            x += width + 10 * dp
        }
        page?.let { drawPage(canvas, it, x + 8 * dp, baseline, width - edge) }
    }

    private fun drawPage(canvas: Canvas, page: Page, left: Float, baseline: Float, end: Float) {
        val more = if (page.count > 1) "${page.index + 1}/${page.count}" else ""
        val right = end - small.measureText(more)
        small.color = dim
        canvas.drawText(more, right, baseline, small)
        val gap = 16 * dp
        val widths = page.items.mapIndexed { i, item -> small.measureText("${i + 1}") + 3 * dp + paint.measureText(item) }
        val focus = page.focus ?: 0
        // Scroll the row so the focused candidate is always visible
        val end = left + widths.take(focus + 1).sum() + gap * focus
        var x = left - maxOf(0f, end - (right - gap))
        canvas.save()
        canvas.clipRect(left, 0f, right - gap / 2, height.toFloat())
        page.items.forEachIndexed { i, item ->
            hits += x..x + widths[i] + gap
            small.color = dim
            canvas.drawText("${i + 1}", x, baseline, small)
            val start = x + small.measureText("${i + 1}") + 3 * dp
            paint.color = if (i == page.focus) accent else ink
            canvas.drawText(item, start, baseline, paint)
            if (i == page.focus) canvas.drawRect(start, baseline + 3 * dp, x + widths[i], baseline + 4.5f * dp, paint)
            x += widths[i] + gap
        }
        canvas.restore()
    }

    private fun drawGrid(canvas: Canvas, grid: List<List<Pair<Char, String>>>) {
        val cell = width / grid.maxOf { it.size }.toFloat()
        val cap = small.measureText("m") + 4 * dp
        grid.forEachIndexed { r, row ->
            val baseline = (r * ROW + ROW / 2 + 4) * dp - (paint.ascent() + paint.descent()) / 2
            row.forEachIndexed { i, (key, text) ->
                val x = (i + r / 2f) * cell + 4 * dp
                small.color = dim
                canvas.drawText(key.toString(), x, baseline, small)
                val fit = Paint(paint).apply {
                    color = ink
                    textSize = minOf(paint.textSize, paint.textSize * (cell - cap - 6 * dp) / paint.measureText(text))
                }
                canvas.drawText(text, x + cap, baseline, fit)
            }
        }
    }

    private companion object {
        const val BAR = 36
        const val ROW = 32
    }
}
