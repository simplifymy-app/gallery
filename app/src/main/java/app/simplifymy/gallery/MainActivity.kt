package app.simplifymy.gallery

import android.content.Context
import android.os.Bundle
import app.simplifymy.gallery.core.settings.storedLanguage
import app.simplifymy.gallery.core.settings.withLanguage
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import app.simplifymy.gallery.core.settings.AppIconManager
import app.simplifymy.gallery.feature.permission.MediaPermissionGate
import app.simplifymy.gallery.navigation.GalleryNavHost
import androidx.compose.ui.graphics.Color
import app.simplifymy.gallery.ui.theme.GalleryTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appIcons: AppIconManager

    /** Swapping the launcher alias finishes the task, so it waits until nothing is on screen. */
    override fun onStop() {
        super.onStop()
        appIcons.flush()
    }

    /** The locale has to be in place before any resource is read, so it goes on the base context. */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withLanguage(base.storedLanguage()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: ThemeViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            GalleryTheme(
                mode = settings.themeMode,
                accent = Color(settings.accentArgb),
                contrast = settings.contrast,
                corners = settings.corners,
                font = settings.font,
                textScale = settings.textScale,
            ) {
                MediaPermissionGate { GalleryNavHost() }
            }
        }
    }
}
