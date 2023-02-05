package imgui.internal.sections

import imgui.*


enum class InputSource {
    None, Mouse, Keyboard, Gamepad,

    /** Currently only used by InputText() */
    Clipboard,

    /** Stored in g.ActiveIdSource only */
    Nav
}

// FIXME: Structures in the union below need to be declared as anonymous unions appears to be an extension?
// Using ImVec2() would fail on Clang 'union member 'MousePos' has a non-trivial default constructor'

sealed class InputEvent {
    abstract val source: InputSource
    var addedByTestEngine = false

    class MousePos(val posX: Float,
                   val posY: Float) : InputEvent() {
        override val source: InputSource = InputSource.Mouse
    }

    class MouseWheel(val wheelX: Float,
                     val wheelY: Float) : InputEvent() {
        override val source: InputSource = InputSource.Mouse
    }

    class MouseButton(val button: Int,
                      val down: Boolean) : InputEvent() {
        override val source: InputSource = InputSource.Mouse
    }

    class Key(val key: imgui.Key,
              val down: Boolean,
              val analogValue: Float,
              override val source: InputSource = InputSource.Keyboard) : InputEvent()

    class Text(val char: Char) : InputEvent() {
        override val source: InputSource = InputSource.Keyboard
    }

    class AppFocused(val focused: Boolean) : InputEvent() {
        override val source: InputSource = InputSource.None
    }
}

// Input function taking an 'ImGuiID owner_id' argument defaults to (ImGuiKeyOwner_Any == 0) aka don't test ownership, which matches legacy behavior.

/** Accept key that have an owner, UNLESS a call to SetKeyOwner() explicitely used ImGuiInputFlags_LockThisFrame or ImGuiInputFlags_LockUntilRelease. */
const val KeyOwner_Any: ID = 0

/** Require key to have no owner. */
const val KeyOwner_None: ID = -1

typealias KeyRoutingIndex = Int

// Routing table entry (sizeof() == 16 bytes)
class KeyRoutingData {
    var nextEntryIndex: KeyRoutingIndex = -1
    var mods = 0
    var routingNextScore = 255               // Lower is better (0: perfect score)
    var routingCurr: ID = KeyOwner_None
    var routingNext: ID = KeyOwner_None
}

// Routing table maintain a desired owner for each possible key-chord (key + mods), and setup owner in NewFrame() when mods are matching.
// Stored in main context (1 instance)
class KeyRoutingTable {
    val index = IntArray(Key.COUNT) { -1 } // Index of first entry in Entries[]
    val entries = ArrayList<KeyRoutingData>()
    val entriesNext = ArrayList<KeyRoutingData>() // Double-buffer to avoid reallocation (could use a shared buffer)

    fun clear() {
        index.fill(-1)
        entries.clear()
        entriesNext.clear()
    }
}

/** This extend ImGuiKeyData but only for named keys (legacy keys don't support the new features)
 *  Stored in main context (1 per named key). In the future might be merged into ImGuiKeyData. */
class KeyOwnerData {
    var ownerCurr = KeyOwner_None
    var ownerNext = KeyOwner_None

    /** Reading this key requires explicit owner id (until end of frame). Set by ImGuiInputFlags_LockThisFrame. */
    var lockThisFrame = false

    /** Reading this key requires explicit owner id (until key is released). Set by ImGuiInputFlags_LockUntilRelease. When this is true LockThisFrame is always true as well. */
    var lockUntilRelease = false
}

/** -> enum ImGuiInputFlags_         // Flags: for IsKeyPressed(), IsMouseClicked(), SetKeyOwner(), SetItemKeyOwner() etc. */
typealias InputFlags = Int

/** Flags for extended versions of IsKeyPressed(), IsMouseClicked(), Shortcut(), SetKeyOwner(), SetItemKeyOwner()
 *  Don't mistake with ImGuiInputTextFlags! (for ImGui::InputText() function) */
enum class InputFlag(val i: InputFlags) {
    /** Flags for IsKeyPressed(), IsMouseClicked(), Shortcut() */
    None(0),
    Repeat(1 shl 0),   // Return true on successive repeats. Default for legacy IsKeyPressed(). NOT Default for legacy IsMouseClicked(). MUST BE == 1.

