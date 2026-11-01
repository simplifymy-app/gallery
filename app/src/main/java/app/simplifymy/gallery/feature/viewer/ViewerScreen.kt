package app.simplifymy.gallery.feature.viewer

import app.simplifymy.gallery.R
import androidx.compose.ui.res.stringResource
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
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
import app.simplifymy.gallery.core.media.MetadataEdit
import app.simplifymy.gallery.core.model.MediaType
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.component.MenuEntry
import app.simplifymy.gallery.ui.component.OverflowMenu
import app.simplifymy.gallery.feature.files.FileAction
import app.simplifymy.gallery.feature.files.FileActions
import app.simplifymy.gallery.ui.formatSize
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CHROME_TEXT = Color(0xFFEDEDEF)

@Composable
fun ViewerScreen(
    onBack: () -> Unit,
    onEdit: (Medium) -> Unit,
    viewModel: ViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var chrome by remember { mutableStateOf(true) }
    var info by remember { mutableStateOf(false) }
    var zoomed by remember { mutableStateOf(false) }
    var fileAction by remember { mutableStateOf<FileAction?>(null) }
    var metadata by remember { mutableStateOf(false) }
    var pendingEdit by remember { mutableStateOf<Pair<Medium, MetadataEdit>?>(null) }
    val pull = remember { Animatable(0f) }
    val swipeThreshold = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
    val scope = rememberCoroutineScope()

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { if (it.resultCode == Activity.RESULT_OK) onBack() }

    val metadataConsent = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val (medium, edit) = pendingEdit ?: return@rememberLauncherForActivityResult
        pendingEdit = null
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.editMetadata(medium, edit) { saved ->
                if (!saved) Toast.makeText(context, R.string.metadata_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (state.media.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val pagerState = rememberPagerState(initialPage = state.startIndex) { state.media.size }
    val current = state.media.getOrNull(pagerState.currentPage) ?: return
    val panorama = rememberPanorama(current.uri, current.type == MediaType.Video)
    var flat by rememberSaveable(current.id) { mutableStateOf(false) }
    val sphere = panorama?.takeIf { !flat }
    val locked by rememberUpdatedState(zoomed || sphere != null)

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            zoomed = false
            state.media.getOrNull(page)?.let { viewModel.loadDetails(it) }
        }
    }

    SystemBars(visible = chrome)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalSwipe(
                enabled = { !locked },
                onDrag = { dy -> if (!info) scope.launch { pull.snapTo(dy.coerceAtLeast(0f)) } },
                onRelease = { dy ->
                    when {
                        dy > swipeThreshold && info -> info = false
                        dy > swipeThreshold -> onBack()
                        dy < -swipeThreshold -> { chrome = true; info = true }
                    }
                    scope.launch { pull.animateTo(0f) }
                },
            ),
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> state.media[page].id },
            userScrollEnabled = sphere == null,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                translationY = pull.value
                val shrink = 1f - (pull.value / size.height).coerceIn(0f, 1f) * 0.3f
                scaleX = shrink
                scaleY = shrink
            },
        ) { page ->
            val medium = state.media[page]
            if (medium.type == MediaType.Video) {
                if (page == pagerState.currentPage) {
                    VideoPlayer(
                        uri = medium.uri,
                        title = medium.name,
                        controlsVisible = chrome,
                        autoplayNext = state.autoplayNextVideo,
                        backgroundPlayback = state.backgroundPlayback,
                        onToggleChrome = { chrome = !chrome },
                        onEnded = {
                            val ahead = state.media.drop(page + 1).indexOfFirst { it.type == MediaType.Video }
                            if (ahead >= 0) scope.launch { pagerState.animateScrollToPage(page + 1 + ahead) }
                        },
                        onZoomedChange = { zoomed = it },
                        panorama = sphere,
                    )
                } else {
                    VideoPoster(medium) { chrome = !chrome }
                }
            } else if (page == pagerState.currentPage && sphere != null) {
                PanoramaPhoto(medium.uri, sphere, onTap = { chrome = !chrome })
            } else {
                val zoom = rememberDeepZoomState()
                if (page == pagerState.currentPage) {
                    LaunchedEffect(zoom) {
                        snapshotFlow { (zoom.zoomableState.zoomFraction ?: 0f) > 0.01f }.collect { zoomed = it }
                    }
                }
                ZoomableAsyncImage(
                    model = medium.uri,
                    contentDescription = null,
                    state = zoom,
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
                panorama = panorama != null,
                panoramaOn = sphere != null,
                onTogglePanorama = { flat = !flat },
                onBack = onBack,
                onToggleFavorite = { viewModel.toggleFavorite(current) },
                onToggleInfo = { info = !info },
            )
        }

        AnimatedVisibility(
            visible = chrome && info,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(
                bottom = if (current.type == MediaType.Video) {
                    VIDEO_CONTROLS_BOTTOM + VIDEO_CONTROLS_HEIGHT + 12.dp
                } else {
                    108.dp
                },
            ),
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
                onEdit = { onEdit(current) },
                onSetAs = { runCatching { context.startActivity(setAsIntent(current)) } },
                onDelete = {
                    trashLauncher.launch(IntentSenderRequest.Builder(viewModel.trash(current)).build())
                },
                onFileAction = { fileAction = it },
                onMetadata = { metadata = true },
            )
        }
    }

    if (metadata) {
        MetadataSheet(
            medium = current,
            details = state.details,
            onSave = { edit ->
                metadata = false
                pendingEdit = current to edit
                metadataConsent.launch(IntentSenderRequest.Builder(viewModel.writeRequest(current)).build())
            },
            onDismiss = { metadata = false },
        )
    }

    FileActions(
        action = fileAction,
        media = listOf(current),
        onDone = {
            fileAction = null
            if (it == FileAction.Move) onBack()
        },
        onDismiss = { fileAction = null },
    )
}

