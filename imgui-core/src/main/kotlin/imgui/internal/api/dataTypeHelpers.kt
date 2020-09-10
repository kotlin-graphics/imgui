package imgui.internal.api

import glm_.*
import imgui.DataType
import imgui.ImGui.style
import imgui.NUL
import imgui._i
import imgui.api.*
import imgui.cStr
import imgui.internal.addClampOverflow
import imgui.internal.subClampOverflow
import uno.kotlin.getValue
import uno.kotlin.setValue
import unsigned.Ubyte
import unsigned.Uint
import unsigned.Ulong
import unsigned.Ushort
import kotlin.reflect.KMutableProperty0

@Suppress("UNCHECKED_CAST")

/** Data type helpers */
internal interface dataTypeHelpers {

    //    IMGUI_API const ImGuiDataTypeInfo*  DataTypeGetInfo(ImGuiDataType data_type);

    /** DataTypeFormatString */
    fun KMutableProperty0<*>.format(dataType: DataType, format: String): String {
        val arg = when (val t = this()) {
            // we need to intervene since java printf cant handle %u
            is Ubyte -> t.i
            is Ushort -> t.i
            is Uint -> t.L
            is Ulong -> t.toBigInt()
            else -> t // normal scalar
        }
        return format.format(style.locale, arg)
    }

    fun <N : Number> dataTypeApplyOp(dataType: DataType, op: Char, value1: N, value2: N): N {
        assert(op == '+' || op == '-')
        return when (dataType) {
            /*  Signedness doesn't matter when adding or subtracting
                Note: on jvm Byte and Short (and their unsigned counterparts use all unsigned under the hood),
                so we directly switch to Integers in these cases. We also use some custom clamp min max values because of this
             */
            DataType.Byte -> when (op) {
                '+' -> addClampOverflow((value1 as Byte).i, (value2 as Byte).i, S8_MIN, S8_MAX).b as N
                '-' -> subClampOverflow((value1 as Byte).i, (value2 as Byte).i, S8_MIN, S8_MAX).b as N
                else -> throw Error()
            }
            DataType.Ubyte -> when (op) {
                '+' -> Ubyte(addClampOverflow((value1 as Ubyte).i, (value2 as Ubyte).i, U8_MIN, U8_MAX)) as N
                '-' -> Ubyte(subClampOverflow((value1 as Ubyte).i, (value2 as Ubyte).i, U8_MIN, U8_MAX)) as N
                else -> throw Error()
            }
            DataType.Short -> when (op) {
                '+' -> addClampOverflow((value1 as Short).i, (value2 as Short).i, S16_MIN, S16_MAX).s as N
                '-' -> subClampOverflow((value1 as Short).i, (value2 as Short).i, S16_MIN, S16_MAX).s as N
                else -> throw Error()
            }
            DataType.Ushort -> when (op) {
                '+' -> Ushort(addClampOverflow((value1 as Ushort).i, (value2 as Ushort).i, U16_MIN, U16_MAX)) as N
                '-' -> Ushort(subClampOverflow((value1 as Ushort).i, (value2 as Ushort).i, U16_MIN, U16_MAX)) as N
                else -> throw Error()
            }
            DataType.Int -> when (op) {
                '+' -> addClampOverflow(value1 as Int, value2 as Int, Int.MIN_VALUE, Int.MAX_VALUE) as N
                '-' -> subClampOverflow(value1 as Int, value2 as Int, Int.MIN_VALUE, Int.MAX_VALUE) as N
                else -> throw Error()
            }
            DataType.Uint -> when (op) {
                '+' -> Uint(addClampOverflow((value1 as Uint).L, (value2 as Uint).L, 0L, Uint.MAX_VALUE)) as N
                '-' -> Uint(subClampOverflow((value1 as Uint).L, (value2 as Uint).L, 0L, Uint.MAX_VALUE)) as N
                else -> throw Error()
            }
            DataType.Long -> when (op) {
                '+' -> addClampOverflow(value1 as Long, value2 as Long, Long.MIN_VALUE, Long.MAX_VALUE) as N
                '-' -> subClampOverflow(value1 as Long, value2 as Long, Long.MIN_VALUE, Long.MAX_VALUE) as N
                else -> throw Error()
            }
            DataType.Ulong -> when (op) {
                '+' -> Ulong(addClampOverflow((value1 as Ulong).toBigInt(), (value2 as Ulong).toBigInt(), Ulong.MIN_VALUE, Ulong.MAX_VALUE)) as N
                '-' -> Ulong(subClampOverflow((value1 as Ulong).toBigInt(), (value2 as Ulong).toBigInt(), Ulong.MIN_VALUE, Ulong.MAX_VALUE)) as N
                else -> throw Error()
            }
            DataType.Float -> when (op) {
                '+' -> (value1 as Float + value2 as Float) as N
                '-' -> (value1 as Float - value2 as Float) as N
                else -> throw Error()
            }
            DataType.Double -> when (op) {
                '+' -> (value1 as Double + value2 as Double) as N
                '-' -> (value1 as Double - value2 as Double) as N
                else -> throw Error()
            }
            else -> error("invalid, this is a private enum value")
        }
    }

