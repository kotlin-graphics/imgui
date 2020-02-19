package imgui.test.engine.context

import glm_.b
import imgui.Key
import imgui.internal.textCharFromUtf8
import imgui.test.engine.KeyModFlag
import imgui.test.engine.KeyModFlags
import imgui.test.engine.KeyState
import imgui.test.engine.core.TestInput
import imgui.test.engine.getKeyModsPrefixStr

fun TestContext.keyDownMap (key: Key, modFlags: KeyModFlags = KeyModFlag.None.i) {

    if (isError)        return

    IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
    val modFlagsStr = getKeyModsPrefixStr(modFlags)
    logDebug("KeyDownMap($modFlagsStr${if(key != Key.Count) key.name else ""})")
    inputs!!.queue += TestInput.fromKey(key, KeyState.Down, modFlags)
    yield()
    yield()
}

fun TestContext.keyUpMap (key: Key, modFlags: KeyModFlags = KeyModFlag.None.i) {

    if (isError)        return

    IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
    val modFlagsStr = getKeyModsPrefixStr(modFlags)
    logDebug("KeyUpMap($modFlagsStr${if(key != Key.Count) key.name else ""})")
    inputs!!.queue += TestInput.fromKey(key, KeyState.Up, modFlags)
    yield()
    yield()
}

fun TestContext.keyPressMap (key: Key, modFlags: KeyModFlags = KeyModFlag.None.i, count_: Int = 1) {

    if (isError)        return

    var count = count_
    IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
    val modFlagsStr = getKeyModsPrefixStr(modFlags)
    logDebug("KeyPressMap($modFlagsStr${if(key != Key.Count) key.name else ""}, $count)")
    while (count > 0)    {
        count--
        inputs!!.queue += TestInput.fromKey(key, KeyState.Down, modFlags)
        yield()
        inputs!!.queue += TestInput.fromKey(key, KeyState.Up, modFlags)
        yield()

        // Give a frame for items to react
        yield()
    }
}

fun TestContext.keyChars (chars: ByteArray) {

    if (isError)        return

    IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
    logDebug("KeyChars('${String(chars)}')")
    var p = 0
    while (chars[p] != 0.b)    {
        val (c, bytesCount) = textCharFromUtf8(chars)
        p += bytesCount
        if (c in 1..0xFFFF)
            inputs!!.queue += TestInput.fromChar(c.c)

        if (!engineIO!!.configRunFast)
            sleep(1f / engineIO!!.typingSpeed)
    }
    yield()
}

fun TestContext.keyCharsAppend (chars: ByteArray) {

    if (isError)        return

    IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
    logDebug("KeyCharsAppend('${String(chars)}')")
    keyPressMap(Key.End)
    keyChars(chars)
}

fun TestContext.keyCharsAppendEnter (chars: ByteArray) {

    if (isError)        return

    IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
    logDebug("KeyCharsAppendEnter('${String(chars)}')")
    keyPressMap(Key.End)
    keyChars(chars)
    keyPressMap(Key.Enter)
}

fun TestContext.keyCharsReplace (chars: ByteArray) {

    if (isError)        return

    IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
    logDebug("KeyCharsReplace('${String(chars)}')")
    keyPressMap(Key.A, KeyModFlag.Ctrl.i)
    keyPressMap(Key.Delete)
    keyChars(chars)
}

fun TestContext.keyCharsReplaceEnter (chars: ByteArray) {

    if (isError) return

    IMGUI_TEST_CONTEXT_REGISTER_DEPTH(this)
    logDebug("KeyCharsReplaceEnter('${String(chars)}')")
    keyPressMap(Key.A, KeyModFlag.Ctrl.i)
    keyPressMap(Key.Delete)
    keyCharsReplace(chars)
    keyPressMap(Key.Enter)
}