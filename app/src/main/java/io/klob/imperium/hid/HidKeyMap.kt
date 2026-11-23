package io.klob.imperium.hid

data class HidKeyStroke(
    val modifier: Int = 0,
    val usageId: Int,
)

object HidKeyMap {
    const val MOD_LEFT_CTRL = 0x01
    const val MOD_LEFT_SHIFT = 0x02
    const val MOD_LEFT_ALT = 0x04
    const val MOD_LEFT_GUI = 0x08

    // Common non-printable USB HID Keyboard Usage IDs (Usage Page 0x07)
    const val KEY_ENTER = 0x28
    const val KEY_ESC = 0x29
    const val KEY_BACKSPACE = 0x2A
    const val KEY_TAB = 0x2B
    const val KEY_SPACE = 0x2C
    const val KEY_CAPS_LOCK = 0x39
    const val KEY_F1 = 0x3A
    const val KEY_F2 = 0x3B
    const val KEY_F3 = 0x3C
    const val KEY_F4 = 0x3D
    const val KEY_F5 = 0x3E
    const val KEY_F6 = 0x3F
    const val KEY_F7 = 0x40
    const val KEY_F8 = 0x41
    const val KEY_F9 = 0x42
    const val KEY_F10 = 0x43
    const val KEY_F11 = 0x44
    const val KEY_F12 = 0x45
    const val KEY_INSERT = 0x49
    const val KEY_HOME = 0x4A
    const val KEY_PAGE_UP = 0x4B
    const val KEY_DELETE = 0x4C
    const val KEY_END = 0x4D
    const val KEY_PAGE_DOWN = 0x4E
    const val KEY_RIGHT = 0x4F
    const val KEY_LEFT = 0x50
    const val KEY_DOWN = 0x51
    const val KEY_UP = 0x52

    // USB HID Consumer Page (0x0C) media & volume usage codes
    const val CONSUMER_NEXT_TRACK = 0x00B5
    const val CONSUMER_PREV_TRACK = 0x00B6
    const val CONSUMER_PLAY_PAUSE = 0x00CD
    const val CONSUMER_MUTE = 0x00E2
    const val CONSUMER_VOL_UP = 0x00E9
    const val CONSUMER_VOL_DOWN = 0x00EA

    /**
     * Maps a US QWERTY character to its USB HID Keyboard Usage ID and shift modifier.
     * Returns null for characters outside standard US ASCII + newline/tab.
     */
    fun forChar(ch: Char): HidKeyStroke? = when (ch) {
        in 'a'..'z' -> HidKeyStroke(0, 0x04 + (ch - 'a'))
        in 'A'..'Z' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x04 + (ch - 'A'))
        '1' -> HidKeyStroke(0, 0x1E)
        '2' -> HidKeyStroke(0, 0x1F)
        '3' -> HidKeyStroke(0, 0x20)
        '4' -> HidKeyStroke(0, 0x21)
        '5' -> HidKeyStroke(0, 0x22)
        '6' -> HidKeyStroke(0, 0x23)
        '7' -> HidKeyStroke(0, 0x24)
        '8' -> HidKeyStroke(0, 0x25)
        '9' -> HidKeyStroke(0, 0x26)
        '0' -> HidKeyStroke(0, 0x27)
        '!' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x1E)
        '@' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x1F)
        '#' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x20)
        '$' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x21)
        '%' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x22)
        '^' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x23)
        '&' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x24)
        '*' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x25)
        '(' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x26)
        ')' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x27)
        '\n', '\r' -> HidKeyStroke(0, KEY_ENTER)
        '\t' -> HidKeyStroke(0, KEY_TAB)
        ' ' -> HidKeyStroke(0, KEY_SPACE)
        '-' -> HidKeyStroke(0, 0x2D)
        '_' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x2D)
        '=' -> HidKeyStroke(0, 0x2E)
        '+' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x2E)
        '[' -> HidKeyStroke(0, 0x2F)
        '{' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x2F)
        ']' -> HidKeyStroke(0, 0x30)
        '}' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x30)
        '\\' -> HidKeyStroke(0, 0x31)
        '|' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x31)
        ';' -> HidKeyStroke(0, 0x33)
        ':' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x33)
        '\'' -> HidKeyStroke(0, 0x34)
        '"' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x34)
        '`' -> HidKeyStroke(0, 0x35)
        '~' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x35)
        ',' -> HidKeyStroke(0, 0x36)
        '<' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x36)
        '.' -> HidKeyStroke(0, 0x37)
        '>' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x37)
        '/' -> HidKeyStroke(0, 0x38)
        '?' -> HidKeyStroke(MOD_LEFT_SHIFT, 0x38)
        else -> null
    }
}
