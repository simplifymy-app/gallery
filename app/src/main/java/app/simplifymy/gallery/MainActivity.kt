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
import app.simplifymy.gallery.feature.viewer.OpenedMediaScreen
import app.simplifymy.gallery.feature.viewer.openedMedium
import app.simplifymy.gallery.navigation.GalleryNavHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.isDark
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appIcons: AppIconManager

    override fun onStop() {
        super.onStop()
        appIcons.flush()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withLanguage(base.storedLanguage()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val opened = intent.openedMedium(intent.resolveType(this))
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
                SystemBars(light = !settings.themeMode.isDark())
                if (opened != null) OpenedMediaScreen(opened, onClose = ::finish)
                else MediaPermissionGate { GalleryNavHost() }
            }
        }
    }
}

@Composable
private fun SystemBars(light: Boolean) {
    val view = LocalView.current
    val window = (view.context as ComponentActivity).window
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }
}
