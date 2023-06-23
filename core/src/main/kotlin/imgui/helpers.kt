package imgui

//import com.sun.jdi.VirtualMachine
import glm_.L
import glm_.b
import glm_.vec3.Vec3
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

/** A unique ID used by widgets (typically the result of hashing a stack of string) */
typealias ID = Int

/** ImTexture: user data for renderer backend to identify a texture [Compile-time configurable type]
 *  - To use something else than an opaque void* pointer: override with e.g. '#define ImTextureID MyTextureType*' in your imconfig.h file.
 *  - This can be whatever to you want it to be! read the FAQ about ImTextureID for details. */
typealias TextureID = Int

/** Return false = pass
 *
 *  Callback function for ImGui::InputText() */
typealias InputTextCallback = (InputTextCallbackData) -> Boolean
/** Callback function for ImGui::SetNextWindowSizeConstraints() */
typealias SizeCallback = (SizeCallbackData) -> Unit

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

//    var vm: VirtualMachine? = null

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

    private fun countInstances() = 0L//vm?.instanceCounts(vm?.allClasses())?.sum() ?: -1

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
        MemoryUtil.memCopy(adr.L, newData.adr.L, remByte.L)
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

/** ~ColorConvertU32ToFloat4 */
val Int.vec4: Vec4
    get() {
        val s = 1f / 255f
        return Vec4(
                ((this ushr COL32_R_SHIFT) and 0xFF) * s,
                ((this ushr COL32_G_SHIFT) and 0xFF) * s,
                ((this ushr COL32_B_SHIFT) and 0xFF) * s,
                ((this ushr COL32_A_SHIFT) and 0xFF) * s)
    }

/** ~ColorConvertFloat4ToU32 */
val Vec4.u32: Int
    get() = floatsToU32(x, y, z, w)

/** ~ColorConvertFloat4ToU32 */
fun floatsToU32(x: Float, y: Float, z: Float, w: Float = 0f): Int {
    var out = F32_TO_INT8_SAT(x) shl COL32_R_SHIFT
    out = out or (F32_TO_INT8_SAT(y) shl COL32_G_SHIFT)
    out = out or (F32_TO_INT8_SAT(z) shl COL32_B_SHIFT)
    return out or (F32_TO_INT8_SAT(w) shl COL32_A_SHIFT)
}

var imeInProgress = false
//    var imeLastKey = 0

fun ByteArray.memchr(startIdx: Int, c: Char, num: Int = size - startIdx): Int {
    val char = c.b
    for (i in startIdx until startIdx + num) {
        if (this[i] == 0.b)
            return -1
        if (this[i] == char)
            return i
    }
    return -1
}

fun ByteArray.strlen(begin: Int = 0): Int {
    var len = 0
    for (i in begin until size)
        if (get(i) == 0.b) break
        else len++
    return len
}
fun List<Char>.strlen(begin: Int = 0): Int {
    var len = 0
    for (i in begin until size)
        if (get(i) == NUL) break
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

fun ByteArray.strncmp(other: ByteArray, len: Int): Int {
    check(size >= len && other.size >= len)
    for (i in 0 until len) {
        val cmp = this[i].compareTo(other[i])
        if (cmp != 0)
            return cmp
    }
    return 0
}

/** TODO -> uno or kool */
operator fun <T> KMutableProperty0<T>.invoke(t: T): KMutableProperty0<T> {
    set(t)
    return this
}

val ByteArray.cStr get() = String(this, 0, strlen())

typealias Vec3Setter = (x: Float, y: Float, z: Float) -> Unit
typealias Vec4Setter = (x: Float, y: Float, z: Float, w: Float) -> Unit

inline infix fun Vec3.into(setter: Vec3Setter) = setter(x, y, z)
inline infix fun Vec4.into(setter: Vec3Setter) = setter(x, y, z)
inline infix fun Vec4.into(setter: Vec4Setter) = setter(x, y, z, w)
inline fun Vec4.into(setter: Vec4Setter, w: Float) = setter(x, y, z, w)

fun Vec4.put(x: Float, y: Float, z: Float) {
    put(x, y, z, w)
}

fun Vec3.put(x: Float, y: Float, z: Float, w: Float) {
    put(x, y, z)
}

fun FloatArray.put(x: Float, y: Float, z: Float) {
    this[0] = x
    this[1] = y
    this[2] = z
}

fun FloatArray.put(x: Float, y: Float, z: Float, w: Float) {
    this[0] = x
    this[1] = y
    this[2] = z
    this[3] = w
}

fun FloatArray.put(vararg f: Float) {
    f.copyInto(this)
}

typealias BitArrayPtr = Int