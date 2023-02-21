package imgui.internal.api

import glm_.*
import imgui.DataType
import imgui.ImGui.style
import imgui._i32
import imgui.api.*
import imgui.internal.addClampOverflow
import imgui.internal.subClampOverflow
import uno.kotlin.NUL
import uno.kotlin.getValue
import uno.kotlin.setValue
import unsigned.*
import unsigned.parseUnsignedLong as _
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
    fun dataTypeApplyFromText(buf: String, dataType: DataType, pData: IntArray, format: String): Boolean {
        _i32 = pData[0]
        return dataTypeApplyFromText(buf, dataType, ::_i32, format)
                .also { pData[0] = _i32 }
    }

    fun dataTypeApplyFromText(buf_: String, dataType: DataType, pData: KMutableProperty0<*>, format: String): Boolean {

        // ImCharIsBlankA
        var buf = buf_.replace(Regex("\\s+"), "")
                .replace("\t", "")
        if (buf.last() == NUL)
            buf = buf.dropLast(1) // termination

        // Copy the value in an opaque buffer so we can compare at the end of the function if it changed at all.
        val dataBackup = pData()

        if (buf.isEmpty())
            return false

        when (dataType) {
            DataType.Byte -> {
                var data by pData as KMutableProperty0<Byte>
                data = format.format(buf.parseInt()).toByte()
            }

            DataType.Ubyte -> {
                var data by pData as KMutableProperty0<Ubyte>
                data = format.format(buf.parseInt()).toByte().toUbyte()
            }

            DataType.Short -> {
                var data by pData as KMutableProperty0<Short>
                data = format.format(buf.parseInt()).toShort()
            }

            DataType.Ushort -> {
                var data by pData as KMutableProperty0<Ushort>
                data = format.format(buf.parseInt()).toShort().toUshort()
            }

            DataType.Int -> {
                var data by pData as KMutableProperty0<Int>
                data = format.format(buf.parseLong()).toInt()
            }

            DataType.Uint -> {
                var data by pData as KMutableProperty0<Uint>
                data = format.format(buf.parseLong()).toInt().toUint()
            }

            DataType.Long -> {
                var data by pData as KMutableProperty0<Long>
                data = format.format(buf.parseUnsignedLong()).toLong()
            }

            DataType.Ulong -> {
                var data by pData as KMutableProperty0<Ulong>
                data = format.format(buf.parseUnsignedLong()).toLong().toUlong()
            }

            DataType.Float -> {
                var data by pData as KMutableProperty0<Float>
                data = format.format(buf.parseFloat).toFloat()
            }

            DataType.Double -> {
                var data by pData as KMutableProperty0<Double>
                data = format.format(buf.parseDouble).toDouble()
            }

            else -> error("")
        }
        // Sanitize format
        // For float/double we have to ignore format with precision (e.g. "%.2f") because sscanf doesn't take them in, so force them into %f and %lf
//        char format_sanitized[32];
//        if (dataType == DataType.Float || dataType == DataType.Double)
//            format = ""
//        else
//        format = ImParseFormatSanitizeForScanning(format, format_sanitized, IM_ARRAYSIZE(format_sanitized));
//
//        // Small types need a 32-bit buffer to receive the result from scanf()
//        int v32 = 0;
//        if (sscanf(buf, format, type_info->Size >= 4 ? p_data : &v32) < 1)
//        return false;
//        if (type_info->Size < 4)
//        {
//            if (data_type == ImGuiDataType_S8)
//            *(ImS8*)p_data = (ImS8)ImClamp(v32, (int)IM_S8_MIN, (int)IM_S8_MAX);
//            else if (data_type == ImGuiDataType_U8)
//            *(ImU8*)p_data = (ImU8)ImClamp(v32, (int)IM_U8_MIN, (int)IM_U8_MAX);
//            else if (data_type == ImGuiDataType_S16)
//            *(ImS16*)p_data = (ImS16)ImClamp(v32, (int)IM_S16_MIN, (int)IM_S16_MAX);
//            else if (data_type == ImGuiDataType_U16)
//            *(ImU16*)p_data = (ImU16)ImClamp(v32, (int)IM_U16_MIN, (int)IM_U16_MAX);
//            else
//            IM_ASSERT(0);
//        }

        return dataBackup != pData()
    }

    // useless on JVM with `N : Number, N : Comparable<N>`
    //    IMGUI_API int           DataTypeCompare(ImGuiDataType data_type, const void* arg_1, const void* arg_2);

    fun <N> dataTypeClampT(pV: KMutableProperty0<N>, vMin: N?, vMax: N?): Boolean
            where N : Number, N : Comparable<N> {
        var v by pV
        // Clamp, both sides are optional, return true if modified
        return when {
            vMin != null && v < vMin -> {; v = vMin; true; }
            vMax != null && v > vMax -> {; v = vMax; true; }
            else -> false
        }
    }

    fun <N> dataTypeClamp(dataType: DataType, pData: KMutableProperty0<N>, pMin: N?, pMax: N?): Boolean
            where N : Number, N : Comparable<N> = when (dataType) {
        DataType.Byte -> dataTypeClampT(pData as KMutableProperty0<Byte>, pMin as Byte?, pMax as Byte?)
        DataType.Ubyte -> dataTypeClampT(pData as KMutableProperty0<Ubyte>, pMin as Ubyte?, pMax as Ubyte?)
        DataType.Short -> dataTypeClampT(pData as KMutableProperty0<Short>, pMin as Short?, pMax as Short?)
        DataType.Ushort -> dataTypeClampT(pData as KMutableProperty0<Ushort>, pMin as Ushort?, pMax as Ushort?)
        DataType.Int -> dataTypeClampT(pData as KMutableProperty0<Int>, pMin as Int?, pMax as Int?)
        DataType.Uint -> dataTypeClampT(pData as KMutableProperty0<Uint>, pMin as Uint?, pMax as Uint?)
        DataType.Long -> dataTypeClampT(pData as KMutableProperty0<Long>, pMin as Long?, pMax as Long?)
        DataType.Ulong -> dataTypeClampT(pData as KMutableProperty0<Ulong>, pMin as Ulong?, pMax as Ulong?)
        DataType.Float -> dataTypeClampT(pData as KMutableProperty0<Float>, pMin as Float?, pMax as Float?)
        DataType.Double -> dataTypeClampT(pData as KMutableProperty0<Double>, pMin as Double?, pMax as Double?)
        else -> error("invalid")
    }
}