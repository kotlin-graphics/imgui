package imgui.internal.sections

import glm_.has
import glm_.hasnt
import org.lwjgl.system.Platform


// To test io.KeyMods (which is a combination of individual fields io.KeyCtrl, io.KeyShift, io.KeyAlt set by user/backend)
enum class KeyMod(val i: KeyModFlags) {
    None(0),
    Ctrl(1 shl 0),
    Shift(1 shl 1),
    Alt(1 shl 2),

    /** Cmd/Super/Windows key */
    Super(1 shl 3);

    infix fun or(b: KeyMod): KeyModFlags = i or b.i
}

infix fun Int.or(k: KeyMod) = or(k.i)
infix fun Int.has(k: KeyMod): Boolean = has(k.i)
infix fun Int.hasnt(k: KeyMod): Boolean = hasnt(k.i)

typealias KeyModFlags = Int

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

    class Text(val char: Char) : InputEvent(Type.Char) {
        override val source: InputSource = InputSource.Keyboard
    }

    class AppFocused(val focused: Boolean) : InputEvent(Type.Focus) {
        override val source: InputSource = InputSource.None
    }

    enum class Type {
        None, MousePos, MouseWheel, MouseButton, Key, Char, Focus;

        companion object {
            val COUNT = values().size
        }
    }
}

// FIXME-NAV: Clarify/expose various repeat delay/rate
enum class InputReadMode { Down, Pressed, Released, Repeat, RepeatSlow, RepeatFast }