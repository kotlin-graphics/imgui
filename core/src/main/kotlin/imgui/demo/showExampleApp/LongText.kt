package imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.Cond
import imgui.ImGui.begin
import imgui.ImGui.beginChild
import imgui.ImGui.button
import imgui.ImGui.combo
import imgui.ImGui.end
import imgui.ImGui.endChild
import imgui.ImGui.popStyleVar
import imgui.ImGui.pushStyleVar
import imgui.ImGui.sameLine
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.text
import imgui.ImGui.textEx
import imgui.StyleVar
import imgui.classes.ListClipper
import uno.kotlin.NUL
import kotlin.reflect.KMutableProperty0

object LongText {

    var testType = 0
    val log = StringBuilder()
    var lines = 0

    /** Demonstrate/test rendering huge amount of text, and the incidence of clipping.  */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        setNextWindowSize(Vec2(520, 600), Cond.FirstUseEver)
        if (!begin("Example: Long text display", open)) {
            end()
            return
        }

        text("Printing unusually long amount of text.")
        combo(
                "Test type", ::testType,
                "Single call to TextUnformatted()" + NUL +
                "Multiple calls to Text(), clipped" + NUL +
                "Multiple calls to Text(), not clipped (slow)" + NUL)
        text("Buffer contents: %d lines, %d bytes", lines, log.length)
        if (button("Clear")) log.clear().also { lines = 0 }
        sameLine()
        if (button("Add 1000 lines")) {
            for (i in 0..999)
                log.append("${lines + i} The quick brown fox jumps over the lazy dog\n")
            lines += 1000
        }
        beginChild("Log")

        when (testType) {
            // Single call to TextUnformatted() with a big buffer
            0 -> textEx(log.toString())
            // Multiple calls to Text(), manually coarsely clipped - demonstrate how to use the ImGuiListClipper helper.
            1 -> {
                pushStyleVar(StyleVar.ItemSpacing, Vec2(0))
                val clipper = ListClipper()
                clipper.begin(lines)
                while (clipper.step())
                    for (i in clipper.displayStart until clipper.displayEnd)
                        text("$i The quick brown fox jumps over the lazy dog")
                popStyleVar()
            }
            2 -> {
                pushStyleVar(StyleVar.ItemSpacing, Vec2(0, 1))
                for (i in 0 until lines)
                    text("%d The quick brown fox jumps over the lazy dog".format(i))
                popStyleVar()
            }
        }

        endChild()
        end()
    }
}