package imgui

import glm_.vec2.Vec2
import imgui.classes.DrawList
import imgui.internal.sections.ViewportP

//-----------------------------------------------------------------------------
// [SECTION] Viewports
//-----------------------------------------------------------------------------

typealias ViewportFlags = Int

/** Flags stored in ImGuiViewport::Flags */
enum class ViewportFlag(val i: ViewportFlags) {
    None(0),
    /** Represent a Platform Window */
    IsPlatformWindow(1 shl 0),
    /** Represent a Platform Monitor (unused yet) */
    IsPlatformMonitor(1 shl 1),
    /** Platform Window: is created/managed by the application (rather than a dear imgui backend) */
    OwnedByApp(1 shl 2);

    infix fun and(b: ViewportFlag): ViewportFlags = i and b.i
    infix fun and(b: ViewportFlags): ViewportFlags = i and b
    infix fun or(b: ViewportFlag): ViewportFlags = i or b.i
    infix fun or(b: ViewportFlags): ViewportFlags = i or b
    infix fun xor(b: ViewportFlag): ViewportFlags = i xor b.i
    infix fun xor(b: ViewportFlags): ViewportFlags = i xor b
    infix fun wo(b: ViewportFlag): ViewportFlags = and(b.i.inv())
    infix fun wo(b: ViewportFlags): ViewportFlags = and(b.inv())
}

infix fun ViewportFlags.and(b: ViewportFlag): ViewportFlags = and(b.i)
infix fun ViewportFlags.or(b: ViewportFlag): ViewportFlags = or(b.i)
infix fun ViewportFlags.xor(b: ViewportFlag): ViewportFlags = xor(b.i)
infix fun ViewportFlags.has(b: ViewportFlag): Boolean = and(b.i) != 0
infix fun ViewportFlags.hasnt(b: ViewportFlag): Boolean = and(b.i) == 0
infix fun ViewportFlags.wo(b: ViewportFlag): ViewportFlags = and(b.i.inv())

// - Currently represents the Platform Window created by the application which is hosting our Dear ImGui windows.
// - In 'docking' branch with multi-viewport enabled, we extend this concept to have multiple active viewports.
// - In the future we will extend this concept further to also represent Platform Monitor and support a "no main platform window" operation mode.
// - About Main Area vs Work Area:
//   - Main Area = entire viewport.
//   - Work Area = entire viewport minus sections used by main menu bars (for platform windows), or by task bar (for platform monitor).
//   - Windows are generally trying to stay within the Work Area of their host viewport.
open class Viewport {
    /** See ImGuiViewportFlags_ */
    var flags: ViewportFlags = 0

    /** Main Area: Position of the viewport (Dear ImGui coordinates are the same as OS desktop/native coordinates) */
    val pos = Vec2()

    /** Main Area: Size of the viewport. */
    val size = Vec2()

    /** Work Area: Position of the viewport minus task bars, menus bars, status bars (>= Pos) */
    val workPos = Vec2()

    /** Work Area: Size of the viewport minus task bars, menu bars, status bars (<= Size) */
    val workSize = Vec2()
    // Helpers
    val center: Vec2
        get() = Vec2(pos.x + size.x * 0.5f, pos.y + size.y * 0.5f)

    val workCenter: Vec2
        get() = Vec2(workPos.x + workSize.x * 0.5f, workPos.y + workSize.y * 0.5f)

    /** !GetBackgroundDrawList(ImGuiViewport* viewport)
     *  get background draw list for the given viewport. this draw list will be the first rendering one. Useful to quickly draw shapes/text behind dear imgui contents.
     */
    val backgroundDrawList: DrawList
        get() = (this as ViewportP).getDrawList(0, "##Background")

    /** get foreground draw list for the given viewport. this draw list will be the last rendered one. Useful to quickly draw shapes/text over dear imgui contents. */
    val foregroundDrawList: DrawList
        get() = (this as ViewportP).getDrawList(1, "##Foreground")
}