package imgui

import com.livefront.sealedenum.GenSealedEnum
import glm_.vec2.Vec2
import imgui.classes.DrawList
import imgui.internal.sections.ViewportP

//-----------------------------------------------------------------------------
// [SECTION] Viewports
//-----------------------------------------------------------------------------

typealias ViewportFlags = Flag<ViewportFlag>

/** Flags stored in ImGuiViewport::Flags, giving indications to the platform backends. */
sealed class ViewportFlag : FlagBase<ViewportFlag>() {
    /** Represent a Platform Window */
    object IsPlatformWindow : ViewportFlag()

    /** Represent a Platform Monitor (unused yet) */
    object IsPlatformMonitor : ViewportFlag()

    /** Platform Window: is created/managed by the application (rather than a dear imgui backend) */
    object OwnedByApp : ViewportFlag()

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object
}

// - Currently represents the Platform Window created by the application which is hosting our Dear ImGui windows.
// - In 'docking' branch with multi-viewport enabled, we extend this concept to have multiple active viewports.
// - In the future we will extend this concept further to also represent Platform Monitor and support a "no main platform window" operation mode.
// - About Main Area vs Work Area:
//   - Main Area = entire viewport.
//   - Work Area = entire viewport minus sections used by main menu bars (for platform windows), or by task bar (for platform monitor).
//   - Windows are generally trying to stay within the Work Area of their host viewport.
open class Viewport {
    /** See ImGuiViewportFlags_ */
    var flags: ViewportFlags = none

    /** Main Area: Position of the viewport (Dear ImGui coordinates are the same as OS desktop/native coordinates) */
    val pos = Vec2()

    /** Main Area: Size of the viewport. */
    val size = Vec2()

    /** Work Area: Position of the viewport minus task bars, menus bars, status bars (>= Pos) */
    val workPos = Vec2()

    /** Work Area: Size of the viewport minus task bars, menu bars, status bars (<= Size) */
    val workSize = Vec2()

    /** Platform/Backend Dependent Data
     *
     *  void* to hold lower-level, platform-native window handle (under Win32 this is expected to be a HWND, unused for other platforms) */
    var platformHandleRaw: Any? = null

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

//-----------------------------------------------------------------------------
// [SECTION] Platform Dependent Interfaces
//-----------------------------------------------------------------------------

/** (Optional) Support for IME (Input Method Editor) via the io.SetPlatformImeDataFn() function. */
data class PlatformImeData(
    /** A widget wants the IME to be visible */
    var wantVisible: Boolean = false,
    /** Position of the input cursor */
    val inputPos: Vec2 = Vec2(),
    /** Line height */
    var inputLineHeight: Float = 0f) {

    constructor(data: PlatformImeData) : this(data.wantVisible, Vec2(data.inputPos), data.inputLineHeight)
}