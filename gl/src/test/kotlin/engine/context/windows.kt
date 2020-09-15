package engine.context

import engine.core.*
import engine.hashDecoratedPath
import glm_.f
import glm_.vec2.Vec2
import imgui.Cond
import imgui.ID
import imgui.ImGui
import imgui.ImGui.findWindowByID
import imgui.ImGui.focusWindow
import imgui.internal.classes.Rect
import imgui.internal.classes.Window
import imgui.internal.floor
import imgui.internal.lengthSqr
import imgui.toByteArray
import io.kotest.matchers.shouldBe

// [JVM]
fun TestContext.windowRef(ref: ID) = windowRef(TestRef(ref))
// [JVM]
fun TestContext.windowRef(ref: String) = windowRef(TestRef(path = ref))

// FIXME-TESTS: May be to focus window when docked? Otherwise locate request won't even see an item?
fun TestContext.windowRef(ref: TestRef) {

    REGISTER_DEPTH {
        logDebug("WindowRef '${ref.path ?: "NULL"}' %08X", ref.id)

        ref.path?.let {
//            size_t len = strlen(ref.Path)
//            IM_ASSERT(len < IM_ARRAYSIZE(RefStr) - 1)

            it.toByteArray(refStr)
            refID = hashDecoratedPath(it, 0)
        } ?: run {
            refStr[0] = 0
            refID = ref.id
        }

        // Automatically uncollapse by default
        if (opFlags hasnt TestOpFlag.NoAutoUncollapse)
            getWindowByRef("")?.let { windowAutoUncollapse(it) }
    }
}

// [JVM]
fun TestContext.windowClose(ref: String) = windowClose(TestRef(path = ref))

fun TestContext.windowClose(ref: TestRef) {
    if (isError) return

    REGISTER_DEPTH {
        logDebug("WindowClose")
        itemClick(getID("#CLOSE", ref))
    }
}

fun TestContext.windowCollapse(window: Window?, collapsed: Boolean) {
    if (isError) return
    if (window == null) return

    REGISTER_DEPTH {
        logDebug("WindowSetCollapsed $collapsed")
        //ImGuiWindow* window = GetWindowByRef(ref);
        //if (window == NULL)
        //{
        //    IM_ERRORF_NOHDR("Unable to find Ref window: %s / %08X", RefStr, RefID);
        //    return;
        //}

        if (window.collapsed != collapsed) {
            var opFlags = opFlags
            val backupOpFlags = opFlags
            opFlags = opFlags or TestOpFlag.NoAutoUncollapse
            itemClick(getID("#COLLAPSE", window.id))
            opFlags = backupOpFlags
            yield()
            CHECK(window.collapsed == collapsed)
        }
    }
}

fun TestContext.windowAutoUncollapse(window: Window) {
    if (window.collapsed)
        REGISTER_DEPTH {
            logDebug("Uncollapse window '${window.name}'")
            windowCollapse(window, false)
            window.collapsed shouldBe false
        }
}

// [JVM]
fun TestContext.windowFocus(ref: String) = windowFocus(TestRef(path = ref))

// FIXME-TESTS: Ideally we would aim toward a clickable spot in the window.
fun TestContext.windowFocus(ref: TestRef) {

    REGISTER_DEPTH {
        val desc = TestRefDesc(ref)
        logDebug("FocusWindow('$desc')")

        val windowId = getID(ref)
        val window = findWindowByID(windowId)
        CHECK_SILENT(window != null)
        window?.let {
            focusWindow(it)
            yield()
        }
    }
}

// [JVM]
fun TestContext.windowMove(ref: String, inputPos: Vec2, pivot: Vec2 = Vec2()) = windowMove(TestRef(path = ref), inputPos, pivot)

fun TestContext.windowMove(ref: TestRef, inputPos: Vec2, pivot: Vec2 = Vec2()) {

    if (isError) return

    val window = getWindowByRef(ref)
    CHECK_SILENT(window != null)
    window!!

    REGISTER_DEPTH {
        logDebug("WindowMove ${window.name} (%.1f,%.1f) ", inputPos.x, inputPos.y)
        val targetPos = floor(inputPos - pivot * window.size)
        if ((targetPos - window.pos).lengthSqr < 0.001f)
            return

        windowBringToFront(window)
        windowCollapse(window, false)

        val h = ImGui.frameHeight

        // FIXME-TESTS: Need to find a -visible- click point
        mouseMoveToPos(window.pos + Vec2(h * 2f, h * 0.5f))
        //IM_CHECK_SILENT(UiContext->HoveredWindow == window);  // FIXME-TESTS:
        mouseDown(0)

        // Disable docking
//        #if helpers.getIMGUI_HAS_DOCK
//        if (UiContext->IO.ConfigDockingWithShift)
//        KeyUpMap(ImGuiKey_COUNT, ImGuiKeyModFlags_Shift)
//        else
//        KeyDownMap(ImGuiKey_COUNT, ImGuiKeyModFlags_Shift)
//        #endif

        val delta = targetPos - window.pos
        mouseMoveToPos(inputs!!.mousePosValue + delta)
        yield()

        mouseUp()
//        #if helpers.getIMGUI_HAS_DOCK
//        KeyUpMap(ImGuiKey_COUNT, ImGuiKeyModFlags_Shift)
//        #endif
    }
}

