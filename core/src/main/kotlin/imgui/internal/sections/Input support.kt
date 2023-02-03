package imgui.internal.sections


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

typealias InputFlags = Int

// Flags for IsKeyPressedEx(). In upcoming feature this will be used more (and IsKeyPressedEx() renamed)
// Don't mistake with ImGuiInputTextFlags! (for ImGui::InputText() function)
enum class InputFlag(val i: InputFlags) {
    // Flags for IsKeyPressedEx()
    None                (0),
    Repeat              (1 shl 0),   // Return true on successive repeats. Default for legacy IsKeyPressed(). NOT Default for legacy IsMouseClicked(). MUST BE == 1.

    // Repeat rate
    RepeatRateDefault   (1 shl 1),   // Repeat rate: Regular (default)
    RepeatRateNavMove   (1 shl 2),   // Repeat rate: Fast
    RepeatRateNavTweak  (1 shl 3),   // Repeat rate: Faster
    RepeatRateMask_     (RepeatRateDefault or RepeatRateNavMove or RepeatRateNavTweak);

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