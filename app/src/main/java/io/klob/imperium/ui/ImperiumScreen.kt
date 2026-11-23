package io.klob.imperium.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardHide
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.klob.imperium.hid.HidConnectionState
import io.klob.imperium.hid.HidKeyMap
import io.klob.imperium.hid.HidUiState
import io.klob.imperium.input.EdgeScrollAccumulator
import io.klob.imperium.input.TrackpadGestureEngine
import io.klob.imperium.settings.ImperiumSettings
import io.klob.imperium.settings.ScrollBarSide
import io.klob.imperium.ui.theme.LocalKlobColors

@Composable
fun ImperiumScreen(
    hidState: HidUiState,
    settings: ImperiumSettings,
    isDragLocked: Boolean,
    isHolding: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    gestureEngine: TrackpadGestureEngine,
    onScrollWheel: (Int) -> Unit,
    onSendText: (text: String, modifier: Int) -> Unit,
    onSendBackspaces: (count: Int, modifier: Int) -> Unit,
    onSendKeyTap: (usageId: Int, modifier: Int) -> Unit,
    onSendConsumerTap: (usageCode: Int) -> Unit,
    onPasteClipboard: () -> Unit,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onRequestUnrestrictedBattery: () -> Unit,
    onConnectHost: (String) -> Unit,
    onDisconnect: () -> Unit,
    onRefreshHosts: () -> Unit,
    onUpdateSettings: ((ImperiumSettings) -> ImperiumSettings) -> Unit,
) {
    val klob = LocalKlobColors.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showHostsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var keyboardOpen by remember { mutableStateOf(false) }

    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragLocked) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "dragLockGlow",
    )

    val isConnected = hidState.connectionState == HidConnectionState.CONNECTED

    LaunchedEffect(isConnected) {
        if (!isConnected && keyboardOpen) {
            keyboardOpen = false
            keyboardController?.hide()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(klob.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .drawWithContent {
                drawContent()
                if (glowAlpha > 0.01f) {
                    val red = klob.dragLockRed
                    // Layered inward neon glow + crisp outer border
                    val layers = listOf(
                        22.dp.toPx() to 0.10f,
                        14.dp.toPx() to 0.18f,
                        8.dp.toPx() to 0.34f,
                        3.dp.toPx() to 0.95f,
                    )
                    for ((strokePx, alpha) in layers) {
                        val half = strokePx / 2f
                        drawRect(
                            color = red.copy(alpha = alpha * glowAlpha),
                            topLeft = Offset(half, half),
                            size = Size(size.width - strokePx, size.height - strokePx),
                            style = Stroke(width = strokePx),
                        )
                    }
                }
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                hidState = hidState,
                isDragLocked = isDragLocked,
                onConsumerTap = { code ->
                    if (settings.hapticsEnabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    }
                    onSendConsumerTap(code)
                },
                onOpenSettings = {
                    keyboardOpen = false
                    keyboardController?.hide()
                    showSettingsDialog = true
                },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(klob.line),
            )

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (settings.scrollBarSide == ScrollBarSide.LEFT) {
                    EdgeScrollColumn(
                        scrollSpeed = settings.scrollSpeed,
                        naturalScroll = settings.naturalScroll,
                        enabled = isConnected,
                        keyboardOpen = keyboardOpen,
                        onScrollWheel = { ticks ->
                            if (settings.hapticsEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            onScrollWheel(ticks)
                        },
                        onToggleKeyboard = {
                            if (!isConnected) return@EdgeScrollColumn
                            val next = !keyboardOpen
                            keyboardOpen = next
                            if (!next) keyboardController?.hide()
                        },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(klob.line),
                    )
                }

                TrackpadSurface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    hidState = hidState,
                    settings = settings,
                    isDragLocked = isDragLocked,
                    isHolding = isHolding,
                    gestureEngine = gestureEngine,
                    onRequestPermissions = onRequestPermissions,
                    onEnableBluetooth = onEnableBluetooth,
                    onOpenHosts = {
                        onRefreshHosts()
                        showHostsDialog = true
                    },
                    onConnectHost = onConnectHost,
                )

                if (settings.scrollBarSide == ScrollBarSide.RIGHT) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(klob.line),
                    )
                    EdgeScrollColumn(
                        scrollSpeed = settings.scrollSpeed,
                        naturalScroll = settings.naturalScroll,
                        enabled = isConnected,
                        keyboardOpen = keyboardOpen,
                        onScrollWheel = { ticks ->
                            if (settings.hapticsEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            onScrollWheel(ticks)
                        },
                        onToggleKeyboard = {
                            if (!isConnected) return@EdgeScrollColumn
                            val next = !keyboardOpen
                            keyboardOpen = next
                            if (!next) keyboardController?.hide()
                        },
                    )
                }
            }

            if (keyboardOpen && isConnected) {
                KeyboardDock(
                    onSendText = onSendText,
                    onSendBackspaces = onSendBackspaces,
                    onSendKeyTap = { usageId, mod ->
                        if (settings.hapticsEnabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        onSendKeyTap(usageId, mod)
                    },
                    onPasteClipboard = {
                        if (settings.hapticsEnabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        }
                        onPasteClipboard()
                    },
                )
            }
        }
    }

    if (showHostsDialog) {
        HostsDialog(
            hidState = hidState,
            onDismiss = { showHostsDialog = false },
            onRequestPermissions = onRequestPermissions,
            onEnableBluetooth = onEnableBluetooth,
            onMakeDiscoverable = onMakeDiscoverable,
            onRefresh = onRefreshHosts,
            onConnect = { address ->
                onConnectHost(address)
                showHostsDialog = false
            },
            onDisconnect = onDisconnect,
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = settings,
            hidState = hidState,
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
            onOpenHosts = {
                showSettingsDialog = false
                onRefreshHosts()
                showHostsDialog = true
            },
            onRequestUnrestrictedBattery = onRequestUnrestrictedBattery,
            onUpdateSettings = onUpdateSettings,
            onDismiss = { showSettingsDialog = false },
        )
    }
}

