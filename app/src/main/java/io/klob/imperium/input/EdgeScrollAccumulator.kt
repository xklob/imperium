package io.klob.imperium.input

import kotlin.math.truncate

/**
 * Converts vertical finger drag on the edge scroll bar into discrete HID mouse wheel ticks.
 */
class EdgeScrollAccumulator(
    private val pixelsPerTick: Float = 28f,
) {
    private var remainder: Float = 0f

    fun reset() {
        remainder = 0f
    }

    /**
     * @param deltaY positive when finger drags downward on screen
     * @param scrollSpeed multiplier from settings (e.g. 0.25..3.0)
     * @param naturalScroll false = classic Windows wheel (drag down -> scroll down -> negative HID wheel);
     *                      true = natural touch scroll (drag down -> scroll up -> positive HID wheel)
     */
    fun onVerticalDrag(deltaY: Float, scrollSpeed: Float, naturalScroll: Boolean): Int {
        val direction = if (naturalScroll) 1f else -1f
        remainder += (deltaY / pixelsPerTick) * scrollSpeed * direction
        val ticks = truncate(remainder).toInt()
        if (ticks != 0) {
            remainder -= ticks
        }
        return ticks
    }
}
