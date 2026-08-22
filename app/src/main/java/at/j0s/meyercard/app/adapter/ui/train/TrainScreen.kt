package at.j0s.meyercard.app.adapter.ui.train

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.adapter.ui.CardArea
import at.j0s.meyercard.app.adapter.ui.MeyerSquareCard
import at.j0s.meyercard.app.domain.MeyerCard

/**
 * The Train screen (docs/PLAN.md §7): a generated card filling the screen,
 * regenerating on tap (T5.4 — "tap anywhere on the card") as well as via
 * the Generate button; shaking the device does the same, wired in
 * [at.j0s.meyercard.app.adapter.ui.FechtkarteApp]'s Train route via
 * [ShakeToGenerate] rather than here, since it has no visible element of
 * its own to attach to. [onSavePng] (T6.1) and [onSavePdf] (T6.2) export
 * the card to the user's gallery/downloads; [onShare] (T6.3) hands a copy
 * to another app without saving one, a separate action from either save.
 */
@Composable
fun TrainScreen(
    card: MeyerCard,
    onGenerate: () -> Unit,
    onConfigure: () -> Unit,
    onSavePng: () -> Unit,
    onSavePdf: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CardArea(modifier = Modifier.weight(1f)) {
            MeyerSquareCard(card, modifier = it.clickable(onClick = onGenerate))
        }
        // FlowRow, not Row: a fixed Row silently pushes whatever doesn't fit past the screen
        // edge, with no scroll to reach it. That is exactly what happened in French, where
        // "Enregistrer en PNG/PDF" are long enough to shove Share off-screen entirely — the
        // button was unreachable, not merely ugly. Wrapping adapts to whatever any translation
        // needs instead of relying on every language happening to be as short as English.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onGenerate) { Text(stringResource(R.string.generate)) }
            OutlinedButton(onClick = onConfigure) { Text(stringResource(R.string.configure)) }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onSavePng) { Text(stringResource(R.string.save_png)) }
            OutlinedButton(onClick = onSavePdf) { Text(stringResource(R.string.save_pdf)) }
            OutlinedButton(onClick = onShare) { Text(stringResource(R.string.share)) }
        }
    }
}