// [JVM]
fun TestContext.windowResize(ref: String, sz: Vec2) = windowResize(TestRef(path = ref), sz)

// [JVM]
fun TestContext.windowResize(ref: ID, sz: Vec2) = windowResize(TestRef(ref), sz)

fun TestContext.windowResize(ref: TestRef, sz: Vec2) {
    if (isError) return

    val window = getWindowByRef(ref)!!
    val size = floor(sz)

    REGISTER_DEPTH {
        logDebug("WindowResize ${window.name} (%.1f,%.1f)", size.x, size.y)
        if ((size - window.size).lengthSqr < 0.001f)
            return

        windowBringToFront(window)
        windowCollapse(window, false)

        val id = window getResizeID 0
        mouseMove(id)
        mouseDown(0)

        val delta = size - window.size
        mouseMoveToPos(inputs!!.mousePosValue + delta)
        yield()

        mouseUp()
    }
}

fun TestContext.windowMoveToMakePosVisible(window: Window, pos: Vec2) {
    val g = uiContext!!
    if (isError) return

    val visibleR = Rect(0f, 0f, g.io.displaySize.x.f, g.io.displaySize.y.f)   // FIXME: Viewport
    if (pos !in visibleR) {
        // Fallback move window directly to make our item reachable with the mouse.
        val pad = g.fontSize
        val delta = Vec2(
                if (pos.x < visibleR.min.x) visibleR.min.x - pos.x + pad else if (pos.x > visibleR.max.x) visibleR.max.x - pos.x - pad else 0f,
                if (pos.y < visibleR.min.y) visibleR.min.y - pos.y + pad else if (pos.y > visibleR.max.y) visibleR.max.y - pos.y - pad else 0f)
        window.setPos(window.pos + delta, Cond.Always)
        logDebug("WindowMoveBypass ${window.name} delta (%.1f,%.1f)", delta.x, delta.y)
        yield()
    }
}

fun TestContext.windowBringToFront(window_: Window?, flags: TestOpFlags = TestOpFlag.None.i): Boolean {

    var window = window_
    val g = uiContext!!
    if (isError) return false

    if (window == null) {
        val windowId = getID("")
        window = ImGui.findWindowByID(windowId)!!
//        assert(window != null)
    }

    if (window !== g.navWindow)
        REGISTER_DEPTH {
            logDebug("BringWindowToFront->FocusWindow('${window.name}')")
            ImGui.focusWindow(window)
            yield()
            yield()
            //IM_CHECK(g.NavWindow == window);
        }
    else if (window.rootWindow !== g.windows.last().rootWindow)
        REGISTER_DEPTH {
            logDebug("BringWindowToDisplayFront('${window.name}') (window.back=${g.windows.last().name})")
            window.bringToDisplayFront()
            yield()
            yield()
        }

    // We cannot guarantee this will work 100%
    // Because merely hovering an item may e.g. open a window or change focus.
    // In particular this can be the case with MenuItem. So trying to Open a MenuItem may lead to its child opening while hovering,
    // causing this function to seemingly fail (even if the end goal was reached).
    val ret = window === g.navWindow
    if (!ret && flags hasnt TestOpFlag.NoError)
        logDebug("-- Expected focused window '${window.name}', but '${g.navWindow?.name ?: "<NULL>"}' got focus back.")

    return ret
}

fun TestContext.popupClose() {
    if (isError) return

    REGISTER_DEPTH {
        logDebug("PopupClose")
        ImGui.closePopupToLevel(0, true)    // FIXME
    }
}

// [JVM]
fun TestContext.getWindowByRef(ref: String): Window? = getWindowByRef(TestRef(path = ref))
fun TestContext.getWindowByRef(ref: TestRef): Window? {
    val windowId = getID(ref)
    return findWindowByID(windowId)
}

val TestContext.focusWindowRef: TestRef
    get() = TestRef(uiContext!!.navWindow?.id ?: 0)