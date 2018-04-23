package imgui.imgui.demo

import glm_.i
import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.beginChild
import imgui.ImGui.bullet
import imgui.ImGui.bulletText
import imgui.ImGui.button
import imgui.ImGui.captureKeyboardFromApp
import imgui.ImGui.checkbox
import imgui.ImGui.checkboxFlags
import imgui.ImGui.endChild
import imgui.ImGui.fontSize
import imgui.ImGui.getMouseDragDelta
import imgui.ImGui.inputText
import imgui.ImGui.io
import imgui.ImGui.isItemActive
import imgui.ImGui.isItemFocused
import imgui.ImGui.isItemHovered
import imgui.ImGui.isKeyPressed
import imgui.ImGui.isKeyReleased
import imgui.ImGui.isMouseClicked
import imgui.ImGui.isMouseDoubleClicked
import imgui.ImGui.isMouseDragging
import imgui.ImGui.isMousePosValid
import imgui.ImGui.isMouseReleased
import imgui.ImGui.isWindowFocused
import imgui.ImGui.isWindowHovered
import imgui.ImGui.mouseCursor
import imgui.ImGui.popAllowKeyboardFocus
import imgui.ImGui.pushAllowKeyboardFocus
import imgui.ImGui.sameLine
import imgui.ImGui.selectable
import imgui.ImGui.setKeyboardFocusHere
import imgui.ImGui.sliderFloat3
import imgui.ImGui.text
import imgui.ImGui.textWrapped
import imgui.ImGui.windowDrawList
import imgui.functionalProgramming.collapsingHeader
import imgui.functionalProgramming.treeNode
import imgui.imgui.imgui_demoDebugInformations.Companion.showHelpMarker
import imgui.FocusedFlag as Ff
import imgui.HoveredFlag as Hf

object inputNavigationAndFocus {

    /* Tabbing */
    var buf0 = "dummy".toCharArray(CharArray(32))


    /* Focus from code */
    var buf1 = "click on a button to set focus".toCharArray(CharArray(128))
    val f3 = FloatArray(3)
    var float3 = 0f

    /** Focused & Hovered Test */
    var embedAllInsideAchildWindow = false

