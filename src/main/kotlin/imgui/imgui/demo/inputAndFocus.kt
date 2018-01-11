package imgui.imgui.demo

import glm_.i
import imgui.*
import imgui.ImGui.bullet
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.calcItemRectClosestPoint
import imgui.ImGui.captureKeyboardFromApp
import imgui.ImGui.checkbox
import imgui.ImGui.getMouseDragDelta
import imgui.ImGui.inputText
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemHovered
import imgui.ImGui.isKeyPressed
import imgui.ImGui.isKeyReleased
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isMouseDoubleClicked
import imgui.ImGui.isMouseDragging
import imgui.ImGui.isMouseReleased
import imgui.ImGui.isWindowHovered
import imgui.ImGui.mouseCursor
import imgui.ImGui.popAllowKeyboardFocus
import imgui.ImGui.pushAllowKeyboardFocus
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.setKeyboardFocusHere
import imgui.ImGui.style
import imgui.ImGui.text
import imgui.ImGui.textWrapped
import imgui.ImGui.u32
import imgui.ImGui.windowDrawList
import imgui.functionalProgramming.collapsingHeader
import imgui.functionalProgramming.treeNode
import imgui.imgui.imgui_demoDebugInfo.Companion.showHelpMarker
import imgui.HoveredFlags as Hf

object inputAndFocus {

    /* Tabbing */
    var buf0 = "dummy".toCharArray(CharArray(32))


    /* Focus from code */
    var buf1 = "click on a button to set focus".toCharArray(CharArray(128))

    operator fun invoke() {

        collapsingHeader("Inputs & Focus") {

            checkbox("io.MouseDrawCursor", IO::mouseDrawCursor)
            sameLine(); showHelpMarker("Request ImGui to render a mouse cursor for you in software. Note that a mouse cursor rendered via regular GPU rendering will feel more laggy than hardware cursor, but will be more in sync with your other visuals.")

            text("WantCaptureMouse: ${IO.wantCaptureMouse}")
            text("WantCaptureKeyboard: ${IO.wantCaptureKeyboard}")
            text("WantTextInput: ${IO.wantTextInput}")
            text("WantMoveMouse: ${IO.wantMoveMouse}")

            treeNode("Keyboard & Mouse State") {
                text("Mouse pos: (%g, %g)", IO.mousePos.x, IO.mousePos.y)
                text("Mouse down:")
                for (i in 0 until IO.mouseDown.size)
                    if (IO.mouseDownDuration[i] >= 0f) {
                        sameLine()
                        text("b$i (%.02f secs)", IO.mouseDownDuration[i])
                    }
                text("Mouse clicked:")
                for (i in 0 until IO.mouseDown.size)
                    if (isMouseClicked(i)) {
                        sameLine()
                        text("b$i")
                    }
                text("Mouse dbl-clicked:")
                for (i in 0 until IO.mouseDown.size)
                    if (isMouseDoubleClicked(i)) {
                        sameLine()
                        text("b$i")
                    }
                text("Mouse released:")
                for (i in 0 until IO.mouseDown.size)
                    if (isMouseReleased(i)) {
                        sameLine()
                        text("b$i")
                    }
                text("Mouse wheel: %.1f", IO.mouseWheel)

                text("Keys down:")
                for (i in 0 until IO.keysDown.size)
                    if (IO.keysDownDuration[i] >= 0f) {
                        sameLine()
                        text("$i (%.02f secs)", IO.keysDownDuration[i])
                    }
                text("Keys pressed:")
                for (i in 0 until IO.keysDown.size)
                    if (isKeyPressed(i)) {
                        sameLine()
                        text("$i")
                    }
                text("Keys release:")
                for (i in 0 until IO.keysDown.size)
                    if (isKeyReleased(i)) {
                        sameLine()
                        text("$i")
                    }
                val ctrl = if (IO.keyCtrl) "CTRL " else ""
                val shift = if (IO.keyShift) "SHIFT " else ""
                val alt = if (IO.keyAlt) "ALT " else ""
                val `super` = if (IO.keySuper) "SUPER " else ""
                text("Keys mods: $ctrl$shift$alt$`super`")

                button("Hovering me sets the\nkeyboard capture flag")
                if (isItemHovered()) captureKeyboardFromApp(true)
                sameLine()
                button("Holding me clears the\nthe keyboard capture flag")
                if (isItemActive) captureKeyboardFromApp(false)
            }

            treeNode("Tabbing") {
                text("Use TAB/SHIFT+TAB to cycle through keyboard editable fields.")
                inputText("1", buf0)
                inputText("2", buf0)
                inputText("3", buf0)
                pushAllowKeyboardFocus(false)
                inputText("4 (tab skip)", buf0)
                //SameLine(); ShowHelperMarker("Use PushAllowKeyboardFocus(bool)\nto disable tabbing through certain widgets.");
                popAllowKeyboardFocus()
                inputText("5", buf0)
            }

            treeNode("Focus from code") {
                val focus1 = button("Focus on 1"); sameLine()
                val focus2 = button("Focus on 2"); sameLine()
                val focus3 = button("Focus on 3")
                var hasFocus = 0

                if (focus1) setKeyboardFocusHere()
                inputText("1", buf1)
                if (isItemActive) hasFocus = 1

                if (focus2) setKeyboardFocusHere()
                inputText("2", buf1)
                if (isItemActive) hasFocus = 2

                pushAllowKeyboardFocus(false)
                if (focus3) setKeyboardFocusHere()
                inputText("3 (tab skip)", buf1)
                if (isItemActive) hasFocus = 3
                popAllowKeyboardFocus()
                text("Item with focus: ${if (hasFocus != 0) "$hasFocus" else "<none>"}")
                textWrapped("Cursor & selection are preserved when refocusing last used item in code.")
            }

            treeNode("Hovering") {
                // Testing IsWindowHovered() function with its various flags (note that the flags can be combined)
                bulletText("IsWindowHovered() = ${isWindowHovered()}\n" +
                        "IsWindowHovered(_AllowWhenBlockedByPopup) = ${isWindowHovered(Hf.AllowWhenBlockedByPopup)}\n" +
                        "IsWindowHovered(_AllowWhenBlockedByActiveItem) = ${isWindowHovered(Hf.AllowWhenBlockedByActiveItem)}\n" +
                        "IsWindowHovered(_FlattenChilds) = ${isWindowHovered(Hf.FlattenChilds)}\n")
                // Testing IsItemHovered() function (because BulletText is an item itself and that would affect the output of IsItemHovered, we pass all lines in a single items to shorten the code)
                button("ITEM")
                bulletText("IsItemHovered() = ${isItemHovered()}\n" +
                        "IsItemHovered(_AllowWhenBlockedByPopup) = ${isItemHovered(Hf.AllowWhenBlockedByPopup)}\n" +
                        "IsItemHovered(_AllowWhenBlockedByActiveItem) = ${isItemHovered(Hf.AllowWhenBlockedByActiveItem)}\n" +
                        "IsItemHovered(_AllowWhenOverlapped) = ${isItemHovered(Hf.AllowWhenOverlapped)}\n" +
                        "IsItemhovered(_RectOnly) = ${isItemHovered(Hf.RectOnly)}\n")
            }

            treeNode("Dragging") {
                textWrapped("You can use getMouseDragDelta(0) to query for the dragged amount on any widget.")
                for (button in 0..2) text("IsMouseDragging($button) = ${isMouseDragging(button)}")
                button("Drag Me")
                if (isItemActive) {
                    // Draw a line between the button and the mouse cursor
                    with(windowDrawList) {
                        pushClipRectFullScreen()
                        addLine(calcItemRectClosestPoint(IO.mousePos, true, -2f), IO.mousePos,
                                style.colors[Col.Button].u32, 4f)
                        popClipRect()
                    }
                    val valueRaw = getMouseDragDelta(0, 0f)
                    val valueWithLockThreshold = getMouseDragDelta(0)
                    val mouseDelta = IO.mouseDelta
                    sameLine(); text("Raw (${valueRaw.x.i}, ${valueRaw.y.i}), WithLockThresold (${valueWithLockThreshold.x.i}, " +
                            "${valueWithLockThreshold.y.i}), MouseDelta (${mouseDelta.x.i}, ${mouseDelta.y.i})")
                }
            }

            treeNode("Mouse cursors") {
                text("Current mouse cursor = $mouseCursor")
                text("Hover to see mouse cursors:")
                sameLine(); showHelpMarker("Your application can render a different mouse cursor based on what GetMouseCursor() returns. If software cursor rendering (io.MouseDrawCursor) is set ImGui will draw the right cursor for you, otherwise your backend needs to handle it.")
                for (i in 0 until MouseCursor.Count.i) {
                    bullet(); selectable("Mouse cursor $i: ${MouseCursor.of(i)}", false)
                    if (isItemHovered()) mouseCursor = MouseCursor.of(i)
                }
            }
        }
    }
}