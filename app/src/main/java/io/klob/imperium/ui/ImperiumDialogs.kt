package io.klob.imperium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.klob.imperium.hid.HidConnectionState
import io.klob.imperium.hid.HidUiState
import io.klob.imperium.settings.ImperiumSettings
import io.klob.imperium.settings.ScrollBarSide
import io.klob.imperium.settings.SettingsStore
import io.klob.imperium.settings.ThemeMode
import io.klob.imperium.ui.theme.LocalKlobColors
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HostsDialog(
    hidState: HidUiState,
    onDismiss: () -> Unit,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onRefresh: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
) {
    val klob = LocalKlobColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, klob.line)
                .background(klob.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Bluetooth Hosts",
                    style = MaterialTheme.typography.titleMedium,
                    color = klob.text,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = onRefresh)
                        .semantics { contentDescription = "Refresh hosts" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, tint = klob.text, modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = onDismiss)
                        .semantics { contentDescription = "Close hosts dialog" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, tint = klob.text, modifier = Modifier.size(20.dp))
                }
            }

            Text(
                text = "Pair in Windows Bluetooth settings, then tap Connect.",
                style = MaterialTheme.typography.bodySmall,
                color = klob.muted,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    !hidState.hasPermissions -> PrimaryActionButton("ALLOW BLUETOOTH", onClick = onRequestPermissions)
                    !hidState.bluetoothEnabled -> PrimaryActionButton("ENABLE BLUETOOTH", onClick = onEnableBluetooth)
                    else -> PrimaryActionButton("MAKE DISCOVERABLE", onClick = onMakeDiscoverable)
                }
                if (hidState.connectionState == HidConnectionState.CONNECTED) {
                    SecondaryActionButton("DISCONNECT", onClick = onDisconnect)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(klob.line))

            if (hidState.bondedHosts.isEmpty()) {
                Text(
                    text = "No paired Bluetooth devices found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = klob.muted,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    hidState.bondedHosts.forEachIndexed { index, host ->
                        val isThisConnected =
                            hidState.connectionState == HidConnectionState.CONNECTED &&
                                hidState.connectedHostAddress == host.address
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .background(if (isThisConnected) klob.accentSoft else Color.Transparent)
                                .clickable {
                                    if (isThisConnected) onDisconnect() else onConnect(host.address)
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (host.isComputer) Icons.Outlined.Computer else Icons.Outlined.Devices,
                                contentDescription = null,
                                tint = if (isThisConnected) klob.accent else klob.muted,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = host.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = klob.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = host.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = klob.muted,
                                )
                            }
                            Text(
                                text = if (isThisConnected) "CONNECTED" else "CONNECT",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isThisConnected) klob.accent else klob.text,
                            )
                        }
                        if (index < hidState.bondedHosts.lastIndex) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(klob.line))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    settings: ImperiumSettings,
    isIgnoringBatteryOptimizations: Boolean,
    onRequestUnrestrictedBattery: () -> Unit,
    onUpdateSettings: ((ImperiumSettings) -> ImperiumSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val klob = LocalKlobColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, klob.line)
                .background(klob.surface)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = klob.text,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = onDismiss)
                        .semantics { contentDescription = "Close settings" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, tint = klob.text, modifier = Modifier.size(20.dp))
                }
            }

            // Appearance (System / Light / Dark icon selector)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Appearance", style = MaterialTheme.typography.titleSmall, color = klob.text)
                Row(modifier = Modifier.border(1.dp, klob.line)) {
                    ThemeIconOption(
                        icon = Icons.Outlined.LightMode,
                        label = "Light theme",
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        onClick = { onUpdateSettings { it.copy(themeMode = ThemeMode.LIGHT) } },
                    )
                    ThemeIconOption(
                        icon = Icons.Outlined.DarkMode,
                        label = "Dark theme",
                        selected = settings.themeMode == ThemeMode.DARK,
                        onClick = { onUpdateSettings { it.copy(themeMode = ThemeMode.DARK) } },
                    )
                    ThemeIconOption(
                        icon = Icons.Outlined.SettingsBrightness,
                        label = "System theme",
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { onUpdateSettings { it.copy(themeMode = ThemeMode.SYSTEM) } },
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(klob.line))

            // Pointer Speed
            SliderSettingRow(
                label = "Pointer Speed",
                valueText = String.format(Locale.US, "%.2fx", settings.pointerSpeed),
                value = settings.pointerSpeed,
                range = SettingsStore.MIN_POINTER_SPEED..SettingsStore.MAX_POINTER_SPEED,
                onValueChange = { raw ->
                    val snapped = (raw * 20f).roundToInt() / 20f
                    onUpdateSettings { it.copy(pointerSpeed = snapped) }
                },
            )

            ToggleSettingRow(
                label = "Pointer Acceleration",
                checked = settings.pointerAcceleration,
                onCheckedChange = { enabled -> onUpdateSettings { it.copy(pointerAcceleration = enabled) } },
            )

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(klob.line))

            // Scroll Speed
            SliderSettingRow(
                label = "Scroll Speed",
                valueText = String.format(Locale.US, "%.2fx", settings.scrollSpeed),
                value = settings.scrollSpeed,
                range = SettingsStore.MIN_SCROLL_SPEED..SettingsStore.MAX_SCROLL_SPEED,
                onValueChange = { raw ->
                    val snapped = (raw * 20f).roundToInt() / 20f
                    onUpdateSettings { it.copy(scrollSpeed = snapped) }
                },
            )

            ToggleSettingRow(
                label = "Natural Scroll",
                checked = settings.naturalScroll,
                onCheckedChange = { enabled -> onUpdateSettings { it.copy(naturalScroll = enabled) } },
            )

            // Scroll Bar Side
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Scroll Bar Side", style = MaterialTheme.typography.titleSmall, color = klob.text)
                Row(modifier = Modifier.border(1.dp, klob.line)) {
                    SegmentOption(
                        label = "LEFT",
                        selected = settings.scrollBarSide == ScrollBarSide.LEFT,
                        onClick = { onUpdateSettings { it.copy(scrollBarSide = ScrollBarSide.LEFT) } },
                    )
                    SegmentOption(
                        label = "RIGHT",
                        selected = settings.scrollBarSide == ScrollBarSide.RIGHT,
                        onClick = { onUpdateSettings { it.copy(scrollBarSide = ScrollBarSide.RIGHT) } },
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(klob.line))

            ToggleSettingRow(
                label = "Haptic Feedback",
                subtitle = "Clicks, double-clicks, right-clicks, press-and-hold, drag lock, and scroll ticks.",
                checked = settings.hapticsEnabled,
                onCheckedChange = { enabled -> onUpdateSettings { it.copy(hapticsEnabled = enabled) } },
            )

            ToggleSettingRow(
                label = "Auto-Reconnect",
                subtitle = "Automatically reconnect to your last paired PC on open or after a drop.",
                checked = settings.autoReconnect,
                onCheckedChange = { enabled -> onUpdateSettings { it.copy(autoReconnect = enabled) } },
            )

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(klob.line))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Battery Optimization", style = MaterialTheme.typography.titleSmall, color = klob.text)
                    Text(
                        text = if (isIgnoringBatteryOptimizations) {
                            "Unrestricted // low-latency Bluetooth active."
                        } else {
                            "Disable battery throttling to reduce Bluetooth input lag."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isIgnoringBatteryOptimizations) klob.accent else klob.muted,
                    )
                }
                if (!isIgnoringBatteryOptimizations) {
                    SecondaryActionButton("DISABLE", onClick = onRequestUnrestrictedBattery)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(klob.line))

            // Concise gesture reference
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Gestures", style = MaterialTheme.typography.titleSmall, color = klob.text)
                Text(
                    text = "1F tap // left click\n1F press & hold // click and hold\n1F double-tap // drag lock on/off\n2F tap // right click\n2F double-tap // double click\nEdge bar // scroll wheel",
                    style = MaterialTheme.typography.bodySmall,
                    color = klob.muted,
                )
            }
        }
    }
}

@Composable
private fun ThemeIconOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val klob = LocalKlobColors.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(if (selected) klob.accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) klob.accent else klob.muted,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SegmentOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val klob = LocalKlobColors.current
    Box(
        modifier = Modifier
            .height(44.dp)
            .background(if (selected) klob.accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) klob.accent else klob.muted,
        )
    }
}

@Composable
private fun SliderSettingRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    val klob = LocalKlobColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = klob.text)
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = klob.accent)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = klob.accent,
                activeTrackColor = klob.accent,
                inactiveTrackColor = klob.line,
            ),
        )
    }
}

@Composable
private fun ToggleSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    val klob = LocalKlobColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = klob.text)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = klob.muted)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = klob.onBrandGreen,
                checkedTrackColor = klob.brandGreen,
                uncheckedThumbColor = klob.muted,
                uncheckedTrackColor = klob.surfaceElevated,
                uncheckedBorderColor = klob.line,
            ),
        )
    }
}
