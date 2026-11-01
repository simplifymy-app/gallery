package app.simplifymy.gallery.core.settings

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import app.simplifymy.gallery.core.model.Grouping
import app.simplifymy.gallery.core.model.MediaFilter
import app.simplifymy.gallery.core.model.ScrollDirection
import app.simplifymy.gallery.core.model.SortKey
import app.simplifymy.gallery.core.model.Sorting
import androidx.compose.ui.graphics.toArgb
import app.simplifymy.gallery.ui.theme.Accent
import app.simplifymy.gallery.ui.theme.AppIcon
import app.simplifymy.gallery.ui.theme.Contrast
import app.simplifymy.gallery.ui.theme.CornerStyle
import app.simplifymy.gallery.ui.theme.DetailsPlacement
import app.simplifymy.gallery.ui.theme.ThumbnailSpacing
import app.simplifymy.gallery.ui.theme.GalleryFont
import app.simplifymy.gallery.ui.theme.TextScale
import app.simplifymy.gallery.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class GallerySettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val accentArgb: Int = Accent.Indigo.color.toArgb(),
    val contrast: Contrast = Contrast.Normal,
    val font: GalleryFont = GalleryFont.System,
    val textScale: TextScale = TextScale.Default,
    val corners: CornerStyle = CornerStyle.Default,
    val appIcon: AppIcon = AppIcon.Carbon,
    val folderColumns: Int = 3,
    val mediaColumns: Int = 3,
    val sorting: Sorting = Sorting(),
    val grouping: Grouping = Grouping.Day,
    val mediaFilter: MediaFilter = MediaFilter.All,
    val scrollDirection: ScrollDirection = ScrollDirection.Down,
    val overlayUi: Boolean = false,
    val cropThumbnails: Boolean = true,
    val showFolderNames: Boolean = true,
    val showFolderDetails: Boolean = false,
    val showRecent: Boolean = false,
    val detailsPlacement: DetailsPlacement = DetailsPlacement.Below,
    val thumbnailSpacing: ThumbnailSpacing = ThumbnailSpacing.Small,
    val showVideoDuration: Boolean = true,
    val autoplayNextVideo: Boolean = false,
    val backgroundPlayback: Boolean = false,
    val copyPathAction: Boolean = false,
    val language: Language = Language.System,
)

private const val BOOTSTRAP_PREFS = "bootstrap"
private const val BOOTSTRAP_LANGUAGE = "language"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

private object Keys {
    val themeMode = stringPreferencesKey("theme_mode")
    val accentArgb = intPreferencesKey("accent_argb")
    val contrast = stringPreferencesKey("contrast")
    val font = stringPreferencesKey("font")
    val textScale = stringPreferencesKey("text_scale")
    val corners = stringPreferencesKey("corners")
    val appIcon = stringPreferencesKey("app_icon")
    val folderColumns = intPreferencesKey("folder_columns")
    val mediaColumns = intPreferencesKey("media_columns")
    val sortKey = stringPreferencesKey("sort_key")
    val sortDesc = booleanPreferencesKey("sort_desc")
    val grouping = stringPreferencesKey("grouping")
    val mediaFilter = stringPreferencesKey("media_filter")
    val scrollDirection = stringPreferencesKey("scroll_direction")
    val overlayUi = booleanPreferencesKey("overlay_ui")
    val cropThumbnails = booleanPreferencesKey("crop_thumbnails")
    val showFolderNames = booleanPreferencesKey("show_folder_names")
    val showFolderDetails = booleanPreferencesKey("show_folder_details")
    val showRecent = booleanPreferencesKey("show_recent")
    val detailsPlacement = stringPreferencesKey("details_placement")
    val thumbnailSpacing = stringPreferencesKey("thumbnail_spacing")
    val showVideoDuration = booleanPreferencesKey("show_video_duration")
    val autoplayNextVideo = booleanPreferencesKey("autoplay_next_video")
    val backgroundPlayback = booleanPreferencesKey("background_playback")
    val copyPathAction = booleanPreferencesKey("copy_path_action")
    val language = stringPreferencesKey("language")
}

