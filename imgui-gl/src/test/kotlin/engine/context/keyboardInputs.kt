package engine.context

import engine.KeyModFlag
import engine.KeyModFlags
import engine.KeyState
import engine.core.TestInput
import engine.getKeyModsPrefixStr
import glm_.b
import glm_.c
import imgui.Key
import imgui.cStr
import imgui.internal.textCharFromUtf8

fun TestContext.keyDownMap(key: Key, modFlags: KeyModFlags = KeyModFlag.None.i) {

    if (isError) return

    REGISTER_DEPTH {
        val modFlagsStr = getKeyModsPrefixStr(modFlags)
        logDebug("KeyDownMap($modFlagsStr${if (key != Key.Count) key.name else ""})")
        inputs!!.queue += TestInput.fromKey(key, KeyState.Down, modFlags)
        yield()
        yield()
    }
}

fun TestContext.keyUpMap(key: Key, modFlags: KeyModFlags = KeyModFlag.None.i) {

    if (isError) return

    REGISTER_DEPTH {
        val modFlagsStr = getKeyModsPrefixStr(modFlags)
        logDebug("KeyUpMap($modFlagsStr${if (key != Key.Count) key.name else ""})")
        inputs!!.queue += TestInput.fromKey(key, KeyState.Up, modFlags)
        yield()
        yield()
    }
}

fun TestContext.keyPressMap(key: Key, modFlags: KeyModFlags = KeyModFlag.None.i, count_: Int = 1) {

    if (isError) return

    var count = count_
    REGISTER_DEPTH {
        val modFlagsStr = getKeyModsPrefixStr(modFlags)
        logDebug("KeyPressMap($modFlagsStr${if (key != Key.Count) key.name else ""}, $count)")
        while (count > 0) {
            count--
            inputs!!.queue += TestInput.fromKey(key, KeyState.Down, modFlags)
            yield()
            inputs!!.queue += TestInput.fromKey(key, KeyState.Up, modFlags)
            yield()

            // Give a frame for items to react
            yield()
        }
    }
}

// [JVM]
fun TestContext.keyChars(string: String) = keyChars(string.toByteArray())

fun TestContext.keyChars(chars: ByteArray) {

    if (isError) return

    REGISTER_DEPTH {
        logDebug("KeyChars('${chars.cStr}')")
        var p = 0
        while (p < chars.size && chars[p] != 0.b) {
            val (c, bytesCount) = textCharFromUtf8(chars, p)
            p += bytesCount
            if (c in 1..0xFFFF)
                inputs!!.queue += TestInput.fromChar(c.c)

            if (!engineIO!!.configRunFast)
                sleep(1f / engineIO!!.typingSpeed)
        }
        yield()
    }
}

// [JVM]
fun TestContext.keyCharsAppend(string: String) = keyCharsAppend(string.toByteArray())

fun TestContext.keyCharsAppend(chars: ByteArray) {

    if (isError) return

    REGISTER_DEPTH {
        logDebug("KeyCharsAppend('${chars.cStr}')")
        keyPressMap(Key.End)
        keyChars(chars)
    }
}

// [JVM]
fun TestContext.keyCharsAppendEnter(string: String) = keyCharsAppendEnter(string.toByteArray())

fun TestContext.keyCharsAppendEnter(chars: ByteArray) {

    if (isError) return

    REGISTER_DEPTH {
        logDebug("KeyCharsAppendEnter('${chars.cStr}')")
        keyPressMap(Key.End)
        keyChars(chars)
        keyPressMap(Key.Enter)
    }
}

// [JVM]
fun TestContext.keyCharsReplace(string: String) = keyCharsReplace(string.toByteArray())

fun TestContext.keyCharsReplace(chars: ByteArray) {

    if (isError) return

    REGISTER_DEPTH {
        logDebug("KeyCharsReplace('${chars.cStr}')")
        keyPressMap(Key.A, KeyModFlag.Ctrl.i)
        keyPressMap(Key.Delete)
        keyChars(chars)
    }
}

fun TestContext.keyCharsReplaceEnter(chars: ByteArray) {

    if (isError) return

    REGISTER_DEPTH {
        logDebug("KeyCharsReplaceEnter('${chars.cStr}')")
        keyPressMap(Key.A, KeyModFlag.Ctrl.i)
        keyPressMap(Key.Delete)
        keyCharsReplace(chars)
        keyPressMap(Key.Enter)
    }
}