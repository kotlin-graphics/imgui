package imgui.internal.sections

import com.livefront.sealedenum.GenSealedEnum
import imgui.Flag
import imgui.FlagBase
import imgui.div
import imgui.internal.classes.Rect
import imgui.or

//-----------------------------------------------------------------------------
// [SECTION] Navigation support
//-----------------------------------------------------------------------------

typealias ActivateFlags = Flag<ActivateFlag>

sealed class ActivateFlag : FlagBase<ActivateFlag>() {

    /** Favor activation that requires keyboard text input (e.g. for Slider/Drag). Default for Enter key. */
    object PreferInput : ActivateFlag()

    /** Favor activation for tweaking with arrows or gamepad (e.g. for Slider/Drag). Default for Space key and if keyboard is not used. */
    object PreferTweak : ActivateFlag()

    /** Request widget to preserve state if it can (e.g. InputText will try to preserve cursor/selection) */
    object TryToPreserveState : ActivateFlag()

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object
}

typealias ScrollFlags = Flag<ScrollFlag>

sealed class ScrollFlag : FlagBase<ScrollFlag>() {

    /** If item is not visible: scroll as little as possible on X axis to bring item back into view [default for X axis] */
    object KeepVisibleEdgeX : ScrollFlag()

    /** If item is not visible: scroll as little as possible on Y axis to bring item back into view [default for Y axis for windows that are already visible] */
    object KeepVisibleEdgeY : ScrollFlag()

    /** If item is not visible: scroll to make the item centered on X axis [rarely used] */
    object KeepVisibleCenterX : ScrollFlag()

    /** If item is not visible: scroll to make the item centered on Y axis */
    object KeepVisibleCenterY : ScrollFlag()

    /** Always center the result item on X axis [rarely used] */
    object AlwaysCenterX : ScrollFlag()

    /** Always center the result item on Y axis [default for Y axis for appearing window) */
    object AlwaysCenterY : ScrollFlag()

    /** Disable forwarding scrolling to parent window if required to keep item/rect visible (only scroll window the function was applied to). */
    object NoScrollParent : ScrollFlag()

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object {
        val MaskX get() = KeepVisibleEdgeX or KeepVisibleCenterX or AlwaysCenterX
        val MaskY get() = KeepVisibleEdgeY or KeepVisibleCenterY or AlwaysCenterY
    }
}


typealias NavHighlightFlags = Flag<NavHighlightFlag>

sealed class NavHighlightFlag : FlagBase<NavHighlightFlag>() {
    object TypeDefault : NavHighlightFlag()
    object TypeThin : NavHighlightFlag()

    /** Draw rectangular highlight if (g.NavId == id) _even_ when using the mouse. */
    object AlwaysDraw : NavHighlightFlag()

    object NoRounding : NavHighlightFlag()

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object
}

typealias NavMoveFlags = Flag<NavMoveFlag>

sealed class NavMoveFlag : FlagBase<NavMoveFlag>() {

    /** On failed request, restart from opposite side */
    object LoopX : NavMoveFlag()

    object LoopY : NavMoveFlag()

    /** On failed request, request from opposite side one line down (when NavDir==right) or one line up (when NavDir==left) */
    object WrapX : NavMoveFlag()

    /** This is not super useful for provided but completeness */
    object WrapY : NavMoveFlag()

    /** Allow scoring and considering the current NavId as a move target candidate.
     *  This is used when the move source is offset (e.g. pressing PageDown actually needs to send a Up move request,
     *  if we are pressing PageDown from the bottom-most item we need to stay in place) */
    object AllowCurrentNavId : NavMoveFlag()

    /** Store alternate result in NavMoveResultLocalVisible that only comprise elements that are already fully visible (used by PageUp/PageDown) */
    object AlsoScoreVisibleSet : NavMoveFlag()

    /** Force scrolling to min/max (used by Home/End) // FIXME-NAV: Aim to remove or reword, probably unnecessary */
    object ScrollToEdgeY : NavMoveFlag()

    object Forwarded : NavMoveFlag()

    /** Dummy scoring for debug purpose, don't apply result */
    object DebugNoResult : NavMoveFlag()

    object FocusApi : NavMoveFlag()

    /** == Focus + Activate if item is Inputable + DontChangeNavHighlight */
    object Tabbing : NavMoveFlag()

    object Activate : NavMoveFlag()

    /** Do not alter the visible state of keyboard vs mouse nav highlight */
    object DontSetNavHighlight : NavMoveFlag()

    override val i: Int = 1 shl ordinal

    @GenSealedEnum
    companion object {
        val WrapMask_ get() = LoopX / LoopY / WrapX / WrapY
    }
}

enum class NavForward(val i: Int) {
    ScrollToEdge(1 shl 6),
    Forwarded(1 shl 7)
}

enum class NavLayer {
    /** Main scrolling layer */
    Main,

    /** Menu layer (access with Alt) */
    Menu;

    infix fun xor(int: Int): Int = ordinal xor int

    companion object {
        val COUNT = values().size
        infix fun of(i: Int) = values().first { it.ordinal == i }
    }
}

operator fun Array<Rect>.get(index: NavLayer): Rect = get(index.ordinal)
operator fun Array<Rect>.set(index: NavLayer, rect: Rect) = set(index.ordinal, rect)
operator fun IntArray.get(index: NavLayer): Int = get(index.ordinal)
operator fun IntArray.set(index: NavLayer, int: Int) = set(index.ordinal, int)
infix fun Int.shl(layer: NavLayer): Int = shl(layer.ordinal)