package imgui.internal.sections

import imgui.Flag
import imgui.internal.classes.Rect
import imgui.or

//-----------------------------------------------------------------------------
// [SECTION] Navigation support
//-----------------------------------------------------------------------------

typealias ActivateFlags = Int

enum class ActivateFlag : Flag<ActivateFlag> {
  None,

  /** Favor activation that requires keyboard text input (e.g. for Slider/Drag). Default if keyboard is available. */
  PreferInput,

  /** Favor activation for tweaking with arrows or gamepad (e.g. for Slider/Drag). Default if keyboard is not available. */
  PreferTweak,

  /** Request widget to preserve state if it can (e.g. InputText will try to preserve cursor/selection) */
  TryToPreserveState;

  override val i: ActivateFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)
}

typealias ScrollFlags = Int

enum class ScrollFlag(override val i: ScrollFlags) : Flag<ScrollFlag> {
  None(0),

  /** If item is not visible: scroll as little as possible on X axis to bring item back into view [default for X axis] */
  KeepVisibleEdgeX(1 shl 0),

  /** If item is not visible: scroll as little as possible on Y axis to bring item back into view [default for Y axis for windows that are already visible] */
  KeepVisibleEdgeY(1 shl 1),

  /** If item is not visible: scroll to make the item centered on X axis [rarely used] */
  KeepVisibleCenterX(1 shl 2),

    /** If item is not visible: scroll to make the item centered on Y axis */
    KeepVisibleCenterY(1 shl 3),

    /** Always center the result item on X axis [rarely used] */
    AlwaysCenterX(1 shl 4),

    /** Always center the result item on Y axis [default for Y axis for appearing window) */
    AlwaysCenterY(1 shl 5),

    /** Disable forwarding scrolling to parent window if required to keep item/rect visible (only scroll window the function was applied to). */
    NoScrollParent(1 shl 6),
  MaskX_(KeepVisibleEdgeX or KeepVisibleCenterX or AlwaysCenterX),
  MaskY_(KeepVisibleEdgeY or KeepVisibleCenterY or AlwaysCenterY)
}


typealias NavHighlightFlags = Int

enum class NavHighlightFlag : Flag<NavHighlightFlag> {
  None, TypeDefault, TypeThin,

  /** Draw rectangular highlight if (g.NavId == id) _even_ when using the mouse. */
  AlwaysDraw,
  NoRounding;

  override val i: NavHighlightFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)
}

typealias NavMoveFlags = Int

enum class NavMoveFlag : Flag<NavMoveFlag> {
  None,

  /** On failed request, restart from opposite side */
  LoopX,
  LoopY,

  /** On failed request, request from opposite side one line down (when NavDir==right) or one line up (when NavDir==left) */
  WrapX,

  /** This is not super useful for provided but completeness */
    WrapY,

    /** Allow scoring and considering the current NavId as a move target candidate.
     *  This is used when the move source is offset (e.g. pressing PageDown actually needs to send a Up move request,
     *  if we are pressing PageDown from the bottom-most item we need to stay in place) */
    AllowCurrentNavId,

    /** Store alternate result in NavMoveResultLocalVisible that only comprise elements that are already fully visible (used by PageUp/PageDown) */
    AlsoScoreVisibleSet,

    /** Force scrolling to min/max (used by Home/End) // FIXME-NAV: Aim to remove or reword, probably unnecessary */
    ScrollToEdgeY,
    Forwarded,

    /** Dummy scoring for debug purpose, don't apply result */
    DebugNoResult,
    FocusApi,
    /** == Focus + Activate if item is Inputable + DontChangeNavHighlight */
    Tabbing,
    Activate,
    /** Do not alter the visible state of keyboard vs mouse nav highlight */
    DontSetNavHighlight;

  override val i: NavMoveFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)
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