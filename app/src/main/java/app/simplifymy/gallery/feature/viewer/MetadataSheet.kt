package app.simplifymy.gallery.feature.viewer

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.simplifymy.gallery.R
import app.simplifymy.gallery.core.media.MediaDetails
import app.simplifymy.gallery.core.media.MetadataEdit
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.ui.component.DialogButtons
import app.simplifymy.gallery.ui.component.DialogCard
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.GallerySheet
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.component.TextDialog
import app.simplifymy.gallery.ui.formatSize
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class Field { Date, Time, Description, Author }

@Composable
fun MetadataSheet(
    medium: Medium,
    details: MediaDetails,
    onSave: (MetadataEdit) -> Unit,
    onDismiss: () -> Unit,
) {
    var taken by remember(medium.id) { mutableLongStateOf(medium.dateTaken) }
    var description by remember(details) { mutableStateOf(details.description) }
    var author by remember(details) { mutableStateOf(details.author) }
    var removeLocation by remember(details) { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Field?>(null) }
    val edit = MetadataEdit(
        taken = taken.takeIf { it != medium.dateTaken },
        description = description.takeIf { it != details.description },
        author = author.takeIf { it != details.author },
        removeLocation = removeLocation,
    )
    val locale = LocalConfiguration.current.locales[0]
    val moment = taken.toZoned()
    val editable = details.editable
    fun editor(field: Field): (() -> Unit)? = if (editable) ({ editing = field }) else null

    GallerySheet(stringResource(R.string.action_metadata), onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = Spacing.base)) {
            MetadataRow(
                R.string.metadata_date,
                DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(moment),
                onEdit = editor(Field.Date),
            )
            MetadataRow(
                R.string.metadata_time,
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(moment),
                onEdit = editor(Field.Time),
            )
            MetadataRow(R.string.metadata_description, description.ifEmpty { "—" }, onEdit = editor(Field.Description))
            MetadataRow(R.string.metadata_author, author.ifEmpty { "—" }, onEdit = editor(Field.Author))
            details.location?.takeIf { !removeLocation }?.let { location ->
                MetadataLine(R.string.metadata_location, location) {
                    if (editable) {
                        IconAction(
                            GalleryIcons.Close,
                            onClick = { removeLocation = true },
                            label = stringResource(R.string.metadata_remove_location),
                            tint = GalleryTheme.colors.textFaint,
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.details_title),
                style = GalleryTheme.type.section,
                color = GalleryTheme.colors.textFaint,
                modifier = Modifier.padding(horizontal = Spacing.screen).padding(top = 18.dp, bottom = Spacing.base),
            )
            details.camera?.let { MetadataRow(R.string.details_camera, it) }
            details.lens?.let { MetadataRow(R.string.details_lens, it) }
            details.exposure?.let { MetadataRow(R.string.details_exposure, it) }
            details.focalLength?.let { MetadataRow(R.string.details_focal_length, it) }
            details.dimensions?.let { MetadataRow(R.string.details_dimensions, it) }
            MetadataRow(R.string.details_size, formatSize(medium.size))
            details.path?.let { MetadataRow(R.string.details_path, it) }
            if (editable) {
                DialogButtons(
                    enabled = edit != MetadataEdit(),
                    onDismiss = onDismiss,
                    onConfirm = { onSave(edit) },
                    confirmLabel = stringResource(R.string.action_save),
                    modifier = Modifier.padding(horizontal = Spacing.screen),
                )
            } else {
                Text(
                    stringResource(R.string.metadata_read_only),
                    style = GalleryTheme.type.meta,
                    color = GalleryTheme.colors.textFaint,
                    modifier = Modifier.padding(horizontal = Spacing.screen).padding(top = 18.dp),
                )
            }
        }
    }

    val close = { editing = null }
    when (editing) {
        Field.Date -> DateDialog(moment, onPick = { taken = it; close() }, onDismiss = close)
        Field.Time -> TimeDialog(moment, onPick = { taken = it; close() }, onDismiss = close)
        Field.Description -> TextDialog(
            title = stringResource(R.string.metadata_description),
            initial = description,
            isValid = { it.trim() != description },
            onConfirm = { description = it.trim(); close() },
            onDismiss = close,
        )
        Field.Author -> TextDialog(
            title = stringResource(R.string.metadata_author),
            initial = author,
            isValid = { it.trim() != author },
            onConfirm = { author = it.trim(); close() },
            onDismiss = close,
        )
        null -> Unit
    }
}

@Composable
private fun MetadataRow(@StringRes label: Int, value: String, onEdit: (() -> Unit)? = null) {
    MetadataLine(label, value, Modifier.clickable(enabled = onEdit != null) { onEdit?.invoke() }) {
        if (onEdit != null) TrailingIcon(GalleryIcons.Edit)
    }
}

@Composable
private fun MetadataLine(
    @StringRes label: Int,
    value: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    val colors = GalleryTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = Spacing.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(label), style = GalleryTheme.type.body, color = colors.textMuted)
        Text(
            value,
            style = GalleryTheme.type.body,
            color = colors.text,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun TrailingIcon(icon: ImageVector) {
    Box(Modifier.size(Spacing.hitTarget), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = GalleryTheme.colors.textFaint, modifier = Modifier.size(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(moment: ZonedDateTime, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val colors = GalleryTheme.colors
    val state = rememberDatePickerState(
        initialSelectedDateMillis = moment.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    val pickerColors = DatePickerDefaults.colors(containerColor = colors.surfaceAlt)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(6.dp),
        colors = pickerColors,
        confirmButton = {
            DialogButtons(
                enabled = state.selectedDateMillis != null,
                onDismiss = onDismiss,
                onConfirm = {
                    state.selectedDateMillis?.let { picked ->
                        val day = Instant.ofEpochMilli(picked).atZone(ZoneOffset.UTC).toLocalDate()
                        onPick(moment.with(day).toInstant().toEpochMilli())
                    }
                },
            )
        },
    ) {
        DatePicker(state, colors = pickerColors, showModeToggle = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(moment: ZonedDateTime, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val colors = GalleryTheme.colors
    val state = rememberTimePickerState(
        initialHour = moment.hour,
        initialMinute = moment.minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
    )
    DialogCard(stringResource(R.string.metadata_time), onDismiss) {
        TimePicker(
            state,
            colors = TimePickerDefaults.colors(
                clockDialColor = colors.surface,
                selectorColor = colors.accent,
                periodSelectorBorderColor = colors.hairline,
                periodSelectorSelectedContainerColor = colors.accent,
                periodSelectorSelectedContentColor = colors.onAccent,
                periodSelectorUnselectedContentColor = colors.textMuted,
                timeSelectorSelectedContainerColor = colors.accent,
                timeSelectorSelectedContentColor = colors.onAccent,
                timeSelectorUnselectedContainerColor = colors.surface,
                timeSelectorUnselectedContentColor = colors.text,
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp),
        )
        DialogButtons(
            enabled = true,
            onDismiss = onDismiss,
            onConfirm = { onPick(moment.withHour(state.hour).withMinute(state.minute).toInstant().toEpochMilli()) },
        )
    }
}

private fun Long.toZoned(): ZonedDateTime = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
