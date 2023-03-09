package imgui.font


fun main() {
    encode(ubyteArrayOf(0u, 1u, 2u, 3u))
}

fun encode(array: UByteArray): String {

    var p = 0
    val result = ArrayList<Char>()
    var remainder = 0

    var maxValues = 0
    var maxRemainder = 0

    fun next(): Int {
        if (maxRemainder < )
    }

    while (p < array.size) {

        if (maxValues >= 95) {
//            val times = maxValues / 95 // 2
//            remainder = maxValues % 95 // 66
//            val tmp = maxValues - remainder
        }
        else {
            val next = array[p++]

        }
    }
}

/*
Dec -> Base 95
0 -> ` `
95 -> ~
96 -> `! `
 */