@Composable
fun StandaloneViewer(uri: Uri, isVideo: Boolean, onBack: () -> Unit) {
    var chrome by remember { mutableStateOf(true) }
    val panorama = rememberPanorama(uri, isVideo)
    var flat by rememberSaveable { mutableStateOf(false) }
    val sphere = panorama?.takeIf { !flat }
    SystemBars(visible = chrome)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (!isVideo && sphere != null) {
            PanoramaPhoto(uri, sphere, onTap = { chrome = !chrome })
        } else if (isVideo) {
            VideoPlayer(
                uri = uri,
                title = uri.lastPathSegment.orEmpty(),
                controlsVisible = chrome,
                autoplayNext = false,
                backgroundPlayback = false,
                onToggleChrome = { chrome = !chrome },
                onEnded = {},
                onZoomedChange = {},
                panorama = sphere,
            )
        } else {
            ZoomableAsyncImage(
                model = uri,
                contentDescription = null,
                state = rememberDeepZoomState(),
                modifier = Modifier.fillMaxSize(),
                onClick = { chrome = !chrome },
            )
        }
        AnimatedVisibility(
            visible = chrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xAE000000), Color.Transparent)))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp),
            ) {
                IconAction(GalleryIcons.Back, onClick = onBack, tint = CHROME_TEXT)
                Spacer(Modifier.weight(1f))
                if (panorama != null) PanoramaToggle(sphere != null) { flat = !flat }
            }
        }
    }
}

@Composable
private fun PanoramaToggle(on: Boolean, onToggle: () -> Unit) {
    IconAction(
        GalleryIcons.Panorama,
        onClick = onToggle,
        label = stringResource(R.string.action_panorama),
        tint = if (on) GalleryTheme.colors.accent else CHROME_TEXT,
    )
}

private const val MAX_ZOOM_OF_PIXELS = 2f
private const val MAX_ZOOM_OF_SCREEN = 6f

@Composable
private fun rememberDeepZoomState(): ZoomableImageState {
    var fitScale by remember { mutableFloatStateOf(0f) }
    val zoomable = rememberZoomableState(
        zoomSpec = ZoomSpec(maxZoomFactor = max(MAX_ZOOM_OF_PIXELS, fitScale * MAX_ZOOM_OF_SCREEN)),
    )
    LaunchedEffect(zoomable) {
        snapshotFlow {
            zoomable.contentTransformation.takeIf { it.isSpecified }
                ?.scaleMetadata?.initialScale?.takeIf { it != ScaleFactor.Unspecified }?.scaleX
        }.collect { if (it != null && it > 0f) fitScale = it }
    }
    return rememberZoomableImageState(zoomable)
}

private val SWIPE_THRESHOLD = 80.dp

private fun Modifier.verticalSwipe(
    enabled: () -> Boolean,
    onDrag: (Float) -> Unit,
    onRelease: (Float) -> Unit,
) = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var total = Offset.Zero
        var tracking = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null || !change.pressed || event.changes.count { it.pressed } > 1) break
            total += change.positionChange()
            if (!tracking) {
                if (total.getDistance() < viewConfiguration.touchSlop) continue
                if (!enabled() || abs(total.y) < abs(total.x) * 2) break
                tracking = true
            }
            change.consume()
            onDrag(total.y)
        }
        if (tracking) onRelease(total.y)
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
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val controller = activity?.let {
            WindowCompat.getInsetsController(it.window, it.window.decorView)
        }
        val wasLight = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null && wasLight != null) {
                controller.isAppearanceLightStatusBars = wasLight
                controller.isAppearanceLightNavigationBars = wasLight
            }
        }
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
    panorama: Boolean,
    panoramaOn: Boolean,
    onTogglePanorama: () -> Unit,
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
        if (panorama) PanoramaToggle(panoramaOn, onTogglePanorama)
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
    onFileAction: (FileAction) -> Unit,
    onMetadata: () -> Unit,
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
        IconAction(GalleryIcons.Edit, onClick = onEdit, label = stringResource(R.string.action_edit), tint = CHROME_TEXT)
        IconAction(GalleryIcons.SetAs, onClick = onSetAs, tint = CHROME_TEXT)
        IconAction(GalleryIcons.Trash, onClick = onDelete, tint = CHROME_TEXT)
        OverflowMenu(tint = CHROME_TEXT) { dismiss ->
            FileAction.entries.forEach { action ->
                MenuEntry(stringResource(action.labelRes)) { dismiss(); onFileAction(action) }
            }
            MenuEntry(stringResource(R.string.action_metadata)) { dismiss(); onMetadata() }
        }
    }
}

@Composable
private fun DetailsPanel(medium: Medium, details: MediaDetails, onClose: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .background(Color(0xD10B0B0C), shape)
            .border(1.dp, CHROME_TEXT.copy(alpha = 0.14f), shape)
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

private fun setAsIntent(medium: Medium) = Intent(Intent.ACTION_ATTACH_DATA).apply {
    setDataAndType(medium.uri, mimeOf(medium))
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
