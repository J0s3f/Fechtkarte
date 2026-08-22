package at.j0s.meyercard.app.adapter.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.adapter.ui.CardArea
import at.j0s.meyercard.app.adapter.ui.MeyerSquareCard
import at.j0s.meyercard.app.adapter.ui.spoken
import at.j0s.meyercard.app.domain.Action
import at.j0s.meyercard.app.domain.CardId
import at.j0s.meyercard.app.domain.CardOrigin
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.Direction
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.MeyerCard
import at.j0s.meyercard.app.domain.Radius
import at.j0s.meyercard.app.domain.Slot
import java.time.Instant

/**
 * The Learn screen: original copy explaining the notation, plus a live
 * worked example. Written directly from the facts (correcting the historical "Lundt" typo to
 * "Lund") rather than paraphrased from any prior source — this project clean-room rewrites its
 * content for FOSS distribution.
 */
@Composable
fun LearnScreen(onNoticesClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.learn_about_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.learn_about_body))

        Text(stringResource(R.string.learn_how_to_read_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.learn_how_to_read_notation))
        Text(stringResource(R.string.learn_how_to_read_thrust))
        Text(stringResource(R.string.learn_how_to_read_branching))
        Text(stringResource(R.string.learn_how_to_read_colour))
        Text(stringResource(R.string.learn_how_to_read_repetitions))

        Text(stringResource(R.string.learn_worked_example_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.learn_worked_example_intro))
        CardArea { MeyerSquareCard(WORKED_EXAMPLE_CARD, modifier = it) }
        val resources = LocalContext.current.resources
        WORKED_EXAMPLE_CARD.actions.sortedBy { it.sequenceNumber }.forEach { action ->
            Text(action.spoken(resources).replaceFirstChar(Char::uppercase) + ".")
        }

        TextButton(onClick = onNoticesClick) { Text(stringResource(R.string.learn_open_source_notices)) }
    }
}

private val WORKED_EXAMPLE_CARD = MeyerCard(
    id = CardId(0L),
    actions = listOf(
        Action(1, Slot(Direction.NW, Radius.OUTER), isThrust = false),
        Action(2, Slot(Direction.E, Radius.OUTER), isThrust = true),
        Action(3, Slot(Direction.S, Radius.OUTER), isThrust = false),
    ),
    hand = Hand.RIGHT,
    palette = CardPalette.default(Hand.RIGHT),
    origin = CardOrigin.Generated(Instant.EPOCH),
)
