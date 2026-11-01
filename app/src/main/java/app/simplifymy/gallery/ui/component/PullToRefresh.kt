package app.simplifymy.gallery.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.simplifymy.gallery.ui.theme.GalleryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    atTop: () -> Boolean,
    modifier: Modifier = Modifier,
    indicatorOffset: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()
    val thresholdPx = with(LocalDensity.current) { PULL_THRESHOLD.toPx() }
    val currentRefreshing by rememberUpdatedState(refreshing)
    val currentRefresh by rememberUpdatedState(onRefresh)
    val pull = remember(state, thresholdPx) {
        PullConnection(state, scope, thresholdPx, { currentRefreshing }, { currentRefresh() })
    }

    LaunchedEffect(refreshing) {
        if (refreshing) state.animateToThreshold() else state.animateToHidden()
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pull.armed = atTop()
                }
            }
            .nestedScroll(pull),
    ) {
        content()
        PullToRefreshDefaults.Indicator(
            state = state,
            isRefreshing = refreshing,
            containerColor = GalleryTheme.colors.surface,
            color = GalleryTheme.colors.accent,
            threshold = PULL_THRESHOLD,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = indicatorOffset),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class PullConnection(
    private val state: PullToRefreshState,
    private val scope: CoroutineScope,
    private val thresholdPx: Float,
    private val refreshing: () -> Boolean,
    private val onRefresh: () -> Unit,
) : NestedScrollConnection {

    var armed = false
    private var pulled = 0f

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (pulled <= 0f || available.y >= 0f) return Offset.Zero
        val taken = maxOf(available.y, -pulled / DRAG_RATE)
        moveTo(pulled + taken * DRAG_RATE)
        return Offset(0f, taken)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (!armed || source != NestedScrollSource.UserInput || available.y <= 0f || refreshing()) return Offset.Zero
        moveTo(pulled + available.y * DRAG_RATE)
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        armed = false
        if (pulled <= 0f) return Velocity.Zero
        val reached = pulled >= thresholdPx
        pulled = 0f
        if (reached) {
            onRefresh()
            state.animateToThreshold()
        } else {
            state.animateToHidden()
        }
        return available
    }

    private fun moveTo(distance: Float) {
        pulled = distance.coerceIn(0f, thresholdPx * MAX_STRETCH)
        scope.launch { state.snapTo(pulled / thresholdPx) }
    }
}

private val PULL_THRESHOLD = 80.dp
private const val DRAG_RATE = 0.5f
private const val MAX_STRETCH = 1.5f