    /** User can input math operators (e.g. +100) to edit a numerical values.
     *  NB: This is _not_ a full expression evaluator. We should probably add one and replace this dumb mess.. */
    fun dataTypeApplyOpFromText(buf: String, initialValueBuf: ByteArray, dataType: DataType, pData: IntArray,
                                format: String? = null): Boolean {
        _i = pData[0]
        return dataTypeApplyOpFromText(buf, initialValueBuf, dataType, ::_i, format)
                .also { pData[0] = _i }
    }

    fun dataTypeApplyOpFromText(buf_: String, initialValueBuf_: ByteArray, dataType: DataType,
                                dataPtr: KMutableProperty0<*>, format: String? = null): Boolean {

        val buf = buf_.replace(Regex("\\s+"), "")
                .replace("$NUL", "")
                .split(Regex("-+\\*/"))

        val initialValueBuf = initialValueBuf_.cStr.replace(Regex("\\s+"), "")
                .replace("$NUL", "")
                .split(Regex("-+\\*/"))

        /*  We don't support '-' op because it would conflict with inputing negative value.
            Instead you can use +-100 to subtract from an existing value     */
        val op = buf.getOrNull(1)?.get(0)

        return when (buf_[0]) {
            NUL -> false
            else -> when (dataType) {
                DataType.Int -> {
                    val fmt = format ?: "%d"
                    var v by dataPtr as KMutableProperty0<Int>
                    val dataBackup = v
                    val arg0i = try {
                        buf[0].format(style.locale, fmt).i
                    } catch (_: Exception) {
                        return false
                    }

                    v = when (op) {
                        '+' -> {    // Add (use "+-" to subtract)
                            val arg1i = buf[2].format(style.locale, "%d").i
                            (arg0i + arg1i).i
                        }
                        '*' -> {    // Multiply
                            val arg1f = buf[2].format(style.locale, "%f").f
                            (arg0i * arg1f).i
                        }
                        '/' -> {    // Divide
                            val arg1f = buf[2].format(style.locale, "%f").f
                            when (arg1f) {
                                0f -> arg0i
                                else -> (arg0i / arg1f).i
                            }
                        }
                        else -> try { // Assign constant
                            buf[1].format(style.locale, fmt).i
                        } catch (_: Exception) {
                            arg0i
                        }
                    }
                    dataBackup != v
                }
                DataType.Float -> {
                    // For floats we have to ignore format with precision (e.g. "%.2f") because sscanf doesn't take them in [JVM] not true
                    val fmt = format ?: "%f"
                    var v by dataPtr as KMutableProperty0<Float>
                    val dataBackup = v
                    val arg0f = try {
                        initialValueBuf[0].format(style.locale, fmt).f
                    } catch (_: Exception) {
                        return false
                    }
                    val arg1f = try {
                        buf.getOrElse(2) { buf[0] }.format(style.locale, fmt).f
                    } catch (_: Exception) {
                        return false
                    }
                    v = when (op) {
                        '+' -> arg0f + arg1f    // Add (use "+-" to subtract)
                        '*' -> arg0f * arg1f    // Multiply
                        '/' -> when (arg1f) {   // Divide
                            0f -> arg0f
                            else -> arg0f / arg1f
                        }
                        else -> arg1f           // Assign constant
                    }
                    dataBackup != v
                }
                DataType.Double -> {
                    val fmt = format ?: "%f"
                    var v by dataPtr as KMutableProperty0<Double>
                    val dataBackup = v
                    val arg0f = try {
                        buf[0].format(style.locale, fmt).d
                    } catch (_: Exception) {
                        return false
                    }
                    val arg1f = try {
                        buf[2].format(style.locale, fmt).d
                    } catch (_: Exception) {
                        return false
                    }
                    v = when (op) {
                        '+' -> arg0f + arg1f    // Add (use "+-" to subtract)
                        '*' -> arg0f * arg1f    // Multiply
                        '/' -> when (arg1f) {   // Divide
                            0.0 -> arg0f
                            else -> arg0f / arg1f
                        }
                        else -> arg1f           // Assign constant
                    }
                    dataBackup != v
                }
                DataType.Uint, DataType.Long, DataType.Ulong ->
                    /*  Assign constant
                        FIXME: We don't bother handling support for legacy operators since they are a little too crappy.
                        Instead we may implement a proper expression evaluator in the future.                 */
                    //sscanf(buf, format, data_ptr)
                    TODO()
                else -> TODO()
//            {
//                // Small types need a 32-bit buffer to receive the result from scanf()
//                int v32;
//                sscanf(buf, format, &v32);
//                if (data_type == ImGuiDataType_S8)
//                *(ImS8*)data_ptr = (ImS8)ImClamp(v32, (int)IM_S8_MIN, (int)IM_S8_MAX);
//                else if (data_type == ImGuiDataType_U8)
//                *(ImU8*)data_ptr = (ImU8)ImClamp(v32, (int)IM_U8_MIN, (int)IM_U8_MAX);
//                else if (data_type == ImGuiDataType_S16)
//                *(ImS16*)data_ptr = (ImS16)ImClamp(v32, (int)IM_S16_MIN, (int)IM_S16_MAX);
//                else if (data_type == ImGuiDataType_U16)
//                *(ImU16*)data_ptr = (ImU16)ImClamp(v32, (int)IM_U16_MIN, (int)IM_U16_MAX);
//                else
//                IM_ASSERT(0);
//            }
            }
        }
    }

}