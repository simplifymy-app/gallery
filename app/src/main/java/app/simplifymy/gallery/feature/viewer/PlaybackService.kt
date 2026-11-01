package app.simplifymy.gallery.feature.viewer

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken

class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        if (session == null) {
            session = activePlayer?.let { player ->
                MediaSession.Builder(this, player).apply { openAppIntent()?.let(::setSessionActivity) }.build()
            }
        }
        return session
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }

    private fun openAppIntent(): PendingIntent? =
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

    companion object {
        var activePlayer: Player? = null
    }
}

@Composable
fun PlaybackSessionEffect(player: Player) {
    val context = LocalContext.current
    DisposableEffect(player) {
        PlaybackService.activePlayer = player
        val service = ComponentName(context, PlaybackService::class.java)
        val controller = MediaController.Builder(context, SessionToken(context, service)).buildAsync()
        onDispose {
            MediaController.releaseFuture(controller)
            if (PlaybackService.activePlayer === player) PlaybackService.activePlayer = null
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }
}
