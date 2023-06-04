package imgui.internal.api

import glm_.*
import imgui.internal.addClampOverflow
import imgui.internal.subClampOverflow
import unsigned.Ubyte
import unsigned.Uint
import unsigned.Ulong
import unsigned.Ushort

// Data type helpers

/* ~DataTypeApplyOp */

infix fun Byte.addOp(arg: Byte): Byte = addClampOverflow(this.i, arg.i, Byte.MIN_VALUE.i, Byte.MAX_VALUE.i).b
infix fun Byte.subOp(arg: Byte): Byte = subClampOverflow(this.i, arg.i, Byte.MIN_VALUE.i, Byte.MAX_VALUE.i).b
infix fun Ubyte.addOp(arg: Ubyte): Ubyte = addClampOverflow(this.i, arg.i, Ubyte.MIN_VALUE, Ubyte.MAX_VALUE).ub
infix fun Ubyte.subOp(arg: Ubyte): Ubyte = subClampOverflow(this.i, arg.i, Ubyte.MIN_VALUE, Ubyte.MAX_VALUE).ub
infix fun Short.addOp(arg: Short): Short = addClampOverflow(this.i, arg.i, Short.MIN_VALUE.i, Short.MAX_VALUE.i).s
infix fun Short.subOp(arg: Short): Short = subClampOverflow(this.i, arg.i, Short.MIN_VALUE.i, Short.MAX_VALUE.i).s
infix fun Ushort.addOp(arg: Ushort): Ushort = addClampOverflow(this.i, arg.i, Ushort.MIN_VALUE, Ushort.MAX_VALUE).us
infix fun Ushort.subOp(arg: Ushort): Ushort = subClampOverflow(this.i, arg.i, Ushort.MIN_VALUE, Ushort.MAX_VALUE).us
infix fun Int.addOp(arg: Int): Int = addClampOverflow(this, arg, Int.MIN_VALUE, Int.MAX_VALUE)
infix fun Int.subOp(arg: Int): Int = subClampOverflow(this, arg, Int.MIN_VALUE, Int.MAX_VALUE)
infix fun Uint.addOp(arg: Uint): Uint = addClampOverflow(this.L, arg.L, Uint.MIN_VALUE, Uint.MAX_VALUE).ui
infix fun Uint.subOp(arg: Uint): Uint = subClampOverflow(this.L, arg.L, Uint.MIN_VALUE, Uint.MAX_VALUE).ui
infix fun Long.addOp(arg: Long): Long = addClampOverflow(this, arg, Long.MIN_VALUE, Long.MAX_VALUE)
infix fun Long.subOp(arg: Long): Long = subClampOverflow(this, arg, Long.MIN_VALUE, Long.MAX_VALUE)
infix fun Ulong.addOp(arg: Ulong): Ulong = addClampOverflow(this.toBigInt(), arg.toBigInt(), Ulong.MIN_VALUE, Ulong.MAX_VALUE).ul
infix fun Ulong.subOp(arg: Ulong): Ulong = subClampOverflow(this.toBigInt(), arg.toBigInt(), Ulong.MIN_VALUE, Ulong.MAX_VALUE).ul