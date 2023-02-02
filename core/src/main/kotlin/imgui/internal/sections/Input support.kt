package imgui.internal.sections

import imgui.shl


enum class InputSource {
    None, Mouse, Keyboard, Gamepad,

    /** Currently only used by InputText() */
    Clipboard,

    /** Stored in g.ActiveIdSource only */
    Nav
}

// FIXME: Structures in the union below need to be declared as anonymous unions appears to be an extension?
// Using ImVec2() would fail on Clang 'union member 'MousePos' has a non-trivial default constructor'

sealed class InputEvent(val type: Type) {
    abstract val source: InputSource
    var addedByTestEngine = false

    class MousePos(val posX: Float, val posY: Float) : InputEvent(Type.MousePos) {
        override val source: InputSource = InputSource.Mouse
    }

    class MouseWheel(val wheelX: Float, val wheelY: Float) : InputEvent(Type.MouseWheel) {
        override val source: InputSource = InputSource.Mouse
    }

    class MouseButton(val button: Int, val down: Boolean) : InputEvent(Type.MouseButton) {
        override val source: InputSource = InputSource.Mouse
    }

    class Key(val key: imgui.Key, val down: Boolean, val analogValue: Float,
              override val source: InputSource = InputSource.Keyboard) : InputEvent(Type.Key)

    class Text(val char: Char) : InputEvent(Type.Text) {
        override val source: InputSource = InputSource.Keyboard
    }

    class AppFocused(val focused: Boolean) : InputEvent(Type.Focus) {
        override val source: InputSource = InputSource.None
    }

    enum class Type {
        None, MousePos, MouseWheel, MouseButton, Key, Text, Focus;

        companion object {
            val COUNT = values().size
        }
    }
}

typealias InputReadFlags = Int

// Flags for IsKeyPressedEx(). In upcoming feature this will be used more (and IsKeyPressedEx() renamed)
// Don't mistake with ImGuiInputTextFlags! (for ImGui::InputText() function)
enum class InputReadFlag(val i: InputReadFlags) {
    // Flags for IsKeyPressedEx()
    None                (0),
    Repeat              (1 shl 0),   // Return true on successive repeats. Default for legacy IsKeyPressed(). NOT Default for legacy IsMouseClicked(). MUST BE == 1.

    // Repeat rate
    RepeatRateDefault   (1 shl 1),   // Regular
    RepeatRateNavMove   (1 shl 2),   // Fast
    RepeatRateNavTweak  (1 shl 3),   // Faster
    RepeatRateMask_     (RepeatRateDefault or RepeatRateNavMove or RepeatRateNavTweak);

    infix fun and(b: InputReadFlag): InputReadFlags = i and b.i
    infix fun and(b: InputReadFlags): InputReadFlags = i and b
    infix fun or(b: InputReadFlag): InputReadFlags = i or b.i
    infix fun or(b: InputReadFlags): InputReadFlags = i or b
    infix fun xor(b: InputReadFlag): InputReadFlags = i xor b.i
    infix fun xor(b: InputReadFlags): InputReadFlags = i xor b
    infix fun wo(b: InputReadFlags): InputReadFlags = and(b.inv())
}

infix fun InputReadFlags.and(b: InputReadFlag): InputReadFlags = and(b.i)
infix fun InputReadFlags.or(b: InputReadFlag): InputReadFlags = or(b.i)
infix fun InputReadFlags.xor(b: InputReadFlag): InputReadFlags = xor(b.i)
infix fun InputReadFlags.has(b: InputReadFlag): Boolean = and(b.i) != 0
infix fun InputReadFlags.hasnt(b: InputReadFlag): Boolean = and(b.i) == 0
infix fun InputReadFlags.wo(b: InputReadFlag): InputReadFlags = and(b.i.inv())