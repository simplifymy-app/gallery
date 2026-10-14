package app.simplifymy.gallery.feature.settings

import androidx.activity.compose.LocalActivity
import app.simplifymy.gallery.core.settings.Language
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simplifymy.gallery.R
import app.simplifymy.gallery.core.model.Folder
import app.simplifymy.gallery.core.model.Grouping
import app.simplifymy.gallery.core.model.ScrollDirection
import app.simplifymy.gallery.core.model.SortKey
import app.simplifymy.gallery.core.settings.GallerySettings
import app.simplifymy.gallery.ui.component.AccentPicker
import app.simplifymy.gallery.ui.component.DropdownSetting
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.component.MediaThumbnail
import app.simplifymy.gallery.ui.component.MenuStepper
import app.simplifymy.gallery.ui.component.SegmentedPicker
import app.simplifymy.gallery.ui.formatCount
import app.simplifymy.gallery.ui.formatSize
import app.simplifymy.gallery.ui.theme.Accent
import app.simplifymy.gallery.ui.theme.AppIcon
import app.simplifymy.gallery.ui.theme.Contrast
import app.simplifymy.gallery.ui.theme.CornerStyle
import app.simplifymy.gallery.ui.theme.DetailsPlacement
import app.simplifymy.gallery.ui.theme.GalleryFont
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing
import app.simplifymy.gallery.ui.theme.TextScale
import app.simplifymy.gallery.ui.theme.ThemeMode
import app.simplifymy.gallery.ui.theme.ThumbnailSpacing

