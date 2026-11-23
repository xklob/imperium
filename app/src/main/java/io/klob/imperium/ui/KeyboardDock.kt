package io.klob.imperium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.klob.imperium.hid.HidKeyMap
import io.klob.imperium.input.LiveImeDiffEngine
import io.klob.imperium.ui.theme.LocalKlobColors

@Composable
fun KeyboardDock(
    onSendText: (text: String, modifier: Int) -> Unit,
    onSendBackspaces: (count: Int, modifier: Int) -> Unit,
    onSendKeyTap: (usageId: Int, modifier: Int) -> Unit,
    onPasteClipboard: () -> Unit,
) {
    val klob = LocalKlobColors.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var armedModifiers by remember { mutableIntStateOf(0) }
    var showFnKeys by remember { mutableStateOf(false) }
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = LiveImeDiffEngine.SENTINEL,
                selection = TextRange(LiveImeDiffEngine.SENTINEL.length),
            )
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    fun consumeModifiers(): Int {
        val mods = armedModifiers
        armedModifiers = 0
        return mods
    }

    fun toggleModifier(flag: Int) {
        armedModifiers = if ((armedModifiers and flag) != 0) {
            armedModifiers and flag.inv()
        } else {
            armedModifiers or flag
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Hidden 1dp IME sink for live Gboard / swipe / voice typing
        BasicTextField(
            value = fieldValue,
            onValueChange = { incoming ->
                val diff = LiveImeDiffEngine.compute(fieldValue.text, incoming.text)
                fieldValue = TextFieldValue(
                    text = diff.nextBuffer,
                    selection = TextRange(diff.nextBuffer.length),
                )
                if (diff.backspaces > 0) {
                    val mods = if (diff.inserted.isEmpty()) consumeModifiers() else 0
                    onSendBackspaces(diff.backspaces, mods)
                }
                if (diff.inserted.isNotEmpty()) {
                    val mods = consumeModifiers()
                    onSendText(diff.inserted, mods)
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = true,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    val mods = consumeModifiers()
                    onSendKeyTap(HidKeyMap.KEY_ENTER, mods)
                },
            ),
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester),
        )

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(klob.line))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(klob.surfaceElevated)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StripKey(
                label = "CTRL",
                active = (armedModifiers and HidKeyMap.MOD_LEFT_CTRL) != 0,
                onClick = { toggleModifier(HidKeyMap.MOD_LEFT_CTRL) },
            )
            StripKey(
                label = "ALT",
                active = (armedModifiers and HidKeyMap.MOD_LEFT_ALT) != 0,
                onClick = { toggleModifier(HidKeyMap.MOD_LEFT_ALT) },
            )
            StripKey(
                label = "WIN",
                active = (armedModifiers and HidKeyMap.MOD_LEFT_GUI) != 0,
                onClick = { toggleModifier(HidKeyMap.MOD_LEFT_GUI) },
            )
            StripKey(
                label = "SHIFT",
                active = (armedModifiers and HidKeyMap.MOD_LEFT_SHIFT) != 0,
                onClick = { toggleModifier(HidKeyMap.MOD_LEFT_SHIFT) },
            )
            StripKey(
                label = "PASTE",
                active = false,
                onClick = onPasteClipboard,
            )
            StripKey(
                label = "FN",
                active = showFnKeys,
                onClick = { showFnKeys = !showFnKeys },
            )

            if (showFnKeys) {
                val fKeys = listOf(
                    "F1" to HidKeyMap.KEY_F1,
                    "F2" to HidKeyMap.KEY_F2,
                    "F3" to HidKeyMap.KEY_F3,
                    "F4" to HidKeyMap.KEY_F4,
                    "F5" to HidKeyMap.KEY_F5,
                    "F6" to HidKeyMap.KEY_F6,
                    "F7" to HidKeyMap.KEY_F7,
                    "F8" to HidKeyMap.KEY_F8,
                    "F9" to HidKeyMap.KEY_F9,
                    "F10" to HidKeyMap.KEY_F10,
                    "F11" to HidKeyMap.KEY_F11,
                    "F12" to HidKeyMap.KEY_F12,
                )
                for ((label, code) in fKeys) {
                    StripKey(
                        label = label,
                        active = false,
                        onClick = { onSendKeyTap(code, consumeModifiers()) },
                    )
                }
            } else {
                val navKeys = listOf(
                    "ESC" to HidKeyMap.KEY_ESC,
                    "TAB" to HidKeyMap.KEY_TAB,
                    "←" to HidKeyMap.KEY_LEFT,
                    "↓" to HidKeyMap.KEY_DOWN,
                    "↑" to HidKeyMap.KEY_UP,
                    "→" to HidKeyMap.KEY_RIGHT,
                    "DEL" to HidKeyMap.KEY_DELETE,
                    "HOME" to HidKeyMap.KEY_HOME,
                    "END" to HidKeyMap.KEY_END,
                    "PGUP" to HidKeyMap.KEY_PAGE_UP,
                    "PGDN" to HidKeyMap.KEY_PAGE_DOWN,
                )
                for ((label, code) in navKeys) {
                    StripKey(
                        label = label,
                        active = false,
                        onClick = { onSendKeyTap(code, consumeModifiers()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StripKey(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val klob = LocalKlobColors.current
    Box(
        modifier = Modifier
            .height(34.dp)
            .widthIn(min = 40.dp)
            .background(if (active) klob.accentSoft else klob.surface)
            .border(1.dp, if (active) klob.accentBorder else klob.line)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) klob.accent else klob.text,
        )
    }
}
