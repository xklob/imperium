package io.klob.imperium.input

/**
 * Computes incremental Backspace + inserted text diffs from a rolling hidden IME buffer.
 * Supports single keystrokes, Backspace on an empty buffer (via a 1-space sentinel),
 * Gboard glide/swipe typing, voice-to-text, and IME autocorrect replacements.
 */
object LiveImeDiffEngine {
    const val SENTINEL = " "
    private const val MAX_BUFFER_LEN = 32

    data class DiffResult(
        val backspaces: Int,
        val inserted: String,
        val nextBuffer: String,
    )

    fun compute(previous: String, current: String): DiffResult {
        // User pressed Backspace when only the sentinel remained
        if (current.isEmpty()) {
            val backspaces = if (previous.length > SENTINEL.length) {
                previous.length - SENTINEL.length
            } else {
                1
            }
            return DiffResult(
                backspaces = backspaces,
                inserted = "",
                nextBuffer = SENTINEL,
            )
        }

        // If IME replaced or stripped the leading sentinel, normalize both sides
        val prevText = if (previous.startsWith(SENTINEL)) previous.substring(SENTINEL.length) else previous
        val currText = if (current.startsWith(SENTINEL)) current.substring(SENTINEL.length) else current

        var common = 0
        val minLen = minOf(prevText.length, currText.length)
        while (common < minLen && prevText[common] == currText[common]) {
            common++
        }

        val backspaces = prevText.length - common
        val inserted = currText.substring(common)

        // Reset rolling buffer on newline or when it grows past the cap so it stays tiny
        val nextBuffer = when {
            inserted.contains('\n') -> SENTINEL
            currText.length >= MAX_BUFFER_LEN -> SENTINEL + currText.takeLast(8)
            else -> SENTINEL + currText
        }

        return DiffResult(
            backspaces = backspaces,
            inserted = inserted,
            nextBuffer = nextBuffer,
        )
    }
}
