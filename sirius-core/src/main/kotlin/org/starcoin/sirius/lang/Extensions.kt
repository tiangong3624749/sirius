package org.starcoin.sirius.lang

import org.starcoin.sirius.util.Utils
import java.math.BigInteger
import java.util.*

fun ByteArray.toULong() = BigInteger(1, this).toLong()

fun ByteArray.toHEXString() = Utils.HEX.encode(this).let { if (!it.startsWith("0x")) "0x$it" else it }

fun ByteArray.toBigInteger(offset: Int, length: Int) = BigInteger(1, Arrays.copyOfRange(this, offset, offset + length))
fun ByteArray.toBigInteger() = BigInteger(this)
fun ByteArray.toUnsignedBigInteger() = BigInteger(1, this)

fun String.hexToByteArray(): ByteArray {
    if (length % 2 != 0)
        throw IllegalArgumentException("hex-string must have an even number of digits (nibbles)")

    val cleanInput = if (startsWith("0x")) substring(2) else this

    return ByteArray(cleanInput.length / 2).apply {
        var i = 0
        while (i < cleanInput.length) {
            this[i / 2] = ((cleanInput[i].getNibbleValue() shl 4) + cleanInput[i + 1].getNibbleValue()).toByte()
            i += 2
        }
    }
}

private fun Char.getNibbleValue() = Character.digit(this, 16).also {
    if (it == -1) throw IllegalArgumentException("Not a valid hex char: $this")
}
