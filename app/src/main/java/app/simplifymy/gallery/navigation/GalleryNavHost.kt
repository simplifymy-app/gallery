package app.simplifymy.gallery.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.simplifymy.gallery.feature.about.AboutScreen
import app.simplifymy.gallery.feature.editor.EditorScreen
import app.simplifymy.gallery.feature.folders.FoldersScreen
import app.simplifymy.gallery.feature.media.MediaScreen
import app.simplifymy.gallery.feature.search.SearchScreen
import app.simplifymy.gallery.feature.settings.ActionBarsScreen
import app.simplifymy.gallery.feature.settings.IgnoredFoldersScreen
import app.simplifymy.gallery.feature.settings.SettingsScreen
import app.simplifymy.gallery.feature.viewer.ViewerScreen
import kotlinx.serialization.Serializable

@Serializable
object Folders

@Serializable
object Settings

@Serializable
object Search

@Serializable
object IgnoredFolders

@Serializable
object ActionBars

@Serializable
object About

@Serializable
data class Media(val bucketId: Long, val title: String)

@Serializable
data class Viewer(val bucketId: Long, val mediumId: Long)

@Serializable
data class Editor(val mediumId: Long)

@Composable
fun GalleryNavHost(start: Any = Folders, onExit: () -> Unit = {}) {
    val navController = rememberNavController()
    val openSettings = { navController.navigate(Settings) }
    val openSearch = { navController.navigate(Search) }
    val back: () -> Unit = { if (!navController.popBackStack()) onExit() }

    NavHost(
        navController = navController,
        startDestination = start,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        composable<Folders> {
            FoldersScreen(
                onOpenFolder = { navController.navigate(Media(it.bucketId, it.name)) },
                onOpenSearch = { openSearch() },
                onOpenSettings = { openSettings() },
                onOpenIgnored = { navController.navigate(IgnoredFolders) },
            )
        }
        composable<Media> { entry ->
            MediaScreen(
                onBack = navController::popBackStack,
                onOpenMedium = { navController.navigate(Viewer(entry.toRoute<Media>().bucketId, it.id)) },
                onOpenSearch = { openSearch() },
                onOpenSettings = { openSettings() },
            )
        }
        composable<Viewer> {
            ViewerScreen(
                onBack = back,
                onEdit = { navController.navigate(Editor(it.id)) },
            )
        }
        composable<Editor> {
            EditorScreen(onBack = navController::popBackStack)
        }
        composable<Search> {
            SearchScreen(
                onBack = navController::popBackStack,
                onOpenMedium = { navController.navigate(Viewer(it.bucketId, it.id)) },
            )
        }
        composable<Settings> {
            SettingsScreen(
                onBack = navController::popBackStack,
                onOpenIgnored = { navController.navigate(IgnoredFolders) },
                onOpenActionBars = { navController.navigate(ActionBars) },
                onOpenAbout = { navController.navigate(About) },
            )
        }
        composable<About> {
            AboutScreen(onBack = navController::popBackStack)
        }
        composable<ActionBars> {
            ActionBarsScreen(onBack = navController::popBackStack)
        }
        composable<IgnoredFolders> {
            IgnoredFoldersScreen(onBack = navController::popBackStack)
        }
    }
}
