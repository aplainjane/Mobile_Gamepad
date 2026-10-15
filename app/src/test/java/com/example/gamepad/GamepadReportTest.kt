package com.example.gamepad

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class GamepadReportTest {
    private fun word(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8)).toShort().toInt()
    private fun buttons(bytes: ByteArray): Int = (bytes[0].toInt() and 255) or
        ((bytes[1].toInt() and 255) shl 8) or ((bytes[2].toInt() and 255) shl 16)

    @Test fun neutralHasFourCenteredSticksReleasedTriggersAndHat() {
        assertArrayEquals(ByteArray(14).also { it[13] = 8 }, GamepadReport().toBytes())
    }

    @Test fun selectStartAndStickClicksOccupyDistinctConventionalButtonIndices() {
        val expected = mapOf(PadButton.A to 0, PadButton.B to 1, PadButton.X to 2, PadButton.Y to 3,
            PadButton.L1 to 4, PadButton.R1 to 5, PadButton.SELECT to 8, PadButton.START to 9,
            PadButton.L3 to 10, PadButton.R3 to 11, PadButton.HOME to 16)
        expected.forEach { (button, index) ->
            assertEquals(button.name, 1 shl index, buttons(GamepadReport().withButton(button, true).toBytes()))
        }
    }

    @Test fun simultaneousButtonsDoNotReleaseEachOther() {
        val held = GamepadReport().withButton(PadButton.A, true).withButton(PadButton.R3, true)
        assertEquals(0x801, buttons(held.toBytes()))
        assertEquals(0x800, buttons(held.withButton(PadButton.A, false).toBytes()))
    }

    @Test fun signedSixteenBitSticksAreClampedAndLittleEndian() {
        val bytes = GamepadReport(leftX = -2f, leftY = 2f, rightX = -1f, rightY = 1f).toBytes()
        assertEquals(14, bytes.size)
        assertEquals(-32767, word(bytes, 3)); assertEquals(32767, word(bytes, 5))
        assertEquals(-32767, word(bytes, 7)); assertEquals(32767, word(bytes, 9))
        assertEquals(0, bytes[11].toInt()); assertEquals(0, bytes[12].toInt())
    }

    @Test fun independentTriggersCannotMoveEitherStickOrPressSystemButtons() {
        val state = GamepadReport(leftX = .4f, rightY = -.6f)
        val neutral = state.toBytes()
        val both = state.copy(leftTrigger = 300, rightTrigger = 255).toBytes()
        assertArrayEquals(neutral.copyOfRange(3, 11), both.copyOfRange(3, 11))
        assertEquals(255, both[11].toInt() and 255); assertEquals(255, both[12].toInt() and 255)
        assertEquals(0xC0, buttons(both))
        val rightOnly = state.copy(leftTrigger = -1, rightTrigger = 1).toBytes()
        assertEquals(0, rightOnly[11].toInt()); assertEquals(1, rightOnly[12].toInt())
        assertEquals(0x80, buttons(rightOnly))
    }

    @Test fun rightStickMovementCannotChangeTriggersOrButtons() {
        val state = GamepadReport(leftTrigger = 255).withButton(PadButton.START, true)
        val still = state.toBytes()
        val moved = state.copy(rightX = 1f, rightY = -1f).toBytes()
        assertEquals(buttons(still), buttons(moved))
        assertArrayEquals(still.copyOfRange(11, 14), moved.copyOfRange(11, 14))
        assertEquals(32767, word(moved, 7)); assertEquals(-32767, word(moved, 9))
    }

    @Test fun allEightDirectionsHaveConsistentHatAndRawButtons() {
        val masks = listOf(1, 3, 2, 6, 4, 12, 8, 9)
        val expectedButtons = listOf(0x1000, 0x9000, 0x8000, 0xA000, 0x2000, 0x6000, 0x4000, 0x5000)
        masks.forEachIndexed { expected, directions ->
            val bytes = GamepadReport(directions = directions).toBytes()
            assertEquals(expected, bytes.last().toInt())
            assertEquals(expectedButtons[expected], buttons(bytes))
        }
    }

    @Test fun oppositeDirectionsCancelWithoutLosingAnUnrelatedHeldButton() {
        val state = GamepadReport().withButton(PadButton.SELECT, true)
            .withDirection(PadDirection.UP, true).withDirection(PadDirection.DOWN, true)
        assertEquals(8, state.toBytes().last().toInt()); assertEquals(0x100, buttons(state.toBytes()))
        val downOnly = state.withDirection(PadDirection.UP, false).toBytes()
        assertEquals(4, downOnly.last().toInt()); assertEquals(0x2100, buttons(downOnly))
    }

    @Test fun hostDescriptorDeclaresFourStickAxesBeforeUnsignedTriggers() {
        val fields = descriptorFields()
        assertEquals(112, fields.sumOf { it.size * it.count })
        assertEquals(1, fields.map { it.id }.distinct().single())
        val data = fields.filter { it.flags and 1 == 0 }
        assertEquals(4, data.size)
        assertEquals((1..17).toList(), data[0].usages); assertEquals(9, data[0].page)
        assertEquals(listOf(0x30, 0x31, 0x32, 0x33), data[1].usages)
        assertEquals(24, data[1].offset); assertEquals(16, data[1].size)
        assertEquals(-32767, data[1].min); assertEquals(32767, data[1].max)
        assertEquals(listOf(0x34, 0x35), data[2].usages)
        assertEquals(88, data[2].offset); assertEquals(8, data[2].size)
        assertEquals(0, data[2].min); assertEquals(255, data[2].max)
        assertEquals(listOf(0x39), data[3].usages)
        assertEquals(104, data[3].offset); assertEquals(0x42, data[3].flags)
    }

    private data class Field(val id: Int, val page: Int, val usages: List<Int>, val size: Int,
        val count: Int, val min: Int, val max: Int, val flags: Int, val offset: Int)

    /** Read the HID items like a host would, independent of the payload encoder. */
    private fun descriptorFields(): List<Field> {
        val bytes = GamepadReport.DESCRIPTOR
        val result = mutableListOf<Field>(); val usages = mutableListOf<Int>()
        var index = 0; var page = 0; var size = 0; var count = 0; var min = 0; var max = 0
        var id = 0; var usageMin: Int? = null; var usageMax: Int? = null; var offset = 0
        while (index < bytes.size) {
            val prefix = bytes[index++].toInt() and 255
            val length = if (prefix and 3 == 3) 4 else prefix and 3
            var value = 0
            repeat(length) { value = value or ((bytes[index++].toInt() and 255) shl (it * 8)) }
            val signed = if (length == 0) 0 else (value shl (32 - length * 8)) shr (32 - length * 8)
            when ((prefix shr 2) and 3) {
                1 -> when (prefix shr 4) {
                    0 -> page = value
                    1 -> min = signed
                    2 -> max = if (min < 0) signed else value
                    7 -> size = value
                    8 -> id = value
                    9 -> count = value
                }
                2 -> when (prefix shr 4) {
                    0 -> usages.add(value)
                    1 -> usageMin = value
                    2 -> usageMax = value
                }
                0 -> {
                    if (prefix shr 4 == 8) {
                        val range = usageMin?.let { first -> usageMax?.let { last -> (first..last).toList() } }
                        result.add(Field(id, page, range ?: usages.toList(), size, count, min, max, value, offset))
                        offset += size * count
                    }
                    usages.clear(); usageMin = null; usageMax = null
                }
            }
        }
        return result
    }
}