private val COLUMN_RANGE = 2..6
private const val CONTACT_URL = "https://www.simplifymy.app/contact/"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenIgnored: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val previewFolders by viewModel.previewFolders.collectAsStateWithLifecycle()
    val colors = GalleryTheme.colors
    var customAccent by remember { mutableStateOf(false) }
    val activity = LocalActivity.current
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(GalleryIcons.Back, onClick = onBack)
            Text(stringResource(R.string.settings_title), style = GalleryTheme.type.title, color = colors.text)
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Section(stringResource(R.string.section_theme)) {
                DropdownSetting(
                    label = stringResource(R.string.setting_mode),
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    onSelect = viewModel::setThemeMode,
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 6.dp),
                ) { stringResource(it.labelRes) }
                AccentRow(
                    argb = settings.accentArgb,
                    custom = customAccent,
                    onSelect = { viewModel.setAccent(it) },
                    onToggleCustom = { customAccent = !customAccent },
                )
                if (customAccent) {
                    Box(Modifier.padding(horizontal = Spacing.screen)) {
                        AccentPicker(settings.accentArgb, viewModel::setAccent)
                    }
                }
                PickerRow(stringResource(R.string.setting_contrast)) {
                    SegmentedPicker(Contrast.entries, settings.contrast, viewModel::setContrast) { stringResource(it.labelRes) }
                }
                PickerRow(stringResource(R.string.setting_corner_radius)) {
                    SegmentedPicker(CornerStyle.entries, settings.corners, viewModel::setCorners) { it.label }
                }
            }

            Section(stringResource(R.string.setting_app_icon)) {
                AppIconRow(settings.appIcon, viewModel::setAppIcon)
            }

            Section(stringResource(R.string.section_typography)) {
                DropdownSetting(
                    label = stringResource(R.string.setting_font),
                    options = GalleryFont.entries,
                    selected = settings.font,
                    onSelect = viewModel::setFont,
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 6.dp),
                ) { font -> font.labelRes?.let { stringResource(it) } ?: font.label }
                DropdownSetting(
                    label = stringResource(R.string.setting_language),
                    options = Language.entries,
                    selected = settings.language,
                    onSelect = { language ->
                        viewModel.setLanguage(language) { activity?.recreate() }
                    },
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 6.dp),
                ) { if (it == Language.System) stringResource(R.string.follow_system) else it.label }
                PickerRow(stringResource(R.string.setting_text_size)) {
                    SegmentedPicker(TextScale.entries, settings.textScale, viewModel::setTextScale) { it.label }
                }
            }

            Section(stringResource(R.string.section_sorting)) {
                DropdownSetting(
                    label = stringResource(R.string.setting_sort_all),
                    options = SortKey.entries,
                    selected = settings.sorting.key,
                    onSelect = { viewModel.setSorting(settings.sorting.copy(key = it)) },
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 6.dp),
                ) { stringResource(it.labelRes) }
                ToggleRow(
                    stringResource(R.string.setting_reverse),
                    stringResource(R.string.setting_reverse_hint),
                    settings.sorting.descending,
                    { viewModel.setSorting(settings.sorting.copy(descending = it)) },
                )
            }

            Section(stringResource(R.string.section_grid)) {
                MenuStepper(
                    label = stringResource(R.string.setting_folder_columns),
                    value = settings.folderColumns,
                    range = COLUMN_RANGE,
                    modifier = Modifier.padding(horizontal = Spacing.screen),
                    onChange = viewModel::setFolderColumns,
                )
                MenuStepper(
                    label = stringResource(R.string.setting_media_columns),
                    value = settings.mediaColumns,
                    range = COLUMN_RANGE,
                    modifier = Modifier.padding(horizontal = Spacing.screen),
                    onChange = viewModel::setMediaColumns,
                )
                PickerRow(stringResource(R.string.setting_group_media)) {
                    SegmentedPicker(Grouping.entries, settings.grouping, viewModel::setGrouping) { it.name }
                }
                PickerRow(stringResource(R.string.setting_scroll_direction)) {
                    SegmentedPicker(
                        ScrollDirection.entries,
                        settings.scrollDirection,
                        viewModel::setScrollDirection,
                    ) { stringResource(it.labelRes) }
                }
                ToggleRow(
                    stringResource(R.string.setting_overlay_ui),
                    stringResource(R.string.setting_overlay_ui_hint),
                    settings.overlayUi,
                    viewModel::setOverlayUi,
                )
            }

            Section(stringResource(R.string.section_folders)) {
                FolderPreview(settings, previewFolders)
                ToggleRow(
                    stringResource(R.string.setting_show_details),
                    stringResource(R.string.setting_show_details_hint),
                    settings.showFolderNames,
                    viewModel::setShowFolderNames,
                )
                ToggleRow(
                    stringResource(R.string.setting_folder_size),
                    stringResource(R.string.setting_folder_size_hint),
                    settings.showFolderDetails,
                    viewModel::setShowFolderDetails,
                )
                DropdownSetting(
                    label = stringResource(R.string.setting_details_position),
                    options = DetailsPlacement.entries,
                    selected = settings.detailsPlacement,
                    onSelect = viewModel::setDetailsPlacement,
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 6.dp),
                ) { stringResource(it.labelRes) }
                LinkRow(
                    stringResource(R.string.setting_ignored),
                    stringResource(R.string.setting_ignored_hint),
                    onOpenIgnored,
                )
            }

            Section(stringResource(R.string.section_thumbnails)) {
                PickerRow(stringResource(R.string.setting_preview_spacing)) {
                    SegmentedPicker(
                        ThumbnailSpacing.entries,
                        settings.thumbnailSpacing,
                        viewModel::setThumbnailSpacing,
                    ) { stringResource(it.labelRes) }
                }
                ToggleRow(
                    stringResource(R.string.setting_crop),
                    stringResource(R.string.setting_crop_hint),
                    settings.cropThumbnails,
                    viewModel::setCropThumbnails,
                )
                ToggleRow(
                    stringResource(R.string.setting_video_duration),
                    stringResource(R.string.setting_video_duration_hint),
                    settings.showVideoDuration,
                    viewModel::setShowVideoDuration,
                )
            }

            Section(stringResource(R.string.section_video)) {
                ToggleRow(
                    stringResource(R.string.setting_autoplay),
                    stringResource(R.string.setting_autoplay_hint),
                    settings.autoplayNextVideo,
                    viewModel::setAutoplayNextVideo,
                )
            }

            Section(stringResource(R.string.section_support)) {
                LinkRow(
                    stringResource(R.string.setting_contact),
                    stringResource(R.string.setting_contact_hint),
                    { uriHandler.openUri(CONTACT_URL) },
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = GalleryTheme.type.section,
            color = GalleryTheme.colors.textFaint,
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
private fun LinkRow(label: String, hint: String, onClick: () -> Unit) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.screen, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = GalleryTheme.type.body, color = colors.text)
            Text(hint, style = GalleryTheme.type.meta, color = colors.textFaint)
        }
        Icon(
            GalleryIcons.Chevron,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(16.dp).rotate(-90f),
        )
    }
}

@Composable
private fun PickerRow(label: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = GalleryTheme.type.body, color = GalleryTheme.colors.text, modifier = Modifier.weight(1f))
        control()
    }
}

