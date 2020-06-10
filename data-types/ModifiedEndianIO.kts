@file:Suppress("NAME_SHADOWING")

import java.nio.ByteBuffer

enum class Endian {
    BIG,
    LITTLE,
    MIDDLE;
}

enum class Modifier {
    NONE,
    ADD,
    INVERSE,
    SUBTRACT;
}

enum class DataType(val byteCount: Int) {
    BYTE(1),
    SHORT(2),
    MEDIUM(3),
    INT(4),
    LONG(8);
}

@Throws(IllegalStateException::class)
fun check(type: DataType, modifier: Modifier, order: Endian) {
    if (order == Endian.MIDDLE) {
        check(modifier == Modifier.NONE || modifier == Modifier.INVERSE) {
            "Middle endian doesn't support variable modifier $modifier"
        }
        check(type == DataType.INT) {
            "Middle endian can only be used with an integer"
        }
    }
}

fun read(buffer: ByteBuffer, type: DataType, modifier: Modifier = Modifier.NONE, order: Endian = Endian.BIG): Long {
    check(buffer.remaining() >= type.byteCount) {
        "Not enough allocated buffer remaining $type."
    }

    check(type, modifier, order)

    val range = endianRange(order, modifier, type.byteCount)
    var modifier = modifier

    var longValue: Long = 0
    for (index in range) {
        val read = readValue(buffer.get().toInt(), modifier, bitIndex = index * 8)
        longValue = longValue or read.toLong()
        if (modifier != Modifier.NONE) {
            modifier = Modifier.NONE
        }
    }
    return longValue
}

fun write(buffer: ByteBuffer, type: DataType, value: Number, modifier: Modifier = Modifier.NONE, order: Endian = Endian.BIG) {
    check(type, modifier, order)

    val range = endianRange(order, modifier, type.byteCount)
    val longValue = value.toLong()
    var modifier = modifier

    for (index in range) {
        writeValue(buffer, longValue, modifier, bitIndex = index * 8)
        if (modifier != Modifier.NONE) {
            modifier = Modifier.NONE
        }
    }
}

private val MIDDLE_ENDIAN_ORDER = listOf(1, 0, 3, 2)
private val MIDDLE_ENDIAN_INVERSE = MIDDLE_ENDIAN_ORDER.reversed()

fun endianRange(order: Endian, modifier: Modifier, byteCount: Int) = when (order) {
    Endian.BIG -> byteCount - 1 downTo 0
    Endian.LITTLE -> 0 until byteCount
    Endian.MIDDLE -> if (modifier == Modifier.INVERSE) {
        MIDDLE_ENDIAN_INVERSE
    } else {
        MIDDLE_ENDIAN_ORDER
    }
}

fun readValue(value: Int, modifier: Modifier, bitIndex: Int): Int {
    return when (modifier) {
        Modifier.ADD -> value - 128 and 0xff
        Modifier.INVERSE -> -value and 0xff
        Modifier.SUBTRACT -> 128 - value and 0xff
        else -> value and 0xff shl bitIndex
    }
}

fun writeValue(buffer: ByteBuffer, value: Long, modifier: Modifier, bitIndex: Int) {
    val value = when (modifier) {
        Modifier.ADD -> value + 128
        Modifier.INVERSE -> -value
        Modifier.SUBTRACT -> 128 - value
        else -> value shr bitIndex
    }
    buffer.put(value.toByte())
}