@Composable
private fun TopBar(
    hidState: HidUiState,
    isDragLocked: Boolean,
    onConsumerTap: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val klob = LocalKlobColors.current
    val isConnected = hidState.connectionState == HidConnectionState.CONNECTED
    val statusColor = when {
        isDragLocked -> klob.dragLockRed
        isConnected -> klob.accent
        hidState.connectionState == HidConnectionState.CONNECTING -> klob.warning
        else -> klob.muted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isConnected) {
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(7.dp)
                    .background(statusColor),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hidState.connectionState == HidConnectionState.CONNECTING) "connecting" else "offline",
                style = MaterialTheme.typography.bodyMedium,
                color = klob.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TopIconButton("Previous track", false, { onConsumerTap(HidKeyMap.CONSUMER_PREV_TRACK) }) {
                    Icon(Icons.Outlined.SkipPrevious, contentDescription = null, tint = klob.text, modifier = Modifier.size(22.dp))
                }
                TopIconButton("Play or pause", false, { onConsumerTap(HidKeyMap.CONSUMER_PLAY_PAUSE) }) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = klob.text, modifier = Modifier.size(22.dp))
                }
                TopIconButton("Next track", false, { onConsumerTap(HidKeyMap.CONSUMER_NEXT_TRACK) }) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = null, tint = klob.text, modifier = Modifier.size(22.dp))
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(1.dp)
                        .height(22.dp)
                        .background(klob.line),
                )

                TopIconButton("Volume down", false, { onConsumerTap(HidKeyMap.CONSUMER_VOL_DOWN) }) {
                    Icon(Icons.AutoMirrored.Outlined.VolumeDown, contentDescription = null, tint = klob.text, modifier = Modifier.size(22.dp))
                }
                TopIconButton("Volume up", false, { onConsumerTap(HidKeyMap.CONSUMER_VOL_UP) }) {
                    Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, tint = klob.text, modifier = Modifier.size(22.dp))
                }
            }
        }

        TopIconButton(
            contentDescription = "Settings",
            active = false,
            onClick = onOpenSettings,
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                tint = klob.text,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun TopIconButton(
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val klob = LocalKlobColors.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(if (active) klob.accentSoft else Color.Transparent)
            .border(1.dp, if (active) klob.accentBorder else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun TrackpadSurface(
    modifier: Modifier,
    hidState: HidUiState,
    settings: ImperiumSettings,
    isDragLocked: Boolean,
    isHolding: Boolean,
    gestureEngine: TrackpadGestureEngine,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenHosts: () -> Unit,
    onConnectHost: (String) -> Unit,
) {
    val klob = LocalKlobColors.current
    val isConnected = hidState.connectionState == HidConnectionState.CONNECTED

    Box(
        modifier = modifier
            .background(klob.surface)
            .pointerInput(isConnected) {
                if (!isConnected) return@pointerInput
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                for (change in event.changes) {
                                    if (change.pressed && !change.previousPressed) {
                                        gestureEngine.onPointerDown(
                                            pointerId = change.id.value.toInt(),
                                            x = change.position.x,
                                            y = change.position.y,
                                            timeMs = change.uptimeMillis,
                                        )
                                        change.consume()
                                    }
                                }
                            }

                            PointerEventType.Move -> {
                                for (change in event.changes) {
                                    if (change.pressed) {
                                        gestureEngine.onPointerMove(
                                            pointerId = change.id.value.toInt(),
                                            x = change.position.x,
                                            y = change.position.y,
                                            timeMs = change.uptimeMillis,
                                        )
                                        change.consume()
                                    }
                                }
                            }

                            PointerEventType.Release -> {
                                for (change in event.changes) {
                                    if (!change.pressed && change.previousPressed) {
                                        gestureEngine.onPointerUp(
                                            pointerId = change.id.value.toInt(),
                                            timeMs = change.uptimeMillis,
                                        )
                                        change.consume()
                                    }
                                }
                            }

                            else -> Unit
                        }

                        if (event.changes.none { it.pressed }) {
                            break
                        }
                    }
                }
            },
    ) {
        if (!isConnected) {
            DisconnectedPrompt(
                hidState = hidState,
                lastHostAddress = settings.lastHostAddress,
                onRequestPermissions = onRequestPermissions,
                onEnableBluetooth = onEnableBluetooth,
                onOpenHosts = onOpenHosts,
                onConnectHost = onConnectHost,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            // Minimal subtle corner crosshairs on the trackpad canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val inset = 20.dp.toPx()
                val arm = 6.dp.toPx()
                val stroke = 1.dp.toPx()
                val markColor = if (isDragLocked) {
                    klob.dragLockRed.copy(alpha = 0.55f)
                } else {
                    klob.line
                }
                val corners = listOf(
                    Offset(inset, inset),
                    Offset(size.width - inset, inset),
                    Offset(inset, size.height - inset),
                    Offset(size.width - inset, size.height - inset),
                )
                for (c in corners) {
                    drawLine(markColor, Offset(c.x - arm, c.y), Offset(c.x + arm, c.y), stroke)
                    drawLine(markColor, Offset(c.x, c.y - arm), Offset(c.x, c.y + arm), stroke)
                }
            }

            if (isDragLocked || isHolding) {
                Text(
                    text = if (isDragLocked) "DRAG LOCK // DOUBLE-TAP TO RELEASE" else "HOLDING",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDragLocked) klob.dragLockRed else klob.accent,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun DisconnectedPrompt(
    hidState: HidUiState,
    lastHostAddress: String?,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenHosts: () -> Unit,
    onConnectHost: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val klob = LocalKlobColors.current
    val lastHost = remember(hidState.bondedHosts, lastHostAddress) {
        hidState.bondedHosts.firstOrNull { it.address == lastHostAddress }
    }

    Column(
        modifier = modifier
            .padding(24.dp)
            .border(1.dp, klob.line)
            .background(klob.surfaceElevated)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = when {
                !hidState.bluetoothSupported -> "Bluetooth Unavailable"
                !hidState.hasPermissions -> "Bluetooth Permission"
                !hidState.bluetoothEnabled -> "Bluetooth Off"
                hidState.connectionState == HidConnectionState.CONNECTING -> "Connecting"
                else -> "Not Connected"
            },
            style = MaterialTheme.typography.titleMedium,
            color = klob.text,
        )

        Text(
            text = when {
                !hidState.bluetoothSupported -> "This device does not support Bluetooth HID."
                !hidState.hasPermissions -> "Allow Bluetooth access to act as a wireless mouse."
                !hidState.bluetoothEnabled -> "Turn on Bluetooth to connect to a Windows PC."
                hidState.connectionState == HidConnectionState.CONNECTING ->
                    hidState.connectedHostName ?: "Waiting for host..."
                else -> "Connect to a paired Windows PC or make this phone discoverable."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = klob.muted,
        )

        if (hidState.statusNote != null) {
            Text(
                text = hidState.statusNote,
                style = MaterialTheme.typography.bodySmall,
                color = klob.warning,
            )
        }

        if (hidState.bluetoothSupported) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    !hidState.hasPermissions -> {
                        PrimaryActionButton("ALLOW BLUETOOTH", onClick = onRequestPermissions)
                    }

                    !hidState.bluetoothEnabled -> {
                        PrimaryActionButton("ENABLE BLUETOOTH", onClick = onEnableBluetooth)
                    }

                    lastHost != null && hidState.connectionState == HidConnectionState.DISCONNECTED -> {
                        PrimaryActionButton(
                            label = "RECONNECT",
                            onClick = { onConnectHost(lastHost.address) },
                        )
                        SecondaryActionButton("HOSTS", onClick = onOpenHosts)
                    }

                    else -> {
                        PrimaryActionButton("CONNECT PC", onClick = onOpenHosts)
                    }
                }
            }
        }
    }
}

