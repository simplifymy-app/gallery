package app.simplifymy.gallery.feature.viewer

import app.simplifymy.gallery.R
import androidx.compose.ui.res.stringResource
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import app.simplifymy.gallery.core.media.MediaDetails
import app.simplifymy.gallery.core.model.MediaType
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.formatSize
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CHROME_TEXT = Color(0xFFEDEDEF)

@Composable
fun ViewerScreen(onBack: () -> Unit, viewModel: ViewerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var chrome by remember { mutableStateOf(true) }
    var info by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { if (it.resultCode == Activity.RESULT_OK) onBack() }

    if (state.media.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val pagerState = rememberPagerState(initialPage = state.startIndex) { state.media.size }
    val current = state.media.getOrNull(pagerState.currentPage) ?: return

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            state.media.getOrNull(page)?.let { viewModel.loadDetails(it) }
        }
    }

    SystemBars(visible = chrome)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val medium = state.media[page]
            if (medium.type == MediaType.Video) {
                if (page == pagerState.currentPage) {
                    VideoPlayer(
                        uri = medium.uri,
                        controlsVisible = chrome,
                        autoplayNext = state.autoplayNextVideo,
                        onToggleChrome = { chrome = !chrome },
                        onEnded = {
                            val ahead = state.media.drop(page + 1).indexOfFirst { it.type == MediaType.Video }
                            if (ahead >= 0) scope.launch { pagerState.animateScrollToPage(page + 1 + ahead) }
                        },
                    )
                } else {
                    VideoPoster(medium) { chrome = !chrome }
                }
            } else {
                ZoomableAsyncImage(
                    model = medium.uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    onClick = { chrome = !chrome },
                )
            }
        }

        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn() + slideInVertically { -it / 3 },
            exit = fadeOut() + slideOutVertically { -it / 3 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopChrome(
                medium = current,
                infoOpen = info,
                onBack = onBack,
                onToggleFavorite = { viewModel.toggleFavorite(current) },
                onToggleInfo = { info = !info },
            )
        }

        AnimatedVisibility(
            visible = chrome && info,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 108.dp),
        ) {
            DetailsPanel(current, state.details, onClose = { info = false })
        }

        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut() + slideOutVertically { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomChrome(
                onShare = { context.startActivity(Intent.createChooser(shareIntent(current), null)) },
                onEdit = { runCatching { context.startActivity(editIntent(current)) } },
                onSetAs = { runCatching { context.startActivity(setAsIntent(current)) } },
                onDelete = {
                    trashLauncher.launch(IntentSenderRequest.Builder(viewModel.trash(current)).build())
                },
            )
        }
    }
}

@Composable
private fun SystemBars(visible: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(visible) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (visible) controller.show(WindowInsetsCompat.Type.systemBars())
        else controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
private fun VideoPoster(medium: Medium, onToggleChrome: () -> Unit) {
    Box(Modifier.fillMaxSize().clickable(onClick = onToggleChrome), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = medium.uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        Icon(GalleryIcons.Play, contentDescription = null, tint = CHROME_TEXT, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun TopChrome(
    medium: Medium,
    infoOpen: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleInfo: () -> Unit,
) {
    val accent = GalleryTheme.colors.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xAE000000), Color.Transparent)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAction(GalleryIcons.Back, onClick = onBack, tint = CHROME_TEXT)
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                medium.name,
                style = GalleryTheme.type.meta,
                color = CHROME_TEXT,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                SimpleDateFormat("d MMMM yyyy · HH:mm", LocalConfiguration.current.locales[0])
                    .format(Date(medium.dateTaken)),
                style = GalleryTheme.type.meta,
                color = CHROME_TEXT.copy(alpha = 0.62f),
            )
        }
        IconAction(
            GalleryIcons.Heart,
            onClick = onToggleFavorite,
            tint = if (medium.isFavorite) accent else CHROME_TEXT,
        )
        IconAction(GalleryIcons.Info, onClick = onToggleInfo, tint = if (infoOpen) accent else CHROME_TEXT)
    }
}

@Composable
private fun BottomChrome(
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onSetAs: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB8000000))))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAction(GalleryIcons.Share, onClick = onShare, tint = CHROME_TEXT)
        IconAction(GalleryIcons.Edit, onClick = onEdit, tint = CHROME_TEXT)
        IconAction(GalleryIcons.SetAs, onClick = onSetAs, tint = CHROME_TEXT)
        IconAction(GalleryIcons.Trash, onClick = onDelete, tint = CHROME_TEXT)
    }
}

@Composable
private fun DetailsPanel(medium: Medium, details: MediaDetails, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .background(Color(0xD10B0B0C), RoundedCornerShape(6.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.details_title),
                style = GalleryTheme.type.section,
                color = CHROME_TEXT.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f),
            )
            Icon(
                GalleryIcons.Close,
                contentDescription = null,
                tint = CHROME_TEXT.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp).clickable(onClick = onClose),
            )
        }
        DetailRow(stringResource(R.string.details_size), formatSize(medium.size))
        details.dimensions?.let { DetailRow(stringResource(R.string.details_dimensions), it) }
        details.camera?.let { DetailRow(stringResource(R.string.details_camera), it) }
        details.exposure?.let { DetailRow(stringResource(R.string.details_exposure), it) }
        details.path?.let { DetailRow(stringResource(R.string.details_path), it) }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = GalleryTheme.type.body, color = CHROME_TEXT.copy(alpha = 0.55f))
        Text(value, style = GalleryTheme.type.meta, color = CHROME_TEXT)
    }
}

private fun mimeOf(medium: Medium) = if (medium.type == MediaType.Video) "video/*" else "image/*"

private fun shareIntent(medium: Medium) = Intent(Intent.ACTION_SEND).apply {
    type = mimeOf(medium)
    putExtra(Intent.EXTRA_STREAM, medium.uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

private fun editIntent(medium: Medium) = Intent(Intent.ACTION_EDIT).apply {
    setDataAndType(medium.uri, mimeOf(medium))
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
}

private fun setAsIntent(medium: Medium) = Intent(Intent.ACTION_ATTACH_DATA).apply {
    setDataAndType(medium.uri, mimeOf(medium))
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
