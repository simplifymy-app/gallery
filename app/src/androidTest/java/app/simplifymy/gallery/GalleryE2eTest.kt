package app.simplifymy.gallery

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import app.simplifymy.gallery.R
import app.simplifymy.gallery.core.settings.Language
import app.simplifymy.gallery.core.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

private const val TIMEOUT = 20_000L
private val SEEDED = listOf("e2e_one.png", "e2e_two.png", "e2e_three.png")

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GalleryE2eTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val hilt = HiltAndroidRule(this)
    private val compose = createAndroidComposeRule<MainActivity>()

    private val media = object : ExternalResource() {
        override fun before() {
            runBlocking { SettingsRepository(context).setLanguage(Language.System) }
            MediaSeed.clear(context)
            MediaSeed.insert(context, SEEDED)
        }

        override fun after() = MediaSeed.clear(context)
    }

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(
            GrantPermissionRule.grant(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        )
        .around(media)
        .around(hilt)
        .around(compose)

    @After
    fun leaveSettingsAtDefaults() = MediaSeed.clear(context)

    @Test
    fun seededFolderAppearsOnTheFoldersScreen() {
        awaitText(MediaSeed.FOLDER)
        compose.onNodeWithText(str(R.string.app_name)).assertIsDisplayed()
        compose.onNodeWithText(MediaSeed.FOLDER).assertIsDisplayed()
    }

    @Test
    fun openingAFolderShowsEveryItemInIt() {
        awaitText(MediaSeed.FOLDER)
        compose.onNodeWithText(MediaSeed.FOLDER).performClick()
        awaitText("${SEEDED.size} items", substring = true)
        compose.onAllNodesWithText("${SEEDED.size} items", substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun longPressOnAFolderStartsSelection() {
        awaitText(MediaSeed.FOLDER)
        compose.onNodeWithText(MediaSeed.FOLDER).performTouchInput { longClick() }
        awaitText(str(R.string.selection_count, 1))
        compose.onNodeWithText(str(R.string.selection_hint_long_press)).assertIsDisplayed()
    }

    @Test
    fun aSelectedFolderCanBeIgnoredThenRestored() {
        awaitText(MediaSeed.FOLDER)
        compose.onNodeWithText(MediaSeed.FOLDER).performTouchInput { longClick() }
        awaitText(str(R.string.selection_count, 1))
        compose.onNodeWithContentDescription(str(R.string.action_ignore)).performClick()
        awaitGone(MediaSeed.FOLDER)

        openSettings()
        tapInSettings(str(R.string.setting_ignored))
        awaitText(str(R.string.ignored_hint, 1))
        compose.onNodeWithText(MediaSeed.FOLDER).performClick()
        compose.onNodeWithText(str(R.string.action_save)).performClick()
        awaitText(str(R.string.ignored_hint, 0))
    }

    @Test
    fun aSelectedFolderCanBePinned() {
        awaitText(MediaSeed.FOLDER)
        compose.onNodeWithText(MediaSeed.FOLDER).performTouchInput { longClick() }
        awaitText(str(R.string.selection_count, 1))
        compose.onNodeWithContentDescription(str(R.string.action_pin)).performClick()
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodesWithContentDescription(str(R.string.pinned)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun overflowMenuOffersSortingColumnsAndSettings() {
        compose.onNodeWithContentDescription(str(R.string.action_more)).performClick()
        awaitText(str(R.string.menu_sort_by))
        compose.onNodeWithText(str(R.string.menu_columns)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.menu_settings)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.setting_ignored)).assertIsDisplayed()
    }

    @Test
    fun settingsOfferGlobalSortingIncludingContentType() {
        openSettings()
        tapInSettings(str(R.string.sort_date_taken))
        awaitText(str(R.string.sort_type))
        compose.onNodeWithText(str(R.string.sort_type)).assertIsDisplayed()
    }

    @Test
    fun settingsShowALiveFolderPreview() {
        openSettings()
        awaitText(str(R.string.preview_label))
        compose.onNodeWithText(str(R.string.preview_label)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun searchScreenOpensFromTheToolbar() {
        compose.onNodeWithContentDescription(str(R.string.action_search)).performClick()
        awaitText(str(R.string.search_placeholder))
        compose.onNodeWithText(str(R.string.search_placeholder)).assertIsDisplayed()
    }

    private fun openSettings() {
        compose.onNodeWithContentDescription(str(R.string.action_more)).performClick()
        awaitText(str(R.string.menu_settings))
        compose.onNodeWithText(str(R.string.menu_settings)).performClick()
        awaitText(str(R.string.section_theme).uppercase())
    }

    private fun tapInSettings(text: String) {
        awaitText(text)
        compose.onNodeWithText(text).performScrollTo().performClick()
    }

    private fun str(id: Int, vararg args: Any) = context.getString(id, *args)

    private fun ukrainian(id: Int): String {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag("uk")))
        }
        return context.createConfigurationContext(configuration).getString(id)
    }

    private fun awaitText(text: String, substring: Boolean = false) =
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }

    private fun awaitGone(text: String) =
        compose.waitUntil(TIMEOUT) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
}
