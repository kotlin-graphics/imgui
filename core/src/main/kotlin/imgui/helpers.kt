package imgui

import com.sun.jdi.VirtualMachine
import glm_.L
import glm_.b
import glm_.vec4.Vec4
import imgui.classes.InputTextCallbackData
import imgui.classes.SizeCallbackData
import imgui.internal.F32_TO_INT8_SAT
import imgui.internal.textStrToUtf8
import kool.*
import org.lwjgl.system.MemoryUtil
import uno.kotlin.NUL
import java.nio.IntBuffer
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.SimpleFormatter
import kotlin.reflect.KMutableProperty0


// We need boxed objects in order to deliver a constant hashcode
internal var ptrId: Array<Int> = Array(512) { it }


operator fun StringBuilder.plusAssign(string: String) {
    append(string)
}

operator fun StringBuilder.plusAssign(char: Char) {
    append(char)
}

const val NUL = '\u0000'

/** Unique ID used by widgets (typically hashed from a stack of string) */
typealias ID = Int

/** User data to identify a texture */
typealias TextureID = Int

// Return false = pass
typealias InputTextCallback = (InputTextCallbackData) -> Boolean
typealias SizeCallback = (SizeCallbackData) -> Unit

typealias TextEditCallbackData = InputTextCallbackData

infix fun String.cmp(charArray: CharArray): Boolean {
    for (i in indices)
        if (get(i) != charArray[i])
            return false
    return true
}

infix fun CharArray.cmp(other: CharArray): Boolean {
    for (i in indices) {
        val a = get(i)
        val b = other.getOrElse(i) { return false }
        if (a == NUL)
            return b == NUL
        if (a != b)
            return false
    }
    return true
}

object Debug {

    var vm: VirtualMachine? = null

    /** Instance count update interval in seconds   */
    var updateInterval = 5
    private var lastUpdate = System.nanoTime()

    init {
        try {
//            val ac: AttachingConnector = Bootstrap.virtualMachineManager().attachingConnectors().find {
//                it.javaClass.name.toLowerCase().indexOf("socket") != -1
//            } ?: throw Error("No socket attaching connector found")
//            val connectArgs = HashMap<String, Connector.Argument>(ac.defaultArguments())
//            connectArgs["hostname"]!!.setValue("127.0.0.1")
//            connectArgs["port"]!!.setValue(3001.toString())
//            connectArgs["timeout"]!!.setValue("3000")
//            vm = ac.attach(connectArgs)
        } catch (error: Exception) {
            System.err.println("Couldn't retrieve the number of allocations, $error")
        }
    }

    val instanceCounts: Long
        get() {
            val now = System.nanoTime()
            if ((now - lastUpdate) > updateInterval * 1e9) {
                cachedInstanceCounts = countInstances()
                lastUpdate = now
            }
            return cachedInstanceCounts
        }

    private fun countInstances() = vm?.instanceCounts(vm?.allClasses())?.sum() ?: -1

    private var cachedInstanceCounts = countInstances()
}

val fileHandler = FileHandler("./imgui.log").apply { formatter = SimpleFormatter() }
val logger = Logger.getLogger("My Logger").apply {
    addHandler(fileHandler)
    useParentHandlers = false
}

infix fun IntBuffer.resize(newSize: Int): IntBuffer = when {
    newSize > cap -> reserve(growCapacity(newSize))
    else -> this
}.apply { lim = newSize }

/** Resize a vector to a smaller size, guaranteed not to cause a reallocation */
infix fun IntBuffer.shrink(newSize: Int) {
    assert(newSize <= cap)
    lim = newSize
}

infix fun IntBuffer.growCapacity(sz: Int): Int {
    val newCapacity = if (cap > 0) cap + cap / 2 else 8
    return if (newCapacity > sz) newCapacity else sz
}

infix fun IntBuffer.reserve(newCapacity: Int): IntBuffer {
    if (newCapacity <= cap)
        return this
    val newData = IntBuffer(newCapacity)
    val backupLim = lim
    lim = 0
    if (cap > 0)
        MemoryUtil.memCopy(adr, newData.adr, remSize.L)
    newData.lim = backupLim
    free()
    return newData
}

fun IntBuffer(from: IntBuffer): IntBuffer {
    val res = IntBuffer(from.cap)
    res.lim = from.lim
    for (i in 0 until from.lim)
        res[i] = from[i]
    return res
}

val CharArray.strlen: Int
    get() {
        var i = 0
        while (i < size && this[i] != NUL) i++
        return i
    }

val Int.vec4: Vec4
    get() {
        val s = 1f / 255f
        return Vec4(
                ((this ushr COL32_R_SHIFT) and 0xFF) * s,
                ((this ushr COL32_G_SHIFT) and 0xFF) * s,
                ((this ushr COL32_B_SHIFT) and 0xFF) * s,
                ((this ushr COL32_A_SHIFT) and 0xFF) * s)
    }

val Vec4.u32: Int
    get() {
        var out = F32_TO_INT8_SAT(x) shl COL32_R_SHIFT
        out = out or (F32_TO_INT8_SAT(y) shl COL32_G_SHIFT)
        out = out or (F32_TO_INT8_SAT(z) shl COL32_B_SHIFT)
        return out or (F32_TO_INT8_SAT(w) shl COL32_A_SHIFT)
    }

var imeInProgress = false
//    var imeLastKey = 0

fun ByteArray.memchr(startIdx: Int, c: Char, num: Int = size - startIdx): Int {
    val char = c.b
    for (i in startIdx until startIdx + num)
        if (this[i] == char)
            return i
    return -1
}

fun ByteArray.strlen(begin: Int = 0): Int {
    var len = 0
    for (i in begin until size)
        if (get(i) == 0.b) break
        else len++
    return len
}

fun String.toByteArray(size: Int): ByteArray = toByteArray().copyInto(ByteArray(size))
fun String.toUtf8(size: Int) = ByteArray(size).also { textStrToUtf8(it, toCharArray()) }
fun String.toByteArray(array: ByteArray): ByteArray {
    val bytes = toByteArray()
    bytes.copyInto(array)
    if (bytes.size < array.size)
        array[bytes.size] = 0 // NUL
    return array
}

// Function to implement strcmp function https://www.techiedelight.com/implement-strcmp-function-c/
infix fun ByteArray.strcmp(other: ByteArray): Int {
    var i = 0
    while (i < size && get(i) != 0.b) {
        // if characters differ or end of second string is reached
        if (get(i) != other.getOrElse(i) { 0 })
            break
        // move to next pair of characters
        i++
    }
    return getOrElse(i) { 0 }.compareTo(other.getOrElse(i) { 0 })
}

/** TODO -> uno or kool */
operator fun <T> KMutableProperty0<T>.invoke(t: T): KMutableProperty0<T> {
    set(t)
    return this
}

val ByteArray.cStr get() = String(this, 0, strlen())