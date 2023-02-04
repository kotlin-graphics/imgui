package imgui.demo

import glm_.i
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.beginDisabled
import imgui.ImGui.bullet
import imgui.ImGui.button
import imgui.ImGui.checkboxFlags
import imgui.ImGui.colorButton
import imgui.ImGui.dummy
import imgui.ImGui.endDisabled
import imgui.ImGui.foregroundDrawList
import imgui.ImGui.getMouseClickedCount
import imgui.ImGui.getMouseDragDelta
import imgui.ImGui.inputText
import imgui.ImGui.io
import imgui.ImGui.isDown
import imgui.ImGui.isItemActive
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isMouseDown
import imgui.ImGui.isMouseDragging
import imgui.ImGui.isMousePosValid
import imgui.ImGui.isMouseReleased
import imgui.ImGui.isPressed
import imgui.ImGui.isReleased
import imgui.ImGui.popAllowKeyboardFocus
import imgui.ImGui.pushAllowKeyboardFocus
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.setKeyboardFocusHere
import imgui.ImGui.setNextFrameWantCaptureKeyboard
import imgui.ImGui.setNextFrameWantCaptureMouse
import imgui.ImGui.setNextItemOpen
import imgui.ImGui.setNextItemWidth
import imgui.ImGui.sliderFloat3
import imgui.ImGui.sliderInt
import imgui.ImGui.text
import imgui.ImGui.textWrapped
import imgui.api.demoDebugInformations.Companion.helpMarker
import imgui.dsl.collapsingHeader
import imgui.dsl.treeNode
import imgui.internal.sections.DrawFlag

object ShowDemoWindowInputs {

    var captureOverrideMouse = -1
    var captureOverrideKeyboard = -1

