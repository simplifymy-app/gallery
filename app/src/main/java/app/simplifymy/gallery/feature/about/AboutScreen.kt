package app.simplifymy.gallery.feature.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.simplifymy.gallery.R
import app.simplifymy.gallery.ui.component.GalleryIcons
import app.simplifymy.gallery.ui.component.IconAction
import app.simplifymy.gallery.ui.component.iconSize
import app.simplifymy.gallery.ui.theme.GalleryFont
import app.simplifymy.gallery.ui.theme.GalleryTheme
import app.simplifymy.gallery.ui.theme.Spacing

private val Mono = FontFamily(Font(R.font.ibm_plex_mono_medium))
private val Hand = FontFamily(Font(R.font.caveat_semibold, FontWeight.SemiBold))

private const val WEBSITE_URL = "https://simplifymy.app/"

private class Credit(val name: String, val by: String, val license: String)

private val Credits = listOf(
    Credit("Jetpack Compose & Material 3", "Google", "Apache-2.0"),
    Credit("AndroidX Room, DataStore, Navigation", "Google", "Apache-2.0"),
    Credit("Hilt & Dagger", "Google", "Apache-2.0"),
    Credit("Media3 ExoPlayer", "Google", "Apache-2.0"),
    Credit("Coil", "Coil Contributors", "Apache-2.0"),
    Credit("Telephoto", "Saket Narayan", "Apache-2.0"),
    Credit("Kotlin & kotlinx.serialization", "JetBrains", "Apache-2.0"),
    Credit("Space Grotesk", "Florian Karsten", "OFL-1.1"),
    Credit("Outfit", "Rodrigo Fuenzalida", "OFL-1.1"),
    Credit("IBM Plex Sans & Mono", "IBM", "OFL-1.1"),
    Credit("Caveat", "Impallari Type", "OFL-1.1"),
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = GalleryTheme.colors
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAction(GalleryIcons.Back, onClick = onBack)
            Text(stringResource(R.string.setting_about), style = GalleryTheme.type.title, color = colors.text)
        }

        val scroll = rememberScrollState()
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Logo()
            Text(
                stringResource(R.string.about_badges),
                style = GalleryTheme.type.section,
                color = colors.textMuted,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(if (scroll.value > 0) colors.hairline else Color.Transparent),
        )

        Column(
            modifier = Modifier.verticalScroll(scroll).padding(top = 6.dp, bottom = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Section(stringResource(R.string.about_section_app)) {
                Paragraph(stringResource(R.string.about_tagline), GalleryTheme.colors.text)
                Paragraph(stringResource(R.string.about_info), GalleryTheme.colors.textMuted)
                ValueRow(stringResource(R.string.about_version), version)
                ValueRow(stringResource(R.string.about_license), "MPL-2.0")
            }

            Section(stringResource(R.string.about_section_credits)) {
                Paragraph(stringResource(R.string.about_credits_hint), GalleryTheme.colors.textMuted)
                Credits.forEach { CreditRow(it) }
            }

            Section(stringResource(R.string.about_section_links)) {
                LinkRow(stringResource(R.string.about_website), stringResource(R.string.about_website_hint)) {
                    uriHandler.openUri(WEBSITE_URL)
                }
            }
        }
    }
}

@Composable
private fun Logo() {
    val colors = GalleryTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Duck(Modifier.size(width = 48.dp, height = 56.dp))
        Spacer(Modifier.width(10.dp))
        Box {
            Column {
                Text(
                    "Simplify",
                    style = TextStyle(
                        fontFamily = GalleryFont.SpaceGrotesk.family,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 32.sp,
                        letterSpacing = (-0.03).em,
                        lineHeight = 32.sp,
                    ),
                    color = colors.text,
                )
                Text(
                    "GALLERY",
                    style = TextStyle(fontFamily = Mono, fontSize = 11.sp, letterSpacing = 0.24.em, lineHeight = 11.sp),
                    color = colors.textMuted,
                    modifier = Modifier.align(Alignment.End).padding(top = 7.dp),
                )
            }
            Text(
                "my",
                style = TextStyle(fontFamily = Hand, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, lineHeight = 38.sp),
                color = colors.accent,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 12.dp, y = 12.dp)
                    .rotate(-7f),
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = GalleryTheme.type.section,
            color = GalleryTheme.colors.textFaint,
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
private fun Paragraph(text: String, color: Color) {
    Text(
        text,
        style = GalleryTheme.type.body,
        color = color,
        modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 6.dp),
    )
}

@Composable
private fun CreditRow(credit: Credit) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(credit.name, style = GalleryTheme.type.body, color = colors.text)
            Text(credit.by, style = GalleryTheme.type.meta, color = colors.textFaint)
        }
        Text(credit.license, style = GalleryTheme.type.meta, color = colors.textMuted)
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = GalleryTheme.type.body, color = GalleryTheme.colors.text, modifier = Modifier.weight(1f))
        Text(value, style = GalleryTheme.type.meta, color = GalleryTheme.colors.textMuted)
    }
}

@Composable
private fun LinkRow(label: String, hint: String, onClick: () -> Unit) {
    val colors = GalleryTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.screen, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = GalleryTheme.type.body, color = colors.text)
            Text(hint, style = GalleryTheme.type.meta, color = colors.textFaint)
        }
        Icon(
            GalleryIcons.Chevron,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.iconSize(16.dp).rotate(-90f),
        )
    }
}
