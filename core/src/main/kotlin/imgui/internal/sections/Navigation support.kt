package imgui.internal.sections

import imgui.internal.classes.Rect

//-----------------------------------------------------------------------------
// [SECTION] Navigation support
//-----------------------------------------------------------------------------

typealias ActivateFlags = Int

enum class ActivateFlag {
    None,
    /** Favor activation that requires keyboard text input (e.g. for Slider/Drag). Default if keyboard is available. */
    PreferInput,
    /** Favor activation for tweaking with arrows or gamepad (e.g. for Slider/Drag). Default if keyboard is not available. */
    PreferTweak,
    /** Request widget to preserve state if it can (e.g. InputText will try to preserve cursor/selection) */
    TryToPreserveState;

    val i: ActivateFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun and(b: ActivateFlag): ActivateFlags = i and b.i
    infix fun and(b: ActivateFlags): ActivateFlags = i and b
    infix fun or(b: ActivateFlag): ActivateFlags = i or b.i
    infix fun or(b: ActivateFlags): ActivateFlags = i or b
    infix fun xor(b: ActivateFlag): ActivateFlags = i xor b.i
    infix fun xor(b: ActivateFlags): ActivateFlags = i xor b
    infix fun wo(b: ActivateFlags): ActivateFlags = and(b.inv())
}

infix fun ActivateFlags.and(b: ActivateFlag): ActivateFlags = and(b.i)
infix fun ActivateFlags.or(b: ActivateFlag): ActivateFlags = or(b.i)
infix fun ActivateFlags.xor(b: ActivateFlag): ActivateFlags = xor(b.i)
infix fun ActivateFlags.has(b: ActivateFlag): Boolean = and(b.i) != 0
infix fun ActivateFlags.hasnt(b: ActivateFlag): Boolean = and(b.i) == 0
infix fun ActivateFlags.wo(b: ActivateFlag): ActivateFlags = and(b.i.inv())


typealias NavHighlightFlags = Int

enum class NavHighlightFlag {
    None, TypeDefault, TypeThin,

    /** Draw rectangular highlight if (g.NavId == id) _even_ when using the mouse. */
    AlwaysDraw,
    NoRounding;

    val i: NavHighlightFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun and(b: NavHighlightFlag): NavHighlightFlags = i and b.i
    infix fun and(b: NavHighlightFlags): NavHighlightFlags = i and b
    infix fun or(b: NavHighlightFlag): NavHighlightFlags = i or b.i
    infix fun or(b: NavHighlightFlags): NavHighlightFlags = i or b
    infix fun xor(b: NavHighlightFlag): NavHighlightFlags = i xor b.i
    infix fun xor(b: NavHighlightFlags): NavHighlightFlags = i xor b
    infix fun wo(b: NavHighlightFlags): NavHighlightFlags = and(b.inv())
}

infix fun NavHighlightFlags.and(b: NavHighlightFlag): NavHighlightFlags = and(b.i)
infix fun NavHighlightFlags.or(b: NavHighlightFlag): NavHighlightFlags = or(b.i)
infix fun NavHighlightFlags.xor(b: NavHighlightFlag): NavHighlightFlags = xor(b.i)
infix fun NavHighlightFlags.has(b: NavHighlightFlag): Boolean = and(b.i) != 0
infix fun NavHighlightFlags.hasnt(b: NavHighlightFlag): Boolean = and(b.i) == 0
infix fun NavHighlightFlags.wo(b: NavHighlightFlag): NavHighlightFlags = and(b.i.inv())


typealias NavDirSourceFlags = Int

enum class NavDirSourceFlag {
    None, Keyboard, PadDPad, PadLStick;

    val i: NavDirSourceFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun and(b: NavDirSourceFlag): NavDirSourceFlags = i and b.i
    infix fun and(b: NavDirSourceFlags): NavDirSourceFlags = i and b
    infix fun or(b: NavDirSourceFlag): NavDirSourceFlags = i or b.i
    infix fun or(b: NavDirSourceFlags): NavDirSourceFlags = i or b
    infix fun xor(b: NavDirSourceFlag): NavDirSourceFlags = i xor b.i
    infix fun xor(b: NavDirSourceFlags): NavDirSourceFlags = i xor b
    infix fun wo(b: NavDirSourceFlags): NavDirSourceFlags = and(b.inv())
}

infix fun NavDirSourceFlags.and(b: NavDirSourceFlag): NavDirSourceFlags = and(b.i)
infix fun NavDirSourceFlags.or(b: NavDirSourceFlag): NavDirSourceFlags = or(b.i)
infix fun NavDirSourceFlags.xor(b: NavDirSourceFlag): NavDirSourceFlags = xor(b.i)
infix fun NavDirSourceFlags.has(b: NavDirSourceFlag): Boolean = and(b.i) != 0
infix fun NavDirSourceFlags.hasnt(b: NavDirSourceFlag): Boolean = and(b.i) == 0
infix fun NavDirSourceFlags.wo(b: NavDirSourceFlag): NavDirSourceFlags = and(b.i.inv())


typealias NavMoveFlags = Int

enum class NavMoveFlag {
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
    ScrollToEdge,
    Forwarded,
    DebugNoResult;

    val i: NavMoveFlags = if (ordinal == 0) 0 else 1 shl (ordinal - 1)

    infix fun and(b: NavMoveFlag): NavMoveFlags = i and b.i
    infix fun and(b: NavMoveFlags): NavMoveFlags = i and b
    infix fun or(b: NavMoveFlag): NavMoveFlags = i or b.i
    infix fun or(b: NavMoveFlags): NavMoveFlags = i or b
    infix fun xor(b: NavMoveFlag): NavMoveFlags = i xor b.i
    infix fun xor(b: NavMoveFlags): NavMoveFlags = i xor b
    infix fun wo(b: NavMoveFlags): NavMoveFlags = and(b.inv())
}

infix fun NavMoveFlags.and(b: NavMoveFlag): NavMoveFlags = and(b.i)
infix fun NavMoveFlags.or(b: NavMoveFlag): NavMoveFlags = or(b.i)
infix fun NavMoveFlags.xor(b: NavMoveFlag): NavMoveFlags = xor(b.i)
infix fun NavMoveFlags.has(b: NavMoveFlag): Boolean = and(b.i) != 0
infix fun NavMoveFlags.hasnt(b: NavMoveFlag): Boolean = and(b.i) == 0
infix fun NavMoveFlags.wo(b: NavMoveFlag): NavMoveFlags = and(b.i.inv())
operator fun NavMoveFlags.minus(flag: NavMoveFlag): NavMoveFlags = wo(flag)
operator fun NavMoveFlags.div(flag: NavMoveFlag): NavMoveFlags = or(flag)


enum class NavForward(val i: Int) {
    ScrollToEdge(1 shl 6),
    Forwarded(1 shl 7)
}

enum class NavLayer {
    /** Main scrolling layer */
    Main,

    /** Menu layer (access with Alt/ImGuiNavInput_Menu) */
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