package at.j0s.meyercard.app.adapter.ui.sources

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.j0s.meyercard.app.R

/**
 * The Sources screen: general credit to Joachim Meyer's fencing tradition, plus the one page
 * from either source manuscript that actually matches Fechtkarte's own card shape (a centre
 * point, eight lines, two rings) -- Rostock MS Var.82, folio 2v. Confirmed by visually reviewing
 * every page of both digitised manuscripts; Lund MS A.4º.2 has no page in this shape (combat-pose
 * illustrations and an unrelated proportion diagram instead) -- see DESIGN_CHOICES.md. Shows
 * Fechtkarte's own redraw of the wheel by default, with a toggle to the real scan, per the
 * project owner's decision that the redraw should read as part of the app's own visual family
 * while still making the real artifact available.
 *
 * Deliberately does not claim any specific card is drawn from a specific manuscript page --
 * DESIGN_CHOICES.md's T9.13 entry already walked back an overclaim of exactly that shape for the
 * card dataset; this screen's copy is general credit only.
 */
@Composable
fun SourcesScreen(scan: ImageBitmap, modifier: Modifier = Modifier) {
    var showingScan by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.sources_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.sources_intro))

        Text(stringResource(R.string.sources_wheel_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.sources_wheel_body))
        if (showingScan) {
            Image(
                bitmap = scan,
                contentDescription = stringResource(R.string.sources_view_scan),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_source_wheel),
                contentDescription = stringResource(R.string.sources_view_redraw),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TextButton(onClick = { showingScan = !showingScan }) {
            Text(stringResource(if (showingScan) R.string.sources_view_redraw else R.string.sources_view_scan))
        }

        Text(stringResource(R.string.sources_citation_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.sources_citation_lund))
        Text(stringResource(R.string.sources_citation_rostock))
    }
}

internal fun Context.readSourcesScanAsset(): ImageBitmap =
    assets.open("sources/rostock_f2v_scan.jpg").use { BitmapFactory.decodeStream(it) }.asImageBitmap()