    // Repeat rate
    RepeatRateDefault(1 shl 1),   // Repeat rate: Regular (default)
    RepeatRateNavMove(1 shl 2),   // Repeat rate: Fast
    RepeatRateNavTweak(1 shl 3),   // Repeat rate: Faster
    RepeatRateMask_(RepeatRateDefault or RepeatRateNavMove or RepeatRateNavTweak),


    // Flags for SetItemKeyOwner()
    CondHovered(1 shl 4),   // Only set if item is hovered (default to both)
    CondActive(1 shl 5),   // Only set if item is active (default to both)
    CondDefault_(CondHovered or CondActive),
    CondMask_(CondHovered or CondActive),

    // Flags for SetKeyOwner(), SetItemKeyOwner()

    /** Access to key data will requires EXPLICIT owner ID (ImGuiKeyOwner_Any/0 will NOT accepted for polling). Cleared at end of frame. This is useful to make input-owner-aware code steal keys from non-input-owner-aware code. */
    LockThisFrame(1 shl 6),

    /** Access to key data will requires EXPLICIT owner ID (ImGuiKeyOwner_Any/0 will NOT accepted for polling). Cleared when key is released or at end of frame is not down. This is useful to make input-owner-aware code steal keys from non-input-owner-aware code. */
    LockUntilRelease(1 shl 7),

    // Routing policies for Shortcut(), SetShortcutRouting()
    // - When a policy is set, Shortcut() will register itself with SetShortcutRouting(),
    //   allowing the system to decide where to route the input among other route-aware calls.
    //   The general idea is that several callers register a shortcut, and only one gets it.
    // - Routing is NOT registered by default, meaning that a simple Shortcut() call
    //   will see all inputs, won't have any side-effect and won't interfere with other inputs.
    // - Priorities (highest-to-lowest): GlobalHigh > Focused (when active item) > Global > Focused (when focused window) > GlobalLow.
    // - Can select only 1 policy among all available.

    /** Do not register route (provided for completeness but technically zero-value) */
    RouteNone(0),

    /** Register route if focused: Accept inputs if window is in focus stack. Deep-most focused window takes inputs. ActiveId takes inputs over deep-most focused window. */
    RouteFocused(1 shl 8),

    /** Register route globally (lowest priority: unless a focused window or active item registered the route) -> recommended Global priority. */
    RouteGlobalLow(1 shl 9),

    /** Register route globally (medium priority: unless an active item registered the route, e.g. CTRL+A registered by InputText). */
    RouteGlobal(1 shl 10),

    /** Register route globally (highest priority: unlikely you need to use that: will interfere with every active items) */
    RouteGlobalHigh(1 shl 11),
    RouteMask_(RouteFocused or RouteGlobal or RouteGlobalLow or RouteGlobalHigh),

    /** Global routes will not be applied if underlying background/void is focused (== no Dear ImGui windows are focused). Useful for overlay applications. */
    RouteUnlessBgFocused(1 shl 12),

    // [Internal] Mask of which function support which flags
    SupportedByIsKeyPressed(Repeat or RepeatRateMask_),
    SupportedByShortcut(Repeat or RepeatRateMask_ or RouteMask_ or RouteUnlessBgFocused),
    SupportedBySetKeyOwner(LockThisFrame or LockUntilRelease),
    SupportedBySetItemKeyOwner(SupportedBySetKeyOwner or CondMask_);

    infix fun and(b: InputFlag): InputFlags = i and b.i
    infix fun and(b: InputFlags): InputFlags = i and b
    infix fun or(b: InputFlag): InputFlags = i or b.i
    infix fun or(b: InputFlags): InputFlags = i or b
    infix fun xor(b: InputFlag): InputFlags = i xor b.i
    infix fun xor(b: InputFlags): InputFlags = i xor b
    infix fun wo(b: InputFlags): InputFlags = and(b.inv())
}

infix fun InputFlags.and(b: InputFlag): InputFlags = and(b.i)
infix fun InputFlags.or(b: InputFlag): InputFlags = or(b.i)
infix fun InputFlags.xor(b: InputFlag): InputFlags = xor(b.i)
infix fun InputFlags.has(b: InputFlag): Boolean = and(b.i) != 0
infix fun InputFlags.hasnt(b: InputFlag): Boolean = and(b.i) == 0
infix fun InputFlags.wo(b: InputFlag): InputFlags = and(b.i.inv())
operator fun InputFlags.minus(flag: InputFlag): InputFlags = wo(flag)
operator fun InputFlags.div(flag: InputFlag): InputFlags = or(flag)