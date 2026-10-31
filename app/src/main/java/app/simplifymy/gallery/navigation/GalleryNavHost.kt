package app.simplifymy.gallery.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.simplifymy.gallery.feature.editor.EditorScreen
import app.simplifymy.gallery.feature.folders.FoldersScreen
import app.simplifymy.gallery.feature.media.MediaScreen
import app.simplifymy.gallery.feature.search.SearchScreen
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
data class Media(val bucketId: Long, val title: String)

@Serializable
data class Viewer(val bucketId: Long, val mediumId: Long)

@Serializable
data class Editor(val mediumId: Long)

@Composable
fun GalleryNavHost() {
    val navController = rememberNavController()
    val openSettings = { navController.navigate(Settings) }

    NavHost(
        navController = navController,
        startDestination = Folders,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        composable<Folders> {
            FoldersScreen(
                onOpenFolder = { navController.navigate(Media(it.bucketId, it.name)) },
                onOpenSearch = { navController.navigate(Search) },
                onOpenSettings = { openSettings() },
                onOpenIgnored = { navController.navigate(IgnoredFolders) },
            )
        }
        composable<Media> { entry ->
            MediaScreen(
                onBack = navController::popBackStack,
                onOpenMedium = { navController.navigate(Viewer(entry.toRoute<Media>().bucketId, it.id)) },
                onOpenSettings = { openSettings() },
            )
        }
        composable<Viewer> {
            ViewerScreen(
                onBack = navController::popBackStack,
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
            )
        }
        composable<IgnoredFolders> {
            IgnoredFoldersScreen(onBack = navController::popBackStack)
        }
    }
}