@Composable
private fun EdgeScrollColumn(
    scrollSpeed: Float,
    naturalScroll: Boolean,
    enabled: Boolean,
    keyboardOpen: Boolean,
    onScrollWheel: (Int) -> Unit,
    onToggleKeyboard: () -> Unit,
) {
    val klob = LocalKlobColors.current
    Column(
        modifier = Modifier
            .width(70.dp)
            .fillMaxHeight(),
    ) {
        EdgeScrollBar(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            scrollSpeed = scrollSpeed,
            naturalScroll = naturalScroll,
            enabled = enabled,
            onScrollWheel = onScrollWheel,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(klob.line),
        )

        Box(
            modifier = Modifier
                .size(70.dp)
                .background(if (keyboardOpen) klob.accentSoft else klob.surfaceElevated)
                .clickable(enabled = enabled, onClick = onToggleKeyboard)
                .semantics {
                    contentDescription = if (keyboardOpen) "Hide keyboard" else "Show keyboard"
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (keyboardOpen) Icons.Outlined.KeyboardHide else Icons.Outlined.Keyboard,
                contentDescription = null,
                tint = when {
                    !enabled -> klob.muted.copy(alpha = 0.45f)
                    keyboardOpen -> klob.accent
                    else -> klob.text
                },
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun EdgeScrollBar(
    modifier: Modifier = Modifier,
    scrollSpeed: Float,
    naturalScroll: Boolean,
    enabled: Boolean,
    onScrollWheel: (Int) -> Unit,
) {
    val klob = LocalKlobColors.current
    val accumulator = remember { EdgeScrollAccumulator() }
    var isTouching by remember { mutableStateOf(false) }
    var visualOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .background(if (isTouching) klob.hover else klob.surfaceElevated)
            .semantics { contentDescription = "Scroll bar" }
            .pointerInput(enabled, scrollSpeed, naturalScroll) {
                if (!enabled) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = {
                        isTouching = true
                        accumulator.reset()
                    },
                    onDragEnd = {
                        isTouching = false
                        accumulator.reset()
                    },
                    onDragCancel = {
                        isTouching = false
                        accumulator.reset()
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        visualOffset = (visualOffset + dragAmount) % 24f
                        val ticks = accumulator.onVerticalDrag(dragAmount, scrollSpeed, naturalScroll)
                        if (ticks != 0) {
                            onScrollWheel(ticks)
                        }
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 18.dp.toPx()
            val tickWidth = 18.dp.toPx()
            val startX = (size.width - tickWidth) / 2f
            val stroke = 1.dp.toPx()
            val color = if (isTouching) klob.accent else klob.line

            var y = (visualOffset % spacing)
            if (y < 0f) y += spacing
            while (y < size.height) {
                drawLine(
                    color = color,
                    start = Offset(startX, y),
                    end = Offset(startX + tickWidth, y),
                    strokeWidth = stroke,
                )
                y += spacing
            }
        }
    }
}

@Composable
fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val klob = LocalKlobColors.current
    Box(
        modifier = modifier
            .height(44.dp)
            .background(klob.brandGreen)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = klob.onBrandGreen,
        )
    }
}

@Composable
fun SecondaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val klob = LocalKlobColors.current
    Box(
        modifier = modifier
            .height(44.dp)
            .border(1.dp, klob.line)
            .background(klob.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = klob.text,
        )
    }
}