@Composable
private fun AccentRow(argb: Int, custom: Boolean, onSelect: (Int) -> Unit, onToggleCustom: () -> Unit) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.setting_accent), style = GalleryTheme.type.body, color = colors.text, modifier = Modifier.weight(1f))
        Accent.entries.forEach { preset ->
            val selected = preset.color.toArgb() == argb
            Box(
                modifier = Modifier
                    .size(if (selected) 26.dp else 22.dp)
                    .clip(CircleShape)
                    .background(preset.color)
                    .clickable { onSelect(preset.color.toArgb()) }
            )
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (custom) Color(argb) else colors.surfaceAlt)
                .clickable(onClick = onToggleCustom),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", style = GalleryTheme.type.label, color = if (custom) colors.onAccent else colors.textMuted)
        }
    }
}

@Composable
private fun AppIconRow(selected: AppIcon, onSelect: (AppIcon) -> Unit) {
    val colors = GalleryTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppIconPreview(selected)
            Column(Modifier.weight(1f)) {
                Text(stringResource(selected.labelRes), style = GalleryTheme.type.body, color = colors.text)
                Text(
                    stringResource(R.string.setting_app_icon_hint),
                    style = GalleryTheme.type.meta,
                    color = colors.textFaint,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon.entries.forEach { icon ->
                val chosen = icon == selected
                Box(
                    modifier = Modifier
                        .size(if (chosen) 26.dp else 22.dp)
                        .clip(CircleShape)
                        .background(icon.background)
                        .border(1.dp, colors.hairline, CircleShape)
                        .clickable { onSelect(icon) }
                )
            }
        }
    }
}

@Composable
private fun AppIconPreview(icon: AppIcon) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(icon.background)
            .border(1.dp, GalleryTheme.colors.hairline, RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(78.dp).aspectRatio(1f),
        )
    }
}

@Composable
private fun FolderPreview(settings: GallerySettings, folders: List<Folder>) {
    val colors = GalleryTheme.colors
    val below = settings.detailsPlacement == DetailsPlacement.Below
    val captioned = settings.showFolderNames || settings.showFolderDetails

    Column(
        modifier = Modifier
            .padding(horizontal = Spacing.screen, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface)
            .border(1.dp, colors.hairline, RoundedCornerShape(6.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.preview_label),
                style = GalleryTheme.type.section,
                color = colors.textFaint,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(
                    R.string.preview_summary,
                    settings.folderColumns,
                    stringResource(settings.thumbnailSpacing.labelRes).lowercase(),
                ),
                style = GalleryTheme.type.meta,
                color = colors.textFaint,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(settings.thumbnailSpacing.gap)) {
            repeat(settings.folderColumns) { index ->
                PreviewTile(
                    folder = folders.getOrNull(index % folders.size.coerceAtLeast(1)),
                    settings = settings,
                    below = below,
                    captioned = captioned,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PreviewTile(
    folder: Folder?,
    settings: GallerySettings,
    below: Boolean,
    captioned: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GalleryTheme.colors
    val shape = RoundedCornerShape(settings.corners.radius)
    val name = folder?.name ?: stringResource(R.string.preview_folder)
    val detail = folder?.let { "${formatCount(it.itemCount)} · ${formatSize(it.size)}" } ?: "0 · 0 B"

    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box {
            if (folder != null) {
                MediaThumbnail(
                    model = folder.coverUri,
                    crop = settings.cropThumbnails,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(colors.surfaceAlt))
            }
            if (captioned && !below) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .clip(shape)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xC7000000))))
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                ) {
                    PreviewCaption(name, detail, settings, Color(0xFFEDEDEF), Color(0xB3EDEDEF))
                }
            }
        }
        if (captioned && below) {
            PreviewCaption(name, detail, settings, colors.text, colors.textMuted)
        }
    }
}

@Composable
private fun PreviewCaption(
    name: String,
    detail: String,
    settings: GallerySettings,
    primary: Color,
    secondary: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        if (settings.showFolderNames) {
            Text(
                name,
                style = GalleryTheme.type.meta,
                color = primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (settings.showFolderDetails) {
            Text(
                detail,
                style = GalleryTheme.type.meta,
                color = secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = Spacing.screen, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = GalleryTheme.type.body, color = colors.text)
            Text(hint, style = GalleryTheme.type.meta, color = colors.textFaint)
        }
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 23.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) colors.accent else colors.surfaceAlt)
                .padding(2.5.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(if (checked) colors.onAccent else colors.textFaint))
        }
    }
}
