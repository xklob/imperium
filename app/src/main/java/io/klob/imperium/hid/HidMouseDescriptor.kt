package io.klob.imperium.hid

import kotlin.math.abs
import kotlin.math.sign

object HidMouseDescriptor {
    const val REPORT_ID: Int = 1
    const val REPORT_ID_KEYBOARD: Int = 2
    const val REPORT_ID_CONSUMER: Int = 3

    const val BUTTON_LEFT: Int = 0x01
    const val BUTTON_RIGHT: Int = 0x02
    const val BUTTON_MIDDLE: Int = 0x04

    /**
     * Composite USB HID 1.11 Report Descriptor:
     *  - Report ID 1: Mouse (3 buttons + relative X, Y, vertical Wheel) -> 4 bytes
     *  - Report ID 2: Standard 6KRO Keyboard (modifiers + reserved + 6 keys + 5-bit LED out) -> 8 bytes
     *  - Report ID 3: Consumer Control / Media (16-bit usage code) -> 2 bytes
     */
    val DESCRIPTOR: ByteArray = byteArrayOf(
        // === Report ID 1: Mouse ===
        0x05, 0x01,          // Usage Page (Generic Desktop)
        0x09, 0x02,          // Usage (Mouse)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), REPORT_ID.toByte(), // Report ID (1)
        0x09, 0x01,          //   Usage (Pointer)
        0xA1.toByte(), 0x00, //   Collection (Physical)
        0x05, 0x09,          //     Usage Page (Button)
        0x19, 0x01,          //     Usage Minimum (Button 1)
        0x29, 0x03,          //     Usage Maximum (Button 3)
        0x15, 0x00,          //     Logical Minimum (0)
        0x25, 0x01,          //     Logical Maximum (1)
        0x95.toByte(), 0x03, //     Report Count (3)
        0x75, 0x01,          //     Report Size (1)
        0x81.toByte(), 0x02, //     Input (Data, Variable, Absolute)
        0x95.toByte(), 0x01, //     Report Count (1)
        0x75, 0x05,          //     Report Size (5)
        0x81.toByte(), 0x01, //     Input (Constant) - padding
        0x05, 0x01,          //     Usage Page (Generic Desktop)
        0x09, 0x30,          //     Usage (X)
        0x09, 0x31,          //     Usage (Y)
        0x09, 0x38,          //     Usage (Wheel)
        0x15, 0x81.toByte(), //     Logical Minimum (-127)
        0x25, 0x7F,          //     Logical Maximum (127)
        0x75, 0x08,          //     Report Size (8)
        0x95.toByte(), 0x03, //     Report Count (3)
        0x81.toByte(), 0x06, //     Input (Data, Variable, Relative)
        0xC0.toByte(),       //   End Collection
        0xC0.toByte(),       // End Collection

        // === Report ID 2: Keyboard ===
        0x05, 0x01,          // Usage Page (Generic Desktop)
        0x09, 0x06,          // Usage (Keyboard)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD.toByte(), // Report ID (2)
        0x05, 0x07,          //   Usage Page (Keyboard/Keypad)
        0x19, 0xE0.toByte(), //   Usage Minimum (Left Control)
        0x29, 0xE7.toByte(), //   Usage Maximum (Right GUI)
        0x15, 0x00,          //   Logical Minimum (0)
        0x25, 0x01,          //   Logical Maximum (1)
        0x75, 0x01,          //   Report Size (1)
        0x95.toByte(), 0x08, //   Report Count (8)
        0x81.toByte(), 0x02, //   Input (Data, Variable, Absolute) - Modifier byte
        0x95.toByte(), 0x01, //   Report Count (1)
        0x75, 0x08,          //   Report Size (8)
        0x81.toByte(), 0x01, //   Input (Constant) - Reserved byte
        0x95.toByte(), 0x05, //   Report Count (5)
        0x75, 0x01,          //   Report Size (1)
        0x05, 0x08,          //   Usage Page (LEDs)
        0x19, 0x01,          //   Usage Minimum (Num Lock)
        0x29, 0x05,          //   Usage Maximum (Kana)
        0x91.toByte(), 0x02, //   Output (Data, Variable, Absolute) - LED report
        0x95.toByte(), 0x01, //   Report Count (1)
        0x75, 0x03,          //   Report Size (3)
        0x91.toByte(), 0x01, //   Output (Constant) - LED padding
        0x95.toByte(), 0x06, //   Report Count (6)
        0x75, 0x08,          //   Report Size (8)
        0x15, 0x00,          //   Logical Minimum (0)
        0x25, 0x65,          //   Logical Maximum (101)
        0x05, 0x07,          //   Usage Page (Keyboard/Keypad)
        0x19, 0x00,          //   Usage Minimum (0)
        0x29, 0x65,          //   Usage Maximum (101)
        0x81.toByte(), 0x00, //   Input (Data, Array) - 6 keycodes
        0xC0.toByte(),       // End Collection

        // === Report ID 3: Consumer Control (Media Keys) ===
        0x05, 0x0C,          // Usage Page (Consumer Devices)
        0x09, 0x01,          // Usage (Consumer Control)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), REPORT_ID_CONSUMER.toByte(), // Report ID (3)
        0x15, 0x00,          //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x03, // Logical Maximum (0x03FF)
        0x19, 0x00,          //   Usage Minimum (0)
        0x2A, 0xFF.toByte(), 0x03, // Usage Maximum (0x03FF)
        0x75, 0x10,          //   Report Size (16)
        0x95.toByte(), 0x01, //   Report Count (1)
        0x81.toByte(), 0x00, //   Input (Data, Array)
        0xC0.toByte(),       // End Collection
    )

    fun buildReport(buttons: Int, dx: Int, dy: Int, wheel: Int = 0): ByteArray = byteArrayOf(
        (buttons and 0x07).toByte(),
        dx.coerceIn(-127, 127).toByte(),
        dy.coerceIn(-127, 127).toByte(),
        wheel.coerceIn(-127, 127).toByte(),
    )

    fun buildKeyboardReport(modifier: Int, keyCode: Int = 0): ByteArray = byteArrayOf(
        (modifier and 0xFF).toByte(),
        0x00,
        (keyCode and 0xFF).toByte(),
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
    )

    fun buildConsumerReport(usageCode: Int): ByteArray = byteArrayOf(
        (usageCode and 0xFF).toByte(),
        ((usageCode shr 8) and 0xFF).toByte(),
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
