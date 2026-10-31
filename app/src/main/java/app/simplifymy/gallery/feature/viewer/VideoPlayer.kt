package app.simplifymy.gallery.feature.viewer

import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import app.simplifymy.gallery.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.MenuEntry
import app.simplifymy.gallery.ui.formatDuration
import app.simplifymy.gallery.ui.theme.ControlRadius
import app.simplifymy.gallery.ui.theme.GalleryTheme
import kotlinx.coroutines.delay
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

private val CHROME = Color(0xFFEDEDEF)
private val SPEEDS = List(8) { 0.25f * (it + 1) }

enum class VideoFit(@StringRes val labelRes: Int) {
    Fit(R.string.fit_fit),
    Fill(R.string.fit_fill),
    Stretch(R.string.fit_stretch),
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    uri: Uri,
    controlsVisible: Boolean,
    autoplayNext: Boolean,
    onToggleChrome: () -> Unit,
    onEnded: () -> Unit,
    onZoomedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var playing by rememberSaveable(uri) { mutableStateOf(true) }
    var muted by rememberSaveable { mutableStateOf(false) }
    var position by rememberSaveable(uri) { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var ratio by remember { mutableFloatStateOf(16f / 9f) }
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var fit by rememberSaveable { mutableStateOf(VideoFit.Fit) }
    val zoom = rememberZoomableState()

    LaunchedEffect(zoom) {
        snapshotFlow { (zoom.zoomFraction ?: 0f) > 0.01f }.collect(onZoomedChange)
    }

    DisposableEffect(uri, autoplayNext) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.repeatMode = if (autoplayNext) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
        player.seekTo(position)
        player.setPlaybackSpeed(speed)
        player.volume = if (muted) 0f else 1f
        player.prepare()
        player.playWhenReady = playing
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    ratio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && autoplayNext) onEnded()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            delay(250)
        }
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().clipToBounds().zoomable(zoom, onClick = { onToggleChrome() }),
            contentAlignment = Alignment.Center,
        ) {
            val containerRatio = maxWidth / maxHeight
            val wide = ratio > containerRatio
            val surface = when (fit) {
                VideoFit.Stretch -> Modifier.fillMaxSize()
                VideoFit.Fit -> if (wide) Modifier.size(maxWidth, maxWidth / ratio)
                else Modifier.size(maxHeight * ratio, maxHeight)
                VideoFit.Fill -> if (wide) Modifier.size(maxHeight * ratio, maxHeight)
                else Modifier.size(maxWidth, maxWidth / ratio)
            }
            PlayerSurface(player = player, modifier = surface)
        }

        AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0x99000000), CircleShape)
                    .clickable { if (playing) player.pause() else player.play() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playing) GalleryIcons.Pause else GalleryIcons.Play,
                    contentDescription = null,
                    tint = CHROME,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(formatDuration(position), style = GalleryTheme.type.meta, color = CHROME)
                Scrubber(
                    progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                    modifier = Modifier.weight(1f),
                ) { fraction ->
                    if (duration > 0) {
                        val target = (fraction.coerceIn(0f, 1f) * duration).toLong()
                        player.seekTo(target)
                        position = target
                    }
                }
                Text(formatDuration(duration), style = GalleryTheme.type.meta, color = CHROME)

                SpeedPicker(speed) {
                    speed = it
                    player.setPlaybackSpeed(it)
                }
                Pill(stringResource(fit.labelRes)) { fit = VideoFit.entries[(fit.ordinal + 1) % VideoFit.entries.size] }
                Icon(
                    if (muted) GalleryIcons.VolumeOff else GalleryIcons.VolumeOn,
                    contentDescription = null,
                    tint = CHROME,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            muted = !muted
                            player.volume = if (muted) 0f else 1f
                        },
                )
            }
        }
    }
}

@Composable
private fun Scrubber(progress: Float, modifier: Modifier = Modifier, onSeek: (Float) -> Unit) {
    val accent = GalleryTheme.colors.accent
    Box(
        modifier = modifier
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { onSeek(it.x / size.width) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> onSeek(change.position.x / size.width) }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0x59EDEDEF), RoundedCornerShape(2.dp))
        )
        Box(
            Modifier
                .fillMaxWidth(progress)
                .height(3.dp)
                .background(accent, RoundedCornerShape(2.dp))
        )
        Box(Modifier.fillMaxWidth(progress), contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(accent))
        }
    }
}

@Composable
private fun SpeedPicker(speed: Float, onSelect: (Float) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Pill(speed.speedLabel()) { open = true }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(6.dp),
            containerColor = GalleryTheme.colors.surface,
        ) {
            SPEEDS.forEach { option ->
                MenuEntry(option.speedLabel(), value = if (option == speed) "\u2713" else null) {
                    onSelect(option)
                    open = false
                }
            }
        }
    }
}

@Composable
private fun Pill(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = GalleryTheme.type.meta,
        color = CHROME,
        modifier = Modifier
            .background(Color(0x59000000), RoundedCornerShape(ControlRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

private fun Float.speedLabel() = "${toString().trimEnd('0').trimEnd('.')}×"
