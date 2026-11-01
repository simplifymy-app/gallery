package app.simplifymy.gallery.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.simplifymy.gallery.R
import app.simplifymy.gallery.ui.theme.ControlRadius
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing

private val SCRIM = Color.Black.copy(alpha = 0.64f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GallerySheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = GalleryTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        containerColor = colors.surfaceAlt,
        contentColor = colors.text,
        tonalElevation = 0.dp,
        scrimColor = SCRIM,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(colors.hairline, CircleShape),
            )
        },
    ) {
        Text(
            title.uppercase(),
            style = GalleryTheme.type.section,
            color = colors.textFaint,
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.base),
        )
        content()
    }
}

@Composable
fun DialogCard(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = GalleryTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surfaceAlt)
                .padding(20.dp),
        ) {
            Text(title, style = GalleryTheme.type.label.copy(fontWeight = FontWeight.SemiBold), color = colors.text)
            content()
        }
    }
}

@Composable
fun TextDialog(
    title: String,
    initial: String,
    isValid: (String) -> Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = GalleryTheme.colors
    val focus = remember { FocusRequester() }
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val valid = isValid(value.text)
    val submit = { if (valid) onConfirm(value.text) }

    LaunchedEffect(Unit) { focus.requestFocus() }

    DialogCard(title, onDismiss) {
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = GalleryTheme.type.body.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .focusRequester(focus)
                .clip(RoundedCornerShape(ControlRadius))
                .background(colors.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
        DialogButtons(enabled = valid, onDismiss = onDismiss, onConfirm = submit)
    }
}

@Composable
fun DialogButtons(
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = stringResource(R.string.action_done),
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.base, Alignment.End),
    ) {
        DialogButton(stringResource(R.string.action_cancel), filled = false, enabled = true, onClick = onDismiss)
        DialogButton(confirmLabel, filled = true, enabled = enabled, onClick = onConfirm)
    }
}

@Composable
private fun DialogButton(label: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = GalleryTheme.colors
    Box(
        modifier = Modifier
            .height(34.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(RoundedCornerShape(ControlRadius))
            .background(if (filled) colors.accent else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = GalleryTheme.type.label, color = if (filled) colors.onAccent else colors.textMuted)
    }
}
