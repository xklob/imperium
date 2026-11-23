package io.klob.imperium.hid

import kotlin.math.abs
import kotlin.math.sign

object HidMouseDescriptor {
    const val REPORT_ID: Int = 1

    const val BUTTON_LEFT: Int = 0x01
    const val BUTTON_RIGHT: Int = 0x02
    const val BUTTON_MIDDLE: Int = 0x04

    /**
     * Standard 3-button + X/Y + vertical Wheel HID Mouse report descriptor.
     * Report format (Report ID = 1, 4 bytes payload):
     *   byte 0: buttons bitmask (bit 0 = left, bit 1 = right, bit 2 = middle)
     *   byte 1: relative X (-127..127)
     *   byte 2: relative Y (-127..127)
     *   byte 3: vertical wheel (-127..127)
     */
    val DESCRIPTOR: ByteArray = byteArrayOf(
        0x05, 0x01,       // Usage Page (Generic Desktop)
        0x09, 0x02,       // Usage (Mouse)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), REPORT_ID.toByte(), // Report ID (1)
        0x09, 0x01,       //   Usage (Pointer)
        0xA1.toByte(), 0x00, //   Collection (Physical)
        0x05, 0x09,       //     Usage Page (Button)
        0x19, 0x01,       //     Usage Minimum (Button 1)
        0x29, 0x03,       //     Usage Maximum (Button 3)
        0x15, 0x00,       //     Logical Minimum (0)
        0x25, 0x01,       //     Logical Maximum (1)
        0x95.toByte(), 0x03, //   Report Count (3)
        0x75, 0x01,       //     Report Size (1)
        0x81.toByte(), 0x02, //   Input (Data, Variable, Absolute)
        0x95.toByte(), 0x01, //   Report Count (1)
        0x75, 0x05,       //     Report Size (5)
        0x81.toByte(), 0x01, //   Input (Constant) - padding
        0x05, 0x01,       //     Usage Page (Generic Desktop)
        0x09, 0x30,       //     Usage (X)
        0x09, 0x31,       //     Usage (Y)
        0x09, 0x38,       //     Usage (Wheel)
        0x15, 0x81.toByte(), //   Logical Minimum (-127)
        0x25, 0x7F,       //     Logical Maximum (127)
        0x75, 0x08,       //     Report Size (8)
        0x95.toByte(), 0x03, //   Report Count (3)
        0x81.toByte(), 0x06, //   Input (Data, Variable, Relative)
        0xC0.toByte(),    //   End Collection
        0xC0.toByte(),    // End Collection
    )

    fun buildReport(buttons: Int, dx: Int, dy: Int, wheel: Int = 0): ByteArray = byteArrayOf(
        (buttons and 0x07).toByte(),
        dx.coerceIn(-127, 127).toByte(),
        dy.coerceIn(-127, 127).toByte(),
        wheel.coerceIn(-127, 127).toByte(),
    )

    /**
     * Splits arbitrarily large relative deltas into a sequence of valid [-127, 127] HID packets
     * so fast flicks never lose distance.
     */
    fun splitDeltas(dx: Int, dy: Int, wheel: Int = 0): List<Triple<Int, Int, Int>> {
        if (dx == 0 && dy == 0 && wheel == 0) return emptyList()
        val result = ArrayList<Triple<Int, Int, Int>>(2)
        var remX = dx
        var remY = dy
        var remW = wheel
        while (remX != 0 || remY != 0 || remW != 0) {
            val stepX = clampStep(remX)
            val stepY = clampStep(remY)
            val stepW = clampStep(remW)
            result.add(Triple(stepX, stepY, stepW))
            remX -= stepX
            remY -= stepY
            remW -= stepW
        }
        return result
    }

    private fun clampStep(value: Int): Int {
        if (abs(value) <= 127) return value
        return 127 * value.sign
    }
}
