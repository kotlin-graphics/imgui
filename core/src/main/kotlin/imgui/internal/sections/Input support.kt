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

// FIXME-NAV: Clarify/expose various repeat delay/rate
enum class NavReadMode { Down, Repeat, RepeatSlow, RepeatFast }