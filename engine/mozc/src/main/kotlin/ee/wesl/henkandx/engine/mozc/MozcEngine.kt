package ee.wesl.henkandx.engine.mozc

import ee.wesl.henkandx.core.Engine
import ee.wesl.henkandx.core.Page
import ee.wesl.henkandx.core.Snapshot
import ee.wesl.henkandx.core.Span
import ee.wesl.henkandx.core.Special
import ee.wesl.henkandx.core.Stroke
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCandidateWindow.Category
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.KeyEvent
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Output
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Preedit.Segment.Annotation
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Request
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.SessionCommand

class MozcEngine(private val mozc: Mozc) : Engine {
    private var ids = emptyList<Int>()
    private var predicting = false
    private val id = mozc.eval(Input.newBuilder().setType(Input.CommandType.CREATE_SESSION)).id

    init {
        // Mix conversion candidates with suggestions so they all appear in one list
        eval(Input.newBuilder().setType(Input.CommandType.SET_REQUEST).setRequest(Request.newBuilder().setMixedConversion(true)))
    }

    override fun send(stroke: Stroke): Snapshot? {
        // A picked suggestion has no clauses so go back to the reading and convert it.
        if (predicting && stroke is Stroke.Press && stroke.key in listOf(Special.Left, Special.Right)) {
            press(KeyEvent.newBuilder().setSpecialKey(KeyEvent.SpecialKey.ESCAPE))
            press(KeyEvent.newBuilder().setSpecialKey(KeyEvent.SpecialKey.SPACE))
        }
        val key = KeyEvent.newBuilder()
        when (stroke) {
            is Stroke.Char -> key.keyCode = stroke.code

            is Stroke.Press -> {
                key.specialKey = specials.getValue(stroke.key)
                if (stroke.shift) key.addModifierKeys(KeyEvent.ModifierKey.SHIFT)
            }
        }
        val output = press(key)
        return if (output.consumed) snapshot(output) else null
    }

    private fun press(key: KeyEvent.Builder) = eval(Input.newBuilder().setType(Input.CommandType.SEND_KEY).setKey(key))

    override fun submit() = snapshot(command(SessionCommand.CommandType.SUBMIT))

    override fun pick(index: Int): Snapshot? {
        val id = ids.getOrNull(index) ?: return null
        return snapshot(eval(Input.newBuilder().setType(Input.CommandType.SEND_COMMAND).setCommand(SessionCommand.newBuilder().setType(SessionCommand.CommandType.SELECT_CANDIDATE).setId(id))))
    }

    override var incognito by mozc::incognito

    override fun reset() {
        command(SessionCommand.CommandType.REVERT)
    }

    private fun command(type: SessionCommand.CommandType) = eval(Input.newBuilder().setType(Input.CommandType.SEND_COMMAND).setCommand(SessionCommand.newBuilder().setType(type)))

    private fun eval(input: Input.Builder): Output = mozc.eval(input.setId(id))

    private fun snapshot(output: Output): Snapshot {
        val segments = output.preedit.segmentList
        val converted = segments.any { it.annotation == Annotation.HIGHLIGHT }
        val window = output.candidateWindow.takeIf { output.hasCandidateWindow() && it.candidateCount > 0 }
        predicting = window?.category == Category.PREDICTION
        // Mozc will only show its candidate list on the second Space, but you can show the candidates on the first.
        val all = output.allCandidateWords.takeIf { window == null && converted && it.candidatesCount > 0 }
        val start = all?.let { it.focusedIndex / PAGE * PAGE } ?: 0
        val shown = all?.candidatesList?.drop(start)?.take(PAGE)
        ids = shown?.map { it.id } ?: output.candidateWindow.candidateList.map { it.id }
        return Snapshot(
            preedit = segments.map {
                Span(
                    it.value,
                    when {
                        it.annotation == Annotation.HIGHLIGHT -> Span.Style.Focused
                        converted -> Span.Style.Converted
                        else -> Span.Style.Raw
                    },
                    it.key.ifEmpty { it.value },
                )
            },
            cursor = output.preedit.cursor,
            candidates = if (all != null) {
                Page(
                    items = shown.orEmpty().map { it.value },
                    focus = all.focusedIndex - start,
                    index = start / PAGE,
                    count = (all.candidatesCount + PAGE - 1) / PAGE,
                )
            } else {
                window?.let { w ->
                    val first = w.getCandidate(0).index
                    Page(
                        items = w.candidateList.map { it.value },
                        focus = if (w.hasFocusedIndex()) w.focusedIndex - first else null,
                        index = first / w.pageSize,
                        count = (w.size + w.pageSize - 1) / w.pageSize,
                    )
                }
            },
            commit = if (output.hasResult()) output.result.value else null,
        )
    }

    private companion object {
        const val PAGE = 9
        val specials = mapOf(
            Special.Space to KeyEvent.SpecialKey.SPACE,
            Special.Enter to KeyEvent.SpecialKey.ENTER,
            Special.Backspace to KeyEvent.SpecialKey.BACKSPACE,
            Special.Escape to KeyEvent.SpecialKey.ESCAPE,
            Special.Left to KeyEvent.SpecialKey.LEFT,
            Special.Right to KeyEvent.SpecialKey.RIGHT,
            Special.Up to KeyEvent.SpecialKey.UP,
            Special.Down to KeyEvent.SpecialKey.DOWN,
        )
    }
}
