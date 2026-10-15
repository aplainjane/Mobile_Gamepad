package com.example.gamepad

import kotlin.math.roundToInt

/** The report ID is sent separately by BluetoothHidDevice.sendReport. */
data class GamepadReport(
    val buttons: Int = 0,
    val leftX: Float = 0f,
    val leftY: Float = 0f,
    val rightX: Float = 0f,
    val rightY: Float = 0f,
    val leftTrigger: Int = 0,
    val rightTrigger: Int = 0,
    val directions: Int = 0,
) {
    fun withButton(button: PadButton, pressed: Boolean) = copy(
        buttons = if (pressed) buttons or button.mask else buttons and button.mask.inv(),
    )

    fun withDirection(direction: PadDirection, pressed: Boolean) = copy(
        directions = if (pressed) directions or direction.mask else directions and direction.mask.inv(),
    )

    fun toBytes(): ByteArray {
        // Mirror triggers and directions to conventional raw button indices as well as axes/POV.
        // This works with generic HID readers without needing a browser's device-specific mapper.
        val x = bit(PadDirection.RIGHT) - bit(PadDirection.LEFT)
        val y = bit(PadDirection.DOWN) - bit(PadDirection.UP)
        var bits = buttons and DIRECT_BUTTON_MASK
        if (leftTrigger > 0) bits = bits or (1 shl 6)
        if (rightTrigger > 0) bits = bits or (1 shl 7)
        if (y < 0) bits = bits or (1 shl 12)
        if (y > 0) bits = bits or (1 shl 13)
        if (x < 0) bits = bits or (1 shl 14)
        if (x > 0) bits = bits or (1 shl 15)
        return ByteArray(SIZE).also { data ->
            data[0] = bits.toByte()
            data[1] = (bits ushr 8).toByte()
            data[2] = (bits ushr 16).toByte()
            listOf(leftX, leftY, rightX, rightY).forEachIndexed { index, value ->
                val encoded = axis(value)
                data[3 + index * 2] = encoded.toByte()
                data[4 + index * 2] = (encoded ushr 8).toByte()
            }
            data[11] = leftTrigger.coerceIn(0, 255).toByte()
            data[12] = rightTrigger.coerceIn(0, 255).toByte()
            data[13] = hat().toByte()
        }
    }

    private fun hat(): Int {
        val x = bit(PadDirection.RIGHT) - bit(PadDirection.LEFT)
        val y = bit(PadDirection.DOWN) - bit(PadDirection.UP)
        return when {
            x == 0 && y < 0 -> 0
            x > 0 && y < 0 -> 1
            x > 0 && y == 0 -> 2
            x > 0 && y > 0 -> 3
            x == 0 && y > 0 -> 4
            x < 0 && y > 0 -> 5
            x < 0 && y == 0 -> 6
            x < 0 && y < 0 -> 7
            else -> 8 // HID null state: the D-pad is released.
        }
    }

    private fun bit(direction: PadDirection) = if (directions and direction.mask != 0) 1 else 0

    companion object {
        const val ID = 1
        const val SIZE = 14
        private const val DIRECT_BUTTON_MASK = 0x10F3F

        private fun axis(value: Float): Int =
            (value.coerceIn(-1f, 1f) * 32767f).roundToInt()

        // Windows raw axis order: X/Y/Z/Rx/Ry/Rz. Keep the four sticks first,
        // followed by independent unsigned triggers. 17 buttons match common raw indices.
        // This is generic HID; browser standard mapping and Steam bindings are separate.
        val DESCRIPTOR = intArrayOf(
            0x05, 0x01, 0x09, 0x05, 0xA1, 0x01, 0x85, ID,
            0x05, 0x09, 0x19, 0x01, 0x29, 0x11,
            0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95, 0x11, 0x81, 0x02,
            0x75, 0x01, 0x95, 0x07, 0x81, 0x03,
            0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x09, 0x32, 0x09, 0x33,
            0x16, 0x01, 0x80, 0x26, 0xFF, 0x7F, 0x75, 0x10, 0x95, 0x04, 0x81, 0x02,
            0x09, 0x34, 0x09, 0x35, 0x15, 0x00, 0x26, 0xFF, 0x00,
            0x75, 0x08, 0x95, 0x02, 0x81, 0x02,
            0x09, 0x39, 0x15, 0x00, 0x25, 0x07, 0x35, 0x00,
            0x46, 0x3B, 0x01, 0x65, 0x14, 0x75, 0x04, 0x95, 0x01, 0x81, 0x42,
            0x65, 0x00, 0x75, 0x04, 0x95, 0x01, 0x81, 0x03, 0xC0,
        ).map(Int::toByte).toByteArray()
    }
}

enum class PadButton(val mask: Int) {
    A(1 shl 0), B(1 shl 1), X(1 shl 2), Y(1 shl 3),
    L1(1 shl 4), R1(1 shl 5), SELECT(1 shl 8), START(1 shl 9),
    L3(1 shl 10), R3(1 shl 11), HOME(1 shl 16),
}

enum class PadDirection(val mask: Int) { UP(1), RIGHT(2), DOWN(4), LEFT(8) }
