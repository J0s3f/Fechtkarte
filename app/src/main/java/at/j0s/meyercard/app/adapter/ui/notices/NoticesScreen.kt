package at.j0s.meyercard.app.adapter.ui.notices

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import at.j0s.meyercard.app.R

/** One dependency (or dependency group) and the licence it ships under. */
data class NoticeEntry(val name: String, val licence: String)

/**
 * F-Droid submission (T7.7) requires a notices screen listing every dependency's licence and
 * the numeral font's OFL text — [entries] mirrors docs/ASSET_PROVENANCE.md's own "Runtime
 * dependencies" and "Fonts" tables directly, deliberately hand-maintained in step with that
 * document rather than parsed from the license-report plugin's generated output at build or
 * run time: this project has a handful of dependency groups, not hundreds, so a generated
 * report would be more machinery than the problem needs.
 */
@Composable
fun NoticesScreen(entries: List<NoticeEntry>, fontLicenceText: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.notices_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.notices_intro))
        entries.forEach { entry ->
            Column {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(entry.licence, style = MaterialTheme.typography.bodyMedium)
            }
        }

        HorizontalDivider()

        Text(stringResource(R.string.notices_font_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.notices_font_licence), style = MaterialTheme.typography.titleMedium)
        Text(fontLicenceText, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
    }
}

/** Mirrors docs/ASSET_PROVENANCE.md's "Runtime dependencies" table — update both together. */
val RUNTIME_DEPENDENCY_NOTICES = listOf(
    NoticeEntry("AndroidX Core, Activity, Lifecycle, Navigation", "Apache License, Version 2.0"),
    NoticeEntry("Jetpack Compose (UI, Foundation, Material 3)", "Apache License, Version 2.0"),
    NoticeEntry("Room", "Apache License, Version 2.0"),
    NoticeEntry("DataStore", "Apache License, Version 2.0"),
    NoticeEntry("kotlinx.serialization", "Apache License, Version 2.0"),
    NoticeEntry("Kotlin standard library", "Apache License, Version 2.0"),
)

internal fun Context.readFontLicenceAsset(): String =
    assets.open("licenses/unifraktur_maguntia_OFL.txt").bufferedReader().use { it.readText() }