private inline fun <reified T : Enum<T>> Preferences.enum(key: Preferences.Key<String>, fallback: T): T =
    this[key]?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: fallback

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    val settings: Flow<GallerySettings> = context.dataStore.data.map { p ->
        val defaults = GallerySettings()
        GallerySettings(
            themeMode = p.enum(Keys.themeMode, defaults.themeMode),
            accentArgb = p[Keys.accentArgb] ?: defaults.accentArgb,
            contrast = p.enum(Keys.contrast, defaults.contrast),
            font = p.enum(Keys.font, defaults.font),
            textScale = p.enum(Keys.textScale, defaults.textScale),
            corners = p.enum(Keys.corners, defaults.corners),
            appIcon = p.enum(Keys.appIcon, defaults.appIcon),
            folderColumns = p[Keys.folderColumns] ?: defaults.folderColumns,
            mediaColumns = p[Keys.mediaColumns] ?: defaults.mediaColumns,
            sorting = Sorting(
                p.enum(Keys.sortKey, defaults.sorting.key),
                p[Keys.sortDesc] ?: defaults.sorting.descending,
            ),
            grouping = p.enum(Keys.grouping, defaults.grouping),
            mediaFilter = p.enum(Keys.mediaFilter, defaults.mediaFilter),
            scrollDirection = p.enum(Keys.scrollDirection, defaults.scrollDirection),
            overlayUi = p[Keys.overlayUi] ?: defaults.overlayUi,
            cropThumbnails = p[Keys.cropThumbnails] ?: defaults.cropThumbnails,
            showFolderNames = p[Keys.showFolderNames] ?: defaults.showFolderNames,
            showFolderDetails = p[Keys.showFolderDetails] ?: defaults.showFolderDetails,
            showRecent = p[Keys.showRecent] ?: defaults.showRecent,
            detailsPlacement = p.enum(Keys.detailsPlacement, defaults.detailsPlacement),
            thumbnailSpacing = p.enum(Keys.thumbnailSpacing, defaults.thumbnailSpacing),
            showVideoDuration = p[Keys.showVideoDuration] ?: defaults.showVideoDuration,
            autoplayNextVideo = p[Keys.autoplayNextVideo] ?: defaults.autoplayNextVideo,
            backgroundPlayback = p[Keys.backgroundPlayback] ?: defaults.backgroundPlayback,
            copyPathAction = p[Keys.copyPathAction] ?: defaults.copyPathAction,
            language = p.enum(Keys.language, defaults.language),
        )
    }

    suspend fun setThemeMode(value: ThemeMode) = put(Keys.themeMode, value.name)
    suspend fun setAccent(argb: Int) = put(Keys.accentArgb, argb)
    suspend fun setContrast(value: Contrast) = put(Keys.contrast, value.name)
    suspend fun setFont(value: GalleryFont) = put(Keys.font, value.name)
    suspend fun setTextScale(value: TextScale) = put(Keys.textScale, value.name)
    suspend fun setCorners(value: CornerStyle) = put(Keys.corners, value.name)
    suspend fun setAppIcon(value: AppIcon) = put(Keys.appIcon, value.name)
    suspend fun setFolderColumns(value: Int) = put(Keys.folderColumns, value)
    suspend fun setMediaColumns(value: Int) = put(Keys.mediaColumns, value)
    suspend fun setGrouping(value: Grouping) = put(Keys.grouping, value.name)
    suspend fun setCropThumbnails(value: Boolean) = put(Keys.cropThumbnails, value)
    suspend fun setMediaFilter(value: MediaFilter) = put(Keys.mediaFilter, value.name)
    suspend fun setScrollDirection(value: ScrollDirection) = put(Keys.scrollDirection, value.name)
    suspend fun setOverlayUi(value: Boolean) = put(Keys.overlayUi, value)
    suspend fun setShowFolderNames(value: Boolean) = put(Keys.showFolderNames, value)
    suspend fun setShowFolderDetails(value: Boolean) = put(Keys.showFolderDetails, value)
    suspend fun setShowRecent(value: Boolean) = put(Keys.showRecent, value)
    suspend fun setDetailsPlacement(value: DetailsPlacement) = put(Keys.detailsPlacement, value.name)
    suspend fun setThumbnailSpacing(value: ThumbnailSpacing) = put(Keys.thumbnailSpacing, value.name)
    suspend fun setShowVideoDuration(value: Boolean) = put(Keys.showVideoDuration, value)
    suspend fun setAutoplayNextVideo(value: Boolean) = put(Keys.autoplayNextVideo, value)
    suspend fun setBackgroundPlayback(value: Boolean) = put(Keys.backgroundPlayback, value)
    suspend fun setCopyPathAction(value: Boolean) = put(Keys.copyPathAction, value)
    suspend fun setLanguage(value: Language) {
        context.getSharedPreferences(BOOTSTRAP_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(BOOTSTRAP_LANGUAGE, value.name)
            .apply()
        put(Keys.language, value.name)
    }

    suspend fun setSorting(value: Sorting) {
        context.dataStore.edit {
            it[Keys.sortKey] = value.key.name
            it[Keys.sortDesc] = value.descending
        }
    }

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }
}

fun Sorting.cycled(): Sorting = copy(key = SortKey.entries[(key.ordinal + 1) % SortKey.entries.size])

fun Context.storedLanguage(): Language =
    getSharedPreferences(BOOTSTRAP_PREFS, Context.MODE_PRIVATE)
        .getString(BOOTSTRAP_LANGUAGE, null)
        ?.let { stored -> Language.entries.firstOrNull { it.name == stored } }
        ?: Language.System

fun Context.withLanguage(language: Language): Context {
    if (language == Language.System) return this
    val configuration = Configuration(resources.configuration).apply {
        setLocales(LocaleList(Locale.forLanguageTag(language.tag)))
    }
    return createConfigurationContext(configuration)
}