    operator fun invoke() {

        collapsingHeader("Inputs, Navigation & Focus") {

            text("WantCaptureMouse: ${io.wantCaptureMouse}")
            text("WantCaptureKeyboard: ${io.wantCaptureKeyboard}")
            text("WantTextInput: ${io.wantTextInput}")
            text("WantMoveMouse: ${io.wantSetMousePos}")
            text("NavActive: ${io.navActive}, NavVisible: ${io.navVisible}")

            checkbox("io.MouseDrawCursor", io::mouseDrawCursor)
            sameLine(); showHelpMarker("Request ImGui to render a mouse cursor for you in software. Note that " +
                "a mouse cursor rendered via your application GPU rendering path will feel more laggy than hardware " +
                "cursor, but will be more in sync with your other visuals.\n\nSome desktop applications may use both " +
                "kinds of cursors (e.g. enable software cursor only when resizing/dragging something).")
            checkboxFlags("io.ConfigFlags: NavEnableGamepad", io::configFlags, ConfigFlag.NavEnableGamepad.i)
            checkboxFlags("io.ConfigFlags: NavEnableKeyboard", io::configFlags, ConfigFlag.NavEnableKeyboard.i)
            checkboxFlags("io.ConfigFlags: NavEnableSetMousePos", io::configFlags, ConfigFlag.NavEnableSetMousePos.i)
            checkboxFlags("io.ConfigFlags: NoSetMouseCursor", io::configFlags, ConfigFlag.NoSetMouseCursor.i)
            sameLine(); showHelpMarker("Request ImGui to move your move cursor when using gamepad/keyboard " +
                "navigation. NewFrame() will change io.MousePos and set the io.WantMoveMouse flag, your backend will " +
                "need to apply the new mouse position.")

            treeNode("Keyboard, Mouse & Navigation State") {
                if (isMousePosValid()) text("Mouse pos: (%g, %g)", io.mousePos.x, io.mousePos.y)
                else text("Mouse pos: <INVALID>")
                text("Mouse down:")
                for (i in 0 until io.mouseDown.size)
                    if (io.mouseDownDuration[i] >= 0f) {
                        sameLine()
                        text("b$i (%.02f secs)", io.mouseDownDuration[i])
                    }
                text("Mouse clicked:")
                for (i in 0 until io.mouseDown.size)
                    if (isMouseClicked(i)) {
                        sameLine()
                        text("b$i")
                    }
                text("Mouse dbl-clicked:")
                for (i in 0 until io.mouseDown.size)
                    if (isMouseDoubleClicked(i)) {
                        sameLine()
                        text("b$i")
                    }
                text("Mouse released:")
                for (i in 0 until io.mouseDown.size)
                    if (isMouseReleased(i)) {
                        sameLine()
                        text("b$i")
                    }
                text("Mouse wheel: %.1f", io.mouseWheel)

                text("Keys down:")
                for (i in io.keysDown.indices)
                    if (io.keysDownDuration[i] >= 0f) {
                        sameLine()
                        text("$i (%.02f secs)", io.keysDownDuration[i])
                    }
                text("Keys pressed:")
                for (i in io.keysDown.indices)
                    if (isKeyPressed(i)) {
                        sameLine()
                        text("$i")
                    }
                text("Keys release:")
                for (i in io.keysDown.indices)
                    if (isKeyReleased(i)) {
                        sameLine()
                        text("$i")
                    }
                val ctrl = if (io.keyCtrl) "CTRL " else ""
                val shift = if (io.keyShift) "SHIFT " else ""
                val alt = if (io.keyAlt) "ALT " else ""
                val super_ = if (io.keySuper) "SUPER " else ""
                text("Keys mods: $ctrl$shift$alt$super_")

                text("NavInputs down:")
                io.navInputs.filter { it > 0f }.forEachIndexed { i, it -> sameLine(); text("[$i] %.2f", it) }
                text("NavInputs pressed:")
                io.navInputsDownDuration.filter { it == 0f }.forEachIndexed { i, it -> sameLine(); text("[$i]") }
                text("NavInputs duration:")
                io.navInputsDownDuration.filter { it >= 0f }.forEachIndexed { i, it -> sameLine(); text("[$i] %.2f", it) }

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

                // Use >= 0 parameter to SetKeyboardFocusHere() to focus an upcoming item
                var focusAhead = -1
                if (button("Focus on X")) focusAhead = 0; sameLine()
                if (button("Focus on Y")) focusAhead = 1; sameLine()
                if (button("Focus on Z")) focusAhead = 2
                if (focusAhead != -1) setKeyboardFocusHere(focusAhead)
                sliderFloat3("Float3", f3, 0f, 1f)

                textWrapped("NB: Cursor & selection are preserved when refocusing last used item in code.")
            }

            treeNode("Focused & Hovered Test") {
                // TODO to test

                checkbox("Embed everything inside a child window (for additional testing)", ::embedAllInsideAchildWindow)
                if (embedAllInsideAchildWindow)
                    beginChild("embeddingchild", Vec2(0, fontSize * 25), true)

                // Testing IsWindowFocused() function with its various flags (note that the flags can be combined)
                bulletText("""
                    IsWindowFocused() = ${isWindowFocused()}
                    IsWindowFocused(_ChildWindows) = ${isWindowFocused(Ff.ChildWindows.i)}
                    IsWindowFocused(_ChildWindows|_RootWindow) = ${isWindowFocused(Ff.ChildWindows or Ff.RootWindow)}
                    IsWindowFocused(_RootWindow) = ${isWindowFocused(Ff.RootWindow.i)}
                    IsWindowFocused(_AnyWindow) = ${isWindowFocused(Ff.AnyWindow.i)}""")

                // Testing IsWindowHovered() function with its various flags (note that the flags can be combined)
                bulletText("IsWindowHovered() = ${isWindowHovered()}\n" + // TODO triple quote?
                        "IsWindowHovered(_AllowWhenBlockedByPopup) = ${isWindowHovered(Hf.AllowWhenBlockedByPopup)}\n" +
                        "IsWindowHovered(_AllowWhenBlockedByActiveItem) = ${isWindowHovered(Hf.AllowWhenBlockedByActiveItem)}\n" +
                        "IsWindowHovered(_ChildWindows) = ${isWindowHovered(Hf.ChildWindows)}\n" +
                        "IsWindowHovered(_ChildWindows|_RootWindow) = ${isWindowHovered(Hf.ChildWindows or Hf.RootWindow)}\n" +
                        "IsWindowHovered(_RootWindow) = ${isWindowHovered(Hf.RootWindow)}\n" +
                        "IsWindowHovered(_AnyWindow) = ${isWindowHovered(Hf.AnyWindow)}")
                // Testing IsItemHovered() function (because BulletText is an item itself and that would affect the output of IsItemHovered, we pass all lines in a single items to shorten the code)
                button("ITEM")
                bulletText("IsItemHovered() = ${isItemHovered()}\n" + // TODO triple quote?
                        "IsItemHovered(_AllowWhenBlockedByPopup) = ${isItemHovered(Hf.AllowWhenBlockedByPopup)}\n" +
                        "IsItemHovered(_AllowWhenBlockedByActiveItem) = ${isItemHovered(Hf.AllowWhenBlockedByActiveItem)}\n" +
                        "IsItemHovered(_AllowWhenOverlapped) = ${isItemHovered(Hf.AllowWhenOverlapped)}\n" +
                        "IsItemhovered(_RectOnly) = ${isItemHovered(Hf.RectOnly)}\n")

                beginChild("child", Vec2(0, 50), true)
                text("This is another child window for testing IsWindowHovered() flags.")
                endChild()

                if (embedAllInsideAchildWindow) endChild()
            }

            treeNode("Dragging") {
                textWrapped("You can use getMouseDragDelta(0) to query for the dragged amount on any widget.")
                for (button in 0..2)
                    text("isMouseDragging($button):  w/ default threshold: ${isMouseDragging(button)},  w/ zero threshold: " +
                            "${isMouseDragging(button, 0f)}\n  w/ large threshold: ${isMouseDragging(button, 20f)}")
                button("Drag Me")
                if (isItemActive) {
                    // Draw a line between the button and the mouse cursor
                    with(windowDrawList) {
                        pushClipRectFullScreen()
                        addLine(io.mouseClickedPos[0], io.mousePos, Col.Button.u32, 4f)
                        popClipRect()
                    }

                    /*  Drag operations gets "unlocked" when the mouse has moved past a certain threshold (the default
                        threshold is stored in io.mouseDragThreshold)
                        You can request a lower or higher threshold using the second parameter of isMouseDragging() and
                        getMouseDragDelta()     */
                    val valueRaw = getMouseDragDelta(0, 0f)
                    val valueWithLockThreshold = getMouseDragDelta(0)
                    val mouseDelta = io.mouseDelta
                    sameLine(); text("Raw (${valueRaw.x.i}, ${valueRaw.y.i}), WithLockThresold (${valueWithLockThreshold.x.i}, " +
                            "${valueWithLockThreshold.y.i}), MouseDelta (${mouseDelta.x.i}, ${mouseDelta.y.i})")
                }
            }

            treeNode("Mouse cursors") {
                text("Current mouse cursor = $mouseCursor")
                text("Hover to see mouse cursors:")
                sameLine(); showHelpMarker("Your application can render a different mouse cursor based on what GetMouseCursor() returns. If software cursor rendering (io.MouseDrawCursor) is set ImGui will draw the right cursor for you, otherwise your backend needs to handle it.")
                for (i in 0 until MouseCursor.COUNT) {
                    bullet(); selectable("Mouse cursor $i: ${MouseCursor.of(i)}", false)
                    if (isItemHovered() || isItemFocused)
                        mouseCursor = MouseCursor.of(i)
                }
            }
        }
    }
}