    operator fun invoke() {

        collapsingHeader("Inputs, Navigation & Focus") {

            // Display ImGuiIO output flags
            setNextItemOpen(true, Cond.Once)
            treeNode("Output") {
                text("io.WantCaptureMouse: ${io.wantCaptureMouse}")
                text("io.WantCaptureMouseUnlessPopupClose: ${io.wantCaptureMouseUnlessPopupClose}")
                text("io.WantCaptureKeyboard: ${io.wantCaptureKeyboard}")
                text("io.WantTextInput: ${io.wantTextInput}")
                text("io.WantMoveMouse: ${io.wantSetMousePos}")
                text("io.NavActive: ${io.navActive}, io.NavVisible: ${io.navVisible}")
            }

            // Display Mouse state
            treeNode("Mouse State") {
                if (isMousePosValid()) text("Mouse pos: (%g, %g)", io.mousePos.x, io.mousePos.y)
                else text("Mouse pos: <INVALID>")
                text("Mouse delta: (%g, %g)", io.mouseDelta.x, io.mouseDelta.y)
                text("Mouse down:")
                val indices = io.mouseDown.indices
                for (i in indices)
                    if (isMouseDown(MouseButton.of(i))) {
                        sameLine(); text("b$i (%.02f secs)", io.mouseDownDuration[i])
                    }
                text("Mouse clicked:")
                for (i in indices)
                    if (isMouseClicked(MouseButton of i)) {
                        sameLine(); text("b$i (${getMouseClickedCount(MouseButton of i)})")
                    }
                text("Mouse released:")
                for (i in indices)
                    if (isMouseReleased(MouseButton of i)) {
                        sameLine()
                        text("b$i")
                    }
                text("Mouse wheel: %.1f", io.mouseWheel)
                text("Pen Pressure: %.1f", io.penPressure) // Note: currently unused
            }

            // Display mouse cursors
            //            IMGUI_DEMO_MARKER("Inputs, Navigation & Focus/Mouse Cursors");
            treeNode("Mouse Cursors") {

                val current = ImGui.mouseCursor
                text("Current mouse cursor = ${current.i}: $current")
                beginDisabled(true)
                checkboxFlags("io.BackendFlags: HasMouseCursors", io::backendFlags, BackendFlag.HasMouseCursors.i)
                endDisabled()

                text("Hover to see mouse cursors:")
                sameLine(); helpMarker(
                "Your application can render a different mouse cursor based on what ImGui::GetMouseCursor() returns. " +
                        "If software cursor rendering (io.MouseDrawCursor) is set ImGui will draw the right cursor for you, " +
                        "otherwise your backend needs to handle it.")
                for (i in 0 until MouseCursor.COUNT) {
                    val label = "Mouse cursor $i: ${MouseCursor of i}"
                    bullet(); selectable(label, false)
                    if (ImGui.isItemHovered())
                        ImGui.mouseCursor = MouseCursor of i
                }
            }

            // Display Keyboard/Mouse state
            treeNode("Keyboard, Gamepad & Navigation State") {

                // We iterate both legacy native range and named ImGuiKey ranges, which is a little odd but this allows displaying the data for old/new backends.
                // User code should never have to go through such hoops: old code may use native keycodes, new code may use ImGuiKey codes.
                TODO()

                text("Keys down:")
                for (i in io.keysData.indices) {
                    val key = Key of (i + Key.BEGIN)
                    if (key.isDown) {
                        sameLine(); text("\"$key\" $i (%.02f)", io.keysData[i].downDuration)
                    }
                }
                text("Keys pressed:")
                for (i in io.keysData.indices) {
                    val key = Key of (i + Key.BEGIN)
                    if (key.isPressed) {
                        sameLine(); text("\"$key\" $i")
                    }
                }
                text("Keys released:")
                for (i in io.keysData.indices) {
                    val key = Key of (i + Key.BEGIN)
                    if (key.isReleased) {
                        sameLine(); text("\"$key\" $i")
                    }
                }
                val ctrl = if (io.keyCtrl) "CTRL " else ""
                val shift = if (io.keyShift) "SHIFT " else ""
                val alt = if (io.keyAlt) "ALT " else ""
                val super_ = if (io.keySuper) "SUPER " else ""
                text("Keys mods: $ctrl$shift$alt$super_")
                text("Chars queue:")
                io.inputQueueCharacters.forEach { c ->
                    // UTF-8 will represent some characters using multiple bytes, so we join them here
                    // example: 'ç' becomes "0xC3, 0xA7"
                    val bytes = c.toString().toByteArray().joinToString { "0x%X".format(it) }
                    sameLine(); text("\'%c\' (%s)", if (c > ' ' && c.i <= 255) c else '?', bytes)
                }
            }

            // Draw an arbitrary US keyboard layout to visualize translated keys
            run {
                val keySize = Vec2(35f)
                val keyRounding = 3f
                val keyfaceSize = Vec2(25f)
                val keyfacePos = Vec2(5f, 3f)
                val keyfaceRounding = 2f
                val keylabelPos = Vec2(7f, 4f)
                val keyStep = keySize - 1f
                val keyrowOffset = 9f

                val boardMin = ImGui.cursorScreenPos // [JVM] same instance
                val boardMax = Vec2(boardMin.x + 3 * keyStep.x + 2 * keyrowOffset + 10f, boardMin.y + 3 * keyStep.y + 10f)
                val startPos = Vec2(boardMin.x + 5f - keyStep.x, boardMin.y)

                class KeyLayoutData(val row: Int, val col: Int, val label: String, val key: Key)

                val keysToDisplay = listOf(
                    KeyLayoutData(0, 0, "", Key.Tab), KeyLayoutData(0, 1, "Q", Key.Q), KeyLayoutData(0, 2, "W", Key.W), KeyLayoutData(0, 3, "E", Key.E), KeyLayoutData(0, 4, "R", Key.R),
                    KeyLayoutData(1, 0, "", Key.CapsLock), KeyLayoutData(1, 1, "A", Key.A), KeyLayoutData(1, 2, "S", Key.S), KeyLayoutData(1, 3, "D", Key.D), KeyLayoutData(1, 4, "F", Key.F),
                    KeyLayoutData(2, 0, "", Key.LeftShift), KeyLayoutData(2, 1, "Z", Key.Z), KeyLayoutData(2, 2, "X", Key.X), KeyLayoutData(2, 3, "C", Key.C), KeyLayoutData(2, 4, "V", Key.V))

                // Elements rendered manually via ImDrawList API are not clipped automatically.
                // While not strictly necessary, here IsItemVisible() is used to avoid rendering these shapes when they are out of view.
                dummy(Vec2(boardMax.x - boardMin.x, boardMax.y - boardMin.y))
                if (ImGui.isItemVisible) {
                    val drawList = ImGui.windowDrawList
                    drawList.pushClipRect(boardMin, boardMax, true)
                    for (keyData in keysToDisplay) {
                        val keyMin = Vec2(startPos.x + keyData.col * keyStep.x + keyData.row * keyrowOffset, startPos.y + keyData.row * keyStep.y)
                        val keyMax = Vec2(keyMin.x + keySize.x, keyMin.y + keySize.y)
                        drawList.addRectFilled(keyMin, keyMax, COL32(204, 204, 204, 255), keyRounding)
                        drawList.addRect(keyMin, keyMax, COL32(24, 24, 24, 255), keyRounding)
                        val faceMin = Vec2(keyMin.x + keyfacePos.x, keyMin.y + keyfacePos.y)
                        val faceMax = Vec2(faceMin.x + keyfaceSize.x, faceMin.y + keyfaceSize.y)
                        drawList.addRect(faceMin, faceMax, COL32(193, 193, 193, 255), keyfaceRounding, DrawFlag.None.i, 2f)
                        drawList.addRectFilled(faceMin, faceMax, COL32(252, 252, 252, 255), keyfaceRounding)
                        val labelMin = Vec2(keyMin.x + keylabelPos.x, keyMin.y + keylabelPos.y)
                        drawList.addText(labelMin, COL32(64, 64, 64, 255), keyData.label)
                        if (keyData.key.isDown)
                            drawList.addRectFilled(keyMin, keyMax, COL32(255, 0, 0, 128), keyRounding)
                    }
                    drawList.popClipRect()
                }
            }

            treeNode("Capture override") {
                helpMarker(
                    "The value of io.WantCaptureMouse and io.WantCaptureKeyboard are normally set by Dear ImGui " +
                            "to instruct your application of how to route inputs. Typically, when a value is true, it means " +
                            "Dear ImGui wants the corresponding inputs and we expect the underlying application to ignore them.\n\n" +
                            "The most typical case is: when hovering a window, Dear ImGui set io.WantCaptureMouse to true, " +
                            "and underlying application should ignore mouse inputs (in practice there are many and more subtle " +
                            "rules leading to how those flags are set).")

                text("io.WantCaptureMouse: ${io.wantCaptureMouse.i}")
                text("io.WantCaptureMouseUnlessPopupClose: ${io.wantCaptureMouseUnlessPopupClose.i}")
                text("io.WantCaptureKeyboard: ${io.wantCaptureKeyboard.i}")

                helpMarker(
                    "Hovering the colored canvas will override io.WantCaptureXXX fields.\n" +
                            "Notice how normally (when set to none), the value of io.WantCaptureKeyboard would be false when hovering and true when clicking.")
                val captureOverrideDesc = listOf("None", "Set to false", "Set to true")
                setNextItemWidth(ImGui.fontSize * 15)
                sliderInt("SetNextFrameWantCaptureMouse()", ::captureOverrideMouse, -1, +1, captureOverrideDesc[captureOverrideMouse + 1], SliderFlag.AlwaysClamp.i)
                setNextItemWidth(ImGui.fontSize * 15)
                sliderInt("SetNextFrameWantCaptureKeyboard()", ::captureOverrideKeyboard, -1, +1, captureOverrideDesc[captureOverrideKeyboard + 1], SliderFlag.AlwaysClamp.i)

                colorButton("##panel", Vec4(0.7f, 0.1f, 0.7f, 1f), ColorEditFlag.NoTooltip or ColorEditFlag.NoDragDrop, Vec2(256f, 192f)) // Dummy item
                if (ImGui.isItemHovered() && captureOverrideMouse != -1)
                    setNextFrameWantCaptureMouse(captureOverrideMouse == 1)
                if (ImGui.isItemHovered() && captureOverrideKeyboard != -1)
                    setNextFrameWantCaptureKeyboard(captureOverrideKeyboard == 1)

            }

            Tabbing()

            `Focus from code`()

            treeNode("Dragging") {
                textWrapped("You can use getMouseDragDelta(0) to query for the dragged amount on any widget.")
                for (button in MouseButton.values())
                    if (button != MouseButton.None) {
                        text("IsMouseDragging(${button.i}):")
                        text("  w/ default threshold: ${isMouseDragging(button).i},")
                        text("  w/ zero threshold: ${isMouseDragging(button, 0f).i},")
                        text("  w/ large threshold: ${isMouseDragging(button, 20f)},")
                    }
                button("Drag Me")
                if (isItemActive)
                    foregroundDrawList.addLine(io.mouseClickedPos[0], io.mousePos, Col.Button.u32, 4f) // Draw a line between the button and the mouse cursor

                // Drag operations gets "unlocked" when the mouse has moved past a certain threshold
                // (the default threshold is stored in io.MouseDragThreshold). You can request a lower or higher
                // threshold using the second parameter of IsMouseDragging() and GetMouseDragDelta().
                val valueRaw = getMouseDragDelta(MouseButton.Left, 0f)
                val valueWithLockThreshold = getMouseDragDelta(MouseButton.Left)
                val mouseDelta = io.mouseDelta
                text("GetMouseDragDelta(0):")
                text("  w/ default threshold: (%.1f, %.1f)", valueWithLockThreshold.x, valueWithLockThreshold.y)
                text("  w/ zero threshold: (%.1f, %.1f)", valueRaw.x, valueRaw.y)
                text("io.MouseDelta: (%.1f, %.1f)", mouseDelta.x, mouseDelta.y)
            }
        }
    }

