package app.simplifymy.gallery.feature.search

import app.simplifymy.gallery.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.simplifymy.gallery.core.model.MediaType
import app.simplifymy.gallery.core.model.Medium
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.component.MediaThumbnail
import app.simplifymy.gallery.ui.formatCount
import app.simplifymy.gallery.ui.formatDuration
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenMedium: (Medium) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val colors = GalleryTheme.colors
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = Spacing.screen, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(GalleryIcons.Back, onClick = onBack)
            Box(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                if (query.isEmpty()) {
                    Text(stringResource(R.string.search_placeholder), style = GalleryTheme.type.title, color = colors.textFaint)
                }
                BasicTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    singleLine = true,
                    textStyle = GalleryTheme.type.title.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            if (query.isNotEmpty()) {
                IconAction(GalleryIcons.Close, onClick = { viewModel.setQuery("") })
            }
        }

        if (query.isNotBlank()) {
            Text(
                if (results.isEmpty()) stringResource(R.string.search_no_matches)
                else stringResource(R.string.search_results, formatCount(results.size)),
                style = GalleryTheme.type.meta,
                color = colors.textMuted,
                modifier = Modifier.padding(start = Spacing.screen, bottom = 10.dp),
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.gridGutter),
            horizontalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.gridGutter),
        ) {
            items(results, key = { it.id }) { medium ->
                Box(Modifier.clickable { onOpenMedium(medium) }) {
                    MediaThumbnail(model = medium.uri, modifier = Modifier.fillMaxWidth())
                    if (medium.type == MediaType.Video) {
                        Text(
                            formatDuration(medium.durationMs),
                            style = GalleryTheme.type.meta,
                            color = androidx.compose.ui.graphics.Color(0xFFEDEDEF),
                            modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                        )
                    }
                }
            }
        }
    }
}
