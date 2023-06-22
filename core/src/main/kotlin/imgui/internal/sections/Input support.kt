package imgui.internal.sections

import imgui.*


enum class InputSource {
    None, Mouse, // Note: may be Mouse or TouchScreen or Pen. See io.MouseSource to distinguish them.
    Keyboard, Gamepad,

    /** Currently only used by InputText() */
    Clipboard
}

// FIXME: Structures in the union below need to be declared as anonymous unions appears to be an extension?
// Using ImVec2() would fail on Clang 'union member 'MousePos' has a non-trivial default constructor'

sealed class InputEvent(val eventId: UInt) { // Unique, sequential increasing integer to identify an event (if you need to correlate them to other data).

    abstract val source: InputSource
    var addedByTestEngine = false

    class MousePos(val posX: Float,
                   val posY: Float,
                   val mouseSource: MouseSource, eventId: UInt) : InputEvent(eventId) {
        override val source: InputSource = InputSource.Mouse
    }

    class MouseWheel(val wheelX: Float,
                     val wheelY: Float,
                     val mouseSource: MouseSource, eventId: UInt) : InputEvent(eventId) {
        override val source: InputSource = InputSource.Mouse
    }

    class MouseButton(val button: imgui.MouseButton,
                      val down: Boolean,
                      val mouseSource: MouseSource, eventId: UInt) : InputEvent(eventId) {
        override val source: InputSource = InputSource.Mouse
    }

    class Key(val key: imgui.Key,
              val down: Boolean,
              val analogValue: Float,
              override val source: InputSource = InputSource.Keyboard, eventId: UInt) : InputEvent(eventId)

    class Text(val char: Char, eventId: UInt) : InputEvent(eventId) {
        override val source: InputSource = InputSource.Keyboard
    }

    class AppFocused(val focused: Boolean, eventId: UInt) : InputEvent(eventId) {
        override val source: InputSource = InputSource.None
    }
}

// Input function taking an 'ImGuiID owner_id' argument defaults to (ImGuiKeyOwner_Any == 0) aka don't test ownership, which matches legacy behavior.

/** Accept key that have an owner, UNLESS a call to SetKeyOwner() explicitly used ImGuiInputFlags_LockThisFrame or ImGuiInputFlags_LockUntilRelease. */
const val KeyOwner_Any: ID = 0

/** Require key to have no owner. */
const val KeyOwner_None: ID = -1

typealias KeyRoutingIndex = Int

// Routing table entry (sizeof() == 16 bytes)
class KeyRoutingData {
    var nextEntryIndex: KeyRoutingIndex = -1

    /** Technically we'd only need 4-bits but for simplify we store ImGuiMod_ values which need 16-bits. ImGuiMod_Shortcut is already translated to Ctrl/Super. */
    var mods: KeyChord = none
    var routingNextScore = 255               // Lower is better (0: perfect score)
    var routingCurr: ID = KeyOwner_None
    var routingNext: ID = KeyOwner_None
}

// Routing table: maintain a desired owner for each possible key-chord (key + mods), and setup owner in NewFrame() when mods are matching.
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

/** This extends ImGuiKeyData but only for named keys (legacy keys don't support the new features)
 *  Stored in main context (1 per named key). In the future it might be merged into ImGuiKeyData. */
class KeyOwnerData {
    var ownerCurr = KeyOwner_None
    var ownerNext = KeyOwner_None

    /** Reading this key requires explicit owner id (until end of frame). Set by ImGuiInputFlags_LockThisFrame. */
    var lockThisFrame = false

    /** Reading this key requires explicit owner id (until key is released). Set by ImGuiInputFlags_LockUntilRelease. When this is true LockThisFrame is always true as well. */
    var lockUntilRelease = false
}