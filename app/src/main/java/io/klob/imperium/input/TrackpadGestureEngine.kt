package io.klob.imperium.input

import kotlin.math.hypot
import kotlin.math.truncate

interface GestureScheduler {
    fun interface Cancellable {
        fun cancel()
    }

    fun schedule(delayMs: Long, action: () -> Unit): Cancellable
}

interface MouseOutput {
    fun onMove(dx: Int, dy: Int)
    fun onLeftClick()
    fun onLeftDown()
    fun onLeftUp()
    fun onRightClick()
    fun onDoubleClick()
    fun onDragLockChanged(locked: Boolean) {}
    fun onHoldingChanged(holding: Boolean) {}
}

data class GestureConfig(
    val touchSlopPx: Float = 14f,
    val holdThresholdMs: Long = 280L,
    val singleTapDoubleTapWindowMs: Long = 230L,
    val twoFingerTapMaxDurationMs: Long = 350L,
    val twoFingerDoubleTapWindowMs: Long = 260L,
    val pointerSpeed: Float = 1.5f,
    val pointerAcceleration: Boolean = true,
)

class TrackpadGestureEngine(
    private val output: MouseOutput,
    private val scheduler: GestureScheduler,
    initialConfig: GestureConfig = GestureConfig(),
) {
    private enum class GesturePhase {
        IDLE,
        ONE_FINGER_PENDING,
        ONE_FINGER_MOVING,
        ONE_FINGER_HOLDING,
        TWO_FINGER_PENDING,
        MULTI_FINGER_IGNORED,
    }

    private data class PointerPos(var x: Float, var y: Float, val downX: Float, val downY: Float)

    var config: GestureConfig = initialConfig

    var isDragLocked: Boolean = false
        private set

    var isClickHolding: Boolean = false
        private set

    private var phase: GesturePhase = GesturePhase.IDLE
    private val pointers = LinkedHashMap<Int, PointerPos>()
    private var primaryPointerId: Int? = null
    private var gestureStartTimeMs: Long = 0L
    private var twoFingerMoved: Boolean = false
    private var twoFingerMaxCount: Int = 0

    private var holdTimer: GestureScheduler.Cancellable? = null
    private var pendingSingleTapTimer: GestureScheduler.Cancellable? = null
    private var pendingLockedTapTimer: GestureScheduler.Cancellable? = null
    private var pendingTwoFingerTapTimer: GestureScheduler.Cancellable? = null

    private var accumX: Float = 0f
    private var accumY: Float = 0f

    fun onPointerDown(pointerId: Int, x: Float, y: Float, timeMs: Long) {
        pointers[pointerId] = PointerPos(x = x, y = y, downX = x, downY = y)

        if (pointers.size == 1) {
            primaryPointerId = pointerId
            gestureStartTimeMs = timeMs
            twoFingerMoved = false
            twoFingerMaxCount = 1
            phase = GesturePhase.ONE_FINGER_PENDING

            // Start click-and-hold timer only when unlocked and not mid-double-tap
            if (!isDragLocked && pendingSingleTapTimer == null) {
                startHoldTimer()
            }
            return
        }

        if (pointers.size == 2) {
            twoFingerMaxCount = 2
            if (phase == GesturePhase.ONE_FINGER_PENDING) {
                cancelHoldTimer()
                flushPendingSingleTapIfUnlocked()
                cancelPendingLockedTap()
                phase = GesturePhase.TWO_FINGER_PENDING
            } else {
                // Already moving or holding with finger 1; ignore extra fingers
                phase = GesturePhase.MULTI_FINGER_IGNORED
            }
            return
        }

        // 3+ fingers: ignore as gesture, cancel timers
        twoFingerMaxCount = maxOf(twoFingerMaxCount, pointers.size)
        cancelHoldTimer()
        phase = GesturePhase.MULTI_FINGER_IGNORED
    }

    fun onPointerMove(pointerId: Int, x: Float, y: Float, timeMs: Long) {
        val ptr = pointers[pointerId] ?: return
        val rawDx = x - ptr.x
        val rawDy = y - ptr.y
        ptr.x = x
        ptr.y = y

        val totalDist = hypot((x - ptr.downX).toDouble(), (y - ptr.downY).toDouble()).toFloat()

        when (phase) {
            GesturePhase.ONE_FINGER_PENDING -> {
                if (pointerId == primaryPointerId && totalDist > config.touchSlopPx) {
                    cancelHoldTimer()
                    flushPendingSingleTapIfUnlocked()
                    cancelPendingLockedTap()
                    phase = GesturePhase.ONE_FINGER_MOVING
                    emitScaledMove(rawDx, rawDy)
                }
            }

            GesturePhase.ONE_FINGER_MOVING,
            GesturePhase.ONE_FINGER_HOLDING -> {
                if (pointerId == primaryPointerId) {
                    emitScaledMove(rawDx, rawDy)
                }
            }

            GesturePhase.TWO_FINGER_PENDING -> {
                if (totalDist > config.touchSlopPx) {
                    twoFingerMoved = true
                }
            }

            GesturePhase.IDLE,
            GesturePhase.MULTI_FINGER_IGNORED -> Unit
        }
    }

    fun onPointerUp(pointerId: Int, timeMs: Long) {
        pointers.remove(pointerId)

        if (pointers.isNotEmpty()) {
            if (pointerId == primaryPointerId) {
                primaryPointerId = pointers.keys.firstOrNull()
            }
            return
        }

        // All fingers lifted
        cancelHoldTimer()
        val elapsed = timeMs - gestureStartTimeMs
        val finishedPhase = phase
        phase = GesturePhase.IDLE
        primaryPointerId = null

        when (finishedPhase) {
            GesturePhase.ONE_FINGER_HOLDING -> {
                isClickHolding = false
                output.onHoldingChanged(false)
                if (!isDragLocked) {
                    output.onLeftUp()
                }
            }

            GesturePhase.ONE_FINGER_PENDING -> {
                handleOneFingerTap()
            }

            GesturePhase.TWO_FINGER_PENDING -> {
                if (!twoFingerMoved && twoFingerMaxCount == 2 && elapsed <= config.twoFingerTapMaxDurationMs) {
                    handleTwoFingerTap()
                }
            }

            GesturePhase.ONE_FINGER_MOVING,
            GesturePhase.MULTI_FINGER_IGNORED,
            GesturePhase.IDLE -> Unit
        }
    }

    fun onCancel() {
        cancelAllTimers()
        pointers.clear()
        primaryPointerId = null
        phase = GesturePhase.IDLE
        accumX = 0f
        accumY = 0f

        if (isClickHolding) {
            isClickHolding = false
            output.onHoldingChanged(false)
            if (!isDragLocked) {
                output.onLeftUp()
            }
        }
    }

    fun clearDragLock() {
        cancelAllTimers()
        if (isClickHolding) {
            isClickHolding = false
            output.onHoldingChanged(false)
        }
        if (isDragLocked) {
            isDragLocked = false
            output.onDragLockChanged(false)
            output.onLeftUp()
        }
    }

    private fun handleOneFingerTap() {
        if (isDragLocked) {
            // Single tap is ignored while locked; double-tap releases Drag Lock
            if (pendingLockedTapTimer != null) {
                cancelPendingLockedTap()
                isDragLocked = false
                output.onDragLockChanged(false)
                output.onLeftUp()
            } else {
                pendingLockedTapTimer = scheduler.schedule(config.singleTapDoubleTapWindowMs) {
                    pendingLockedTapTimer = null
                }
            }
            return
        }

        // Unlocked: single tap = left click; double-tap = engage Drag Lock
        if (pendingSingleTapTimer != null) {
            cancelPendingSingleTap()
            isDragLocked = true
            output.onLeftDown()
            output.onDragLockChanged(true)
        } else {
            pendingSingleTapTimer = scheduler.schedule(config.singleTapDoubleTapWindowMs) {
                pendingSingleTapTimer = null
                output.onLeftClick()
            }
        }
    }

    private fun handleTwoFingerTap() {
        if (pendingTwoFingerTapTimer != null) {
            cancelPendingTwoFingerTap()
            output.onDoubleClick()
        } else {
            pendingTwoFingerTapTimer = scheduler.schedule(config.twoFingerDoubleTapWindowMs) {
                pendingTwoFingerTapTimer = null
                output.onRightClick()
            }
        }
    }

    private fun startHoldTimer() {
        cancelHoldTimer()
        holdTimer = scheduler.schedule(config.holdThresholdMs) {
            holdTimer = null
            if (phase == GesturePhase.ONE_FINGER_PENDING && pointers.size == 1 && !isDragLocked) {
                phase = GesturePhase.ONE_FINGER_HOLDING
                isClickHolding = true
                output.onLeftDown()
                output.onHoldingChanged(true)
            }
        }
    }

    private fun emitScaledMove(rawDx: Float, rawDy: Float) {
        val dist = hypot(rawDx.toDouble(), rawDy.toDouble()).toFloat()
        val accel = if (config.pointerAcceleration) {
            // Gentle curve: 1.0x for fine precision, up to 2.0x for fast flicks
            (1.0f + (dist / 28f).coerceIn(0f, 1.0f))
        } else {
            1.0f
        }
        val gain = config.pointerSpeed * accel
        accumX += rawDx * gain
        accumY += rawDy * gain

        val sendX = truncate(accumX).toInt()
        val sendY = truncate(accumY).toInt()
        if (sendX != 0 || sendY != 0) {
            accumX -= sendX
            accumY -= sendY
            output.onMove(sendX, sendY)
        }
    }

    private fun flushPendingSingleTapIfUnlocked() {
        if (pendingSingleTapTimer != null) {
            cancelPendingSingleTap()
            if (!isDragLocked) {
                output.onLeftClick()
            }
        }
    }

    private fun cancelHoldTimer() {
        holdTimer?.cancel()
        holdTimer = null
    }

    private fun cancelPendingSingleTap() {
        pendingSingleTapTimer?.cancel()
        pendingSingleTapTimer = null
    }

    private fun cancelPendingLockedTap() {
        pendingLockedTapTimer?.cancel()
        pendingLockedTapTimer = null
    }

    private fun cancelPendingTwoFingerTap() {
        pendingTwoFingerTapTimer?.cancel()
        pendingTwoFingerTapTimer = null
    }

    private fun cancelAllTimers() {
        cancelHoldTimer()
        cancelPendingSingleTap()
        cancelPendingLockedTap()
        cancelPendingTwoFingerTap()
    }
}
