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
import imgui.ImGui.data
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

        collapsingHeader("Inputs & Focus") {

            // Display inputs submitted to ImGuiIO
            setNextItemOpen(true, Cond.Once)
            treeNode("Inputs") {
                helpMarker("This is a simplified view. See more detailed input state:\n" +
                                   "- in 'Tools->Metrics/Debugger->Inputs'.\n" +
                                   "- in 'Tools->Debug Log->IO'.")
                if (isMousePosValid())
                    text("Mouse pos: (%g, %g)", io.mousePos.x, io.mousePos.y)
                else
                    text("Mouse pos: <INVALID>")
                text("Mouse delta: (%g, %g)", io.mouseDelta.x, io.mouseDelta.y)
                text("Mouse down:")
                for (i in io.mouseDown.indices) if (isMouseDown(MouseButton.of(i))) {; sameLine(); text("b$i (%.02f secs)", io.mouseDownDuration[i]); }
                text("Mouse wheel: %.1f", io.mouseWheel)

                // We iterate both legacy native range and named ImGuiKey ranges, which is a little odd but this allows displaying the data for old/new backends.
                // User code should never have to go through such hoops: old code may use native keycodes, new code may use ImGuiKey codes.

                text("Keys down:")
                for (keyIdx in Key.BEGIN until Key.END) {
                    val key = Key of keyIdx
                    if (!key.isDown) continue
                    sameLine(); text('"' + key.name + '"'); sameLine(); text("(%.02f)", key.data.downDuration)
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

            // Display ImGuiIO output flags
            //            IMGUI_DEMO_MARKER("Inputs & Focus/Outputs");
            setNextItemOpen(true, Cond.Once)
            treeNode("Outputs") {
                text("io.WantCaptureMouse: ${io.wantCaptureMouse.i}")
                text("io.WantCaptureMouseUnlessPopupClose: ${io.wantCaptureMouseUnlessPopupClose.i}")
                text("io.WantCaptureKeyboard: ${io.wantCaptureKeyboard.i}")
                text("io.WantTextInput: ${io.wantTextInput.i}")
                text("io.WantSetMousePos: ${io.wantSetMousePos.i}")
                text("io.NavActive: ${io.navActive.i}, io.NavVisible: ${io.navVisible.i}")
            }

            //            IMGUI_DEMO_MARKER("Inputs & Focus/IO Output: Capture override")
            treeNode("IO Output: Capture override") {
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

            // Display mouse cursors
            //            IMGUI_DEMO_MARKER("Inputs & Focus/Mouse Cursors");
            treeNode("Mouse Cursors") {
                //                const char* mouse_cursors_names[] = { "Arrow", "TextInput", "ResizeAll", "ResizeNS", "ResizeEW", "ResizeNESW", "ResizeNWSE", "Hand", "NotAllowed" };
                //                IM_ASSERT(IM_ARRAYSIZE(mouse_cursors_names) == ImGuiMouseCursor_COUNT);

                val current = ImGui.mouseCursor
                text("Current mouse cursor = ${current.i}: $current")
                beginDisabled(true)
                checkboxFlags("io.BackendFlags: HasMouseCursors", io::backendFlags, BackendFlag.HasMouseCursors.i)
                endDisabled()

                text("Hover to see mouse cursors:")
                sameLine(); helpMarker("Your application can render a different mouse cursor based on what ImGui::GetMouseCursor() returns. " +
                                               "If software cursor rendering (io.MouseDrawCursor) is set ImGui will draw the right cursor for you, " +
                                               "otherwise your backend needs to handle it.")
                for (i in 0 until MouseCursor.COUNT) {
                    val cursor = MouseCursor of i
                    val label = "Mouse cursor $i: $cursor"
                    bullet(); selectable(label, false)
                    if (ImGui.isItemHovered())
                        ImGui.mouseCursor = cursor
                }
            }

            Tabbing()

            `Focus from code`()

//            IMGUI_DEMO_MARKER("Inputs & Focus/Dragging");
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
            //            IMGUI_DEMO_MARKER("Inputs & Focus/Tabbing");
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
//            IMGUI_DEMO_MARKER("Inputs & Focus/Focus from code");
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