package io.klob.imperium.input

import io.klob.imperium.hid.HidMouseDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackpadGestureEngineTest {

    private class FakeScheduler : GestureScheduler {
        private data class Task(val fireAt: Long, var cancelled: Boolean = false, val action: () -> Unit)

        var nowMs: Long = 0L
            private set

        private val tasks = mutableListOf<Task>()

        override fun schedule(delayMs: Long, action: () -> Unit): GestureScheduler.Cancellable {
            val task = Task(fireAt = nowMs + delayMs, action = action)
            tasks.add(task)
            return GestureScheduler.Cancellable { task.cancelled = true }
        }

        fun advanceBy(deltaMs: Long) {
            val target = nowMs + deltaMs
            while (true) {
                val next = tasks
                    .filter { !it.cancelled && it.fireAt <= target }
                    .minByOrNull { it.fireAt } ?: break
                nowMs = next.fireAt
                next.cancelled = true
                next.action()
            }
            nowMs = target
        }
    }

    private class RecordingOutput : MouseOutput {
        val events = mutableListOf<String>()
        var totalDx = 0
        var totalDy = 0

        override fun onMove(dx: Int, dy: Int) {
            totalDx += dx
            totalDy += dy
            events.add("MOVE($dx,$dy)")
        }

        override fun onLeftClick() { events.add("LEFT_CLICK") }
        override fun onLeftDown() { events.add("LEFT_DOWN") }
        override fun onLeftUp() { events.add("LEFT_UP") }
        override fun onRightClick() { events.add("RIGHT_CLICK") }
        override fun onDoubleClick() { events.add("DOUBLE_CLICK") }
        override fun onDragLockChanged(locked: Boolean) { events.add("DRAG_LOCK($locked)") }
        override fun onHoldingChanged(holding: Boolean) { events.add("HOLD($holding)") }
    }

    @Test
    fun singleTapEmitsLeftClick() {
        val sched = FakeScheduler()
        val out = RecordingOutput()
        val engine = TrackpadGestureEngine(out, sched, GestureConfig(pointerAcceleration = false))

        engine.onPointerDown(0, 100f, 100f, sched.nowMs)
        sched.advanceBy(60)
        engine.onPointerUp(0, sched.nowMs)
        sched.advanceBy(250)

        assertEquals(listOf("LEFT_CLICK"), out.events)
        assertFalse(engine.isDragLocked)
    }

    @Test
    fun oneFingerPressAndHoldEmitsLeftDownThenUp() {
        val sched = FakeScheduler()
        val out = RecordingOutput()
        val engine = TrackpadGestureEngine(out, sched, GestureConfig(pointerSpeed = 1f, pointerAcceleration = false))

        engine.onPointerDown(0, 100f, 100f, sched.nowMs)
        sched.advanceBy(300)
        assertTrue(engine.isClickHolding)
        engine.onPointerMove(0, 125f, 100f, sched.nowMs)
        engine.onPointerUp(0, sched.nowMs)

        assertFalse(engine.isClickHolding)
        assertEquals(
            listOf("LEFT_DOWN", "HOLD(true)", "MOVE(25,0)", "HOLD(false)", "LEFT_UP"),
            out.events,
        )
    }

    @Test
    fun oneFingerHoverMoveDoesNotClick() {
        val sched = FakeScheduler()
        val out = RecordingOutput()
        val engine = TrackpadGestureEngine(out, sched, GestureConfig(pointerSpeed = 1f, pointerAcceleration = false))

        engine.onPointerDown(0, 100f, 100f, sched.nowMs)
        sched.advanceBy(20)
        engine.onPointerMove(0, 130f, 110f, sched.nowMs)
        sched.advanceBy(400)
        engine.onPointerUp(0, sched.nowMs)

        assertEquals(listOf("MOVE(30,10)"), out.events)
        assertFalse(engine.isClickHolding)
        assertFalse(engine.isDragLocked)
    }

    @Test
    fun oneFingerDoubleTapLocksAndUnlocksDragAndIgnoresSingleTapWhileLocked() {
        val sched = FakeScheduler()
        val out = RecordingOutput()
        val engine = TrackpadGestureEngine(out, sched, GestureConfig(pointerSpeed = 1f, pointerAcceleration = false))

        // Double tap -> lock ON
        engine.onPointerDown(0, 100f, 100f, sched.nowMs)
        sched.advanceBy(40)
        engine.onPointerUp(0, sched.nowMs)
        sched.advanceBy(80)
        engine.onPointerDown(0, 100f, 100f, sched.nowMs)
        sched.advanceBy(40)
        engine.onPointerUp(0, sched.nowMs)
        assertTrue(engine.isDragLocked)

        // Single tap while locked -> ignored
        sched.advanceBy(100)
        engine.onPointerDown(0, 100f, 100f, sched.nowMs)
        sched.advanceBy(40)
        engine.onPointerUp(0, sched.nowMs)
        sched.advanceBy(300)
        assertTrue(engine.isDragLocked)

        // Drag while locked -> moves cursor without releasing
        engine.onPointerDown(0, 100f, 100f, sched.nowMs)
        engine.onPointerMove(0, 150f, 120f, sched.nowMs)
        engine.onPointerUp(0, sched.nowMs)
        assertTrue(engine.isDragLocked)

        // Double tap -> lock OFF
        sched.advanceBy(100)
        engine.onPointerDown(0, 100f, 100f, sched.nowMs)
        sched.advanceBy(40)
        engine.onPointerUp(0, sched.nowMs)
        sched.advanceBy(80)
        engine.onPointerDown(0, 100f, 100f, sched.nowMs)
        sched.advanceBy(40)
        engine.onPointerUp(0, sched.nowMs)
        assertFalse(engine.isDragLocked)

        assertEquals(
            listOf("LEFT_DOWN", "DRAG_LOCK(true)", "MOVE(50,20)", "DRAG_LOCK(false)", "LEFT_UP"),
            out.events,
        )
    }

    @Test
    fun twoFingerTapEmitsRightClick() {
        val sched = FakeScheduler()
        val out = RecordingOutput()
        val engine = TrackpadGestureEngine(out, sched)

        engine.onPointerDown(0, 100f, 100f, sched.nowMs)
        sched.advanceBy(15)
        engine.onPointerDown(1, 200f, 100f, sched.nowMs)
        sched.advanceBy(50)
        engine.onPointerUp(1, sched.nowMs)
        sched.advanceBy(10)
        engine.onPointerUp(0, sched.nowMs)
        sched.advanceBy(300)

        assertEquals(listOf("RIGHT_CLICK"), out.events)
    }

    @Test
    fun twoFingerDoubleTapEmitsDoubleClick() {
        val sched = FakeScheduler()
        val out = RecordingOutput()
        val engine = TrackpadGestureEngine(out, sched)

        // First 2-finger tap
        engine.onPointerDown(0, 100f, 100f, sched.nowMs)
        engine.onPointerDown(1, 200f, 100f, sched.nowMs)
        sched.advanceBy(40)
        engine.onPointerUp(0, sched.nowMs)
        engine.onPointerUp(1, sched.nowMs)

        // Second 2-finger tap within window
        sched.advanceBy(90)
        engine.onPointerDown(0, 100f, 100f, sched.nowMs)
        engine.onPointerDown(1, 200f, 100f, sched.nowMs)
        sched.advanceBy(40)
        engine.onPointerUp(0, sched.nowMs)
        engine.onPointerUp(1, sched.nowMs)
        sched.advanceBy(300)

        assertEquals(listOf("DOUBLE_CLICK"), out.events)
    }

    @Test
    fun edgeScrollAccumulatorRespectsDirectionAndSpeed() {
        val acc = EdgeScrollAccumulator(pixelsPerTick = 20f)
        // Classic Windows: drag down (+40px) -> negative wheel (-2)
        assertEquals(-2, acc.onVerticalDrag(40f, scrollSpeed = 1f, naturalScroll = false))
        // Natural scroll: drag down (+40px) -> positive wheel (+2)
        assertEquals(2, acc.onVerticalDrag(40f, scrollSpeed = 1f, naturalScroll = true))
    }

    @Test
    fun hidDescriptorSplitsLargeDeltasWithoutClipping() {
        val packets = HidMouseDescriptor.splitDeltas(300, -260, 0)
        assertEquals(300, packets.sumOf { it.first })
        assertEquals(-260, packets.sumOf { it.second })
        assertTrue(packets.all { it.first in -127..127 && it.second in -127..127 })
    }
}
