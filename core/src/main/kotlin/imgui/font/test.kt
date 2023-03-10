package imgui.font

import imgui.stb_.TrueType.ULong
import imgui.stb_.i
import imgui.stb_.ul
import java.io.File


fun main() {
    val ini = File("imgui.ini")
    val a = ini.readBytes()
//    encode(ubyteArrayOf(0u, 1u, 2u, 3u))
}

/**
Base32768 is a binary-to-text encoding optimised for UTF-16-encoded text.
(e.g. Windows, Java, JavaScript)
 */

// Z is a number, usually a uint15 but sometimes a uint7

const val BITS_PER_CHAR = 15 // Base32768 is a 15-bit encoding
const val BITS_PER_BYTE = 8

val pairStrings = listOf("ҠҿԀԟڀڿݠޟ߀ߟကဟႠႿᄀᅟᆀᆟᇠሿበቿዠዿጠጿᎠᏟᐠᙟᚠᛟកសᠠᡟᣀᣟᦀᦟ᧠᧿ᨠᨿᯀᯟᰀᰟᴀᴟ⇠⇿⋀⋟⍀⏟␀␟─❟➀➿⠀⥿⦠⦿⨠⩟⪀⪿⫠⭟ⰀⰟⲀⳟⴀⴟⵀⵟ⺠⻟㇀㇟㐀䶟䷀龿ꀀꑿ꒠꒿ꔀꗿꙀꙟꚠꛟ꜀ꝟꞀꞟꡀꡟ",
                               "ƀƟɀʟ")

const lookupE = {}
const lookupD = {}
pairStrings.forEach((pairString, r) => {
    // Decompression
    const encodeRepertoire =[]
    pairString.match(/../ gu).forEach(pair => {
        const first = pair . codePointAt (0)
        const last = pair . codePointAt (1)
        for (let codePoint = first; codePoint <= last; codePoint++) {
        encodeRepertoire.push(String.fromCodePoint(codePoint))
    }
    })

    const numZBits = BITS_PER_CHAR -BITS_PER_BYTE * r // 0 -> 15, 1 -> 7
    lookupE[numZBits] = encodeRepertoire
    encodeRepertoire.forEach((chr, z) => {
        lookupD[chr] = [numZBits, z]
    })
})

const encode = uint8Array => {
    const length = uint8Array . length

            let str = ''
    let z = 0
    let numZBits = 0

    for (let i = 0; i < length; i++) {
        const uint8 = uint8Array [i]

        // Take most significant bit first
        for (let j = BITS_PER_BYTE - 1; j >= 0; j--) {
        const bit =(uint8 > > j) & 1

        z = (z < < 1)+bit
        numZBits++

        if (numZBits === BITS_PER_CHAR) {
            str += lookupE[numZBits][z]
            z = 0
            numZBits = 0
        }
    }
    }

    if (numZBits !== 0) {
        // Final bits require special treatment.

        // z = bbbbbbcccccccc, numZBits = 14, padBits = 1
        // z = bbbbbcccccccc, numZBits = 13, padBits = 2
        // z = bbbbcccccccc, numZBits = 12, padBits = 3
        // z = bbbcccccccc, numZBits = 11, padBits = 4
        // z = bbcccccccc, numZBits = 10, padBits = 5
        // z = bcccccccc, numZBits = 9, padBits = 6
        // z = cccccccc, numZBits = 8, padBits = 7
        // => Pad `z` out to 15 bits using 1s, then encode as normal (r = 0)

        // z = ccccccc, numZBits = 7, padBits = 0
        // z = cccccc, numZBits = 6, padBits = 1
        // z = ccccc, numZBits = 5, padBits = 2
        // z = cccc, numZBits = 4, padBits = 3
        // z = ccc, numZBits = 3, padBits = 4
        // z = cc, numZBits = 2, padBits = 5
        // z = c, numZBits = 1, padBits = 6
        // => Pad `z` out to 7 bits using 1s, then encode specially (r = 1)

        while (!(numZBits in lookupE)) {
            z = (z < < 1)+1
            numZBits++
        }

        str += lookupE[numZBits][z]
    }

    return str
}

const decode = str => {
    const length = str . length

            // This length is a guess. There's a chance we allocate one more byte here
            // than we actually need. But we can count and slice it off later
            const uint8Array = new Uint8Array(Math.floor(length * BITS_PER_CHAR / BITS_PER_BYTE))
    let numUint8s = 0
    let uint8 = 0
    let numUint8Bits = 0

    for (let i = 0; i < length; i++) {
        const chr = str . charAt (i)

        if (!(chr in lookupD)) {
            throw new Error (`Unrecognised Base32768 character: ${chr}`)
        }

        const[numZBits, z] = lookupD[chr]

        if (numZBits !== BITS_PER_CHAR && i !== length - 1) {
            throw new Error ('Secondary character found before end of input at position ' + String(i))
        }

        // Take most significant bit first
        for (let j = numZBits - 1; j >= 0; j--) {
        const bit =(z > > j) & 1

        uint8 = (uint8 < < 1)+bit
        numUint8Bits++

        if (numUint8Bits === BITS_PER_BYTE) {
            uint8Array[numUint8s] = uint8
            numUint8s++
            uint8 = 0
            numUint8Bits = 0
        }
    }
    }

    // Final padding bits! Requires special consideration!
    // Remember how we always pad with 1s?
    // Note: there could be 0 such bits, check still works though
    if (uint8 !== ((1 < < numUint8Bits) - 1)) {
        throw new Error ('Padding mismatch')
    }

    return new Uint8Array (uint8Array.buffer, 0, numUint8s)
}

export { encode, decode }