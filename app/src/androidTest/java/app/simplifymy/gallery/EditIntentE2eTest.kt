package app.simplifymy.gallery

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.simplifymy.gallery.feature.editor.EDITOR_PREVIEW
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

private const val SCREENSHOT = "e2e_screenshot.png"
private const val DOWNLOAD = "e2e_download.png"

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EditIntentE2eTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO))
        .around(HiltAndroidRule(this))
        .around(compose)

    @Before
    fun seed() {
        MediaSeed.clear(context)
        MediaSeed.insert(context, listOf(SCREENSHOT))
    }

    @After
    fun clear() = MediaSeed.clear(context)

    @Test
    fun anEditRequestFromAnotherAppOpensTheEditor() {
        ActivityScenario.launch<MainActivity>(editIntent(SCREENSHOT)).use { awaitEditor() }
    }

    @Test
    fun anEditedDownloadIsSavedToPictures() {
        MediaSeed.insert(
            context,
            listOf(DOWNLOAD),
            folder = "Download/${MediaSeed.FOLDER}",
            collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        )
        ActivityScenario.launch<MainActivity>(editIntent(DOWNLOAD)).use {
            awaitEditor()
            compose.onNodeWithText(context.getString(R.string.action_save)).performClick()
            compose.waitUntil(20_000) { MediaSeed.count(context, "e2e_download_edited_%", folderLike = "Pictures/") == 1 }
        }
    }

    private fun editIntent(name: String) = Intent(Intent.ACTION_EDIT)
        .setClass(context, MainActivity::class.java)
        .setDataAndType(MediaSeed.uriOf(context, name), "image/png")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    private fun awaitEditor() = compose.waitUntil(20_000) {
        compose.onAllNodesWithTag(EDITOR_PREVIEW).fetchSemanticsNodes().isNotEmpty()
    }
}