    object Tabbing {
        var buf = "hello".toByteArray(32)
        operator fun invoke() {
            treeNode("Tabbing") {
                text("Use TAB/SHIFT+TAB to cycle through keyboard editable fields.")
                inputText("1", buf)
                inputText("2", buf)
                inputText("3", buf)
                pushAllowKeyboardFocus(false)
                inputText("4 (tab skip)", buf)
                sameLine(); helpMarker("Item won't be cycled through when using TAB or Shift+Tab.")
                popAllowKeyboardFocus()
                inputText("5", buf)
            }
        }
    }

    object `Focus from code` {
        var buf = "click on a button to set focus".toByteArray(128)
        val f3 = FloatArray(3)
        operator fun invoke() {
            treeNode("Focus from code") {
                val focus1 = button("Focus on 1"); sameLine()
                val focus2 = button("Focus on 2"); sameLine()
                val focus3 = button("Focus on 3")
                var hasFocus = 0

                if (focus1) setKeyboardFocusHere()
                inputText("1", buf)
                if (isItemActive) hasFocus = 1

                if (focus2) setKeyboardFocusHere()
                inputText("2", buf)
                if (isItemActive) hasFocus = 2

                pushAllowKeyboardFocus(false)
                if (focus3) setKeyboardFocusHere()
                inputText("3 (tab skip)", buf)
                if (isItemActive) hasFocus = 3
                sameLine(); helpMarker("Item won't be cycled through when using TAB or Shift+Tab.")
                popAllowKeyboardFocus()

                text("Item with focus: ${if (hasFocus != 0) "$hasFocus" else "<none>"}")

                // Use >= 0 parameter to SetKeyboardFocusHere() to focus an upcoming item
                var focusAhead = -1
                if (button("Focus on X")) focusAhead = 0; sameLine()
                if (button("Focus on Y")) focusAhead = 1; sameLine()
                if (button("Focus on Z")) focusAhead = 2
                if (focusAhead != -1) setKeyboardFocusHere(focusAhead)
                sliderFloat3("Float3", f3, 0f, 1f)

                textWrapped("NB: Cursor & selection are preserved when refocusing last used item in code.")
            }
        }
    }
}