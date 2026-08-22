package at.j0s.meyercard.app.adapter.ui.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.adapter.ui.CardArea
import at.j0s.meyercard.app.adapter.ui.MeyerSquareCard
import at.j0s.meyercard.app.adapter.ui.displayName
import at.j0s.meyercard.app.domain.Hand
import at.j0s.meyercard.app.domain.HistoricalDrill
import at.j0s.meyercard.app.domain.Instruction
import at.j0s.meyercard.app.domain.MeyerCard

private enum class LibraryTab(@StringRes val label: Int) {
    DRILLS(R.string.library_tab_drills),
    TECHNIQUES(R.string.library_tab_techniques),
}

/**
 * The Library screen (docs/PLAN.md §7): the 44 historical drills with a hand
 * toggle, and the 21 technique cards, each in their own tab with independent
 * filtering and first/previous/next/last/±10/random navigation — see
 * docs/NEXT_STEPS.md T3.3 for why two tabs rather than one combined list.
 */
@Composable
fun LibraryScreen(drills: List<HistoricalDrill>, techniqueCards: List<MeyerCard>, modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(LibraryTab.DRILLS) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab.ordinal) {
            Tab(
                selected = tab == LibraryTab.DRILLS,
                onClick = { tab = LibraryTab.DRILLS },
                text = { Text(stringResource(LibraryTab.DRILLS.label)) },
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            )
            Tab(
                selected = tab == LibraryTab.TECHNIQUES,
                onClick = { tab = LibraryTab.TECHNIQUES },
                text = { Text(stringResource(LibraryTab.TECHNIQUES.label)) },
                icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
            )
        }
        when (tab) {
            LibraryTab.DRILLS -> DrillsTab(drills, modifier = Modifier.weight(1f))
            LibraryTab.TECHNIQUES -> TechniquesTab(techniqueCards, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DrillsTab(drills: List<HistoricalDrill>, modifier: Modifier = Modifier) {
    var state by remember(drills) { mutableStateOf(DrillsLibraryState(drills)) }
    val actionCounts = remember(drills) { drills.map { it.rightHandCard.actions.size }.distinct().sorted() }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actionCounts.forEach { count ->
                SelectableChip(
                    selected = state.filter.actionCount == count,
                    label = pluralStringResource(R.plurals.library_filter_actions, count, count),
                    onClick = {
                        val newCount = if (state.filter.actionCount == count) null else count
                        state = state.withFilter(state.filter.copy(actionCount = newCount))
                    },
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (0..3).forEach { count ->
                SelectableChip(
                    selected = state.filter.thrustCount == count,
                    label = pluralStringResource(R.plurals.library_filter_thrusts, count, count),
                    onClick = {
                        val newCount = if (state.filter.thrustCount == count) null else count
                        state = state.withFilter(state.filter.copy(thrustCount = newCount))
                    },
                )
            }
        }
        TextButton(onClick = { state = state.toggleHand() }) {
            Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                " " + stringResource(
                    R.string.library_hand,
                    stringResource(
                        if (state.hand == Hand.RIGHT) R.string.library_hand_right else R.string.library_hand_left,
                    ),
                ),
            )
        }

        val current = state.current
        CardArea(modifier = Modifier.weight(1f)) {
            if (current != null) {
                MeyerSquareCard(current.card(state.hand), modifier = it)
            } else {
                Text(stringResource(R.string.library_no_drills_match), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (current != null) {
            Text(
                stringResource(R.string.library_drill_position, state.position.index + 1, state.visibleDrills.size),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        BrowseControls(
            onFirst = { state = state.first() },
            onFastBackward = { state = state.fastBackward() },
            onPrevious = { state = state.previous() },
            onNext = { state = state.next() },
            onFastForward = { state = state.fastForward() },
            onLast = { state = state.last() },
            onRandom = { state = state.random() },
        )
    }
}

@Composable
private fun TechniquesTab(cards: List<MeyerCard>, modifier: Modifier = Modifier) {
    var state by remember(cards) { mutableStateOf(TechniqueLibraryState(cards)) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SelectableChip(
                selected = state.filter.instruction == null,
                label = stringResource(R.string.library_filter_all),
                onClick = { state = state.withFilter(TechniqueFilter(null)) },
            )
            Instruction.entries.forEach { instruction ->
                SelectableChip(
                    selected = state.filter.instruction == instruction,
                    label = instruction.displayName(LocalContext.current.resources),
                    onClick = { state = state.withFilter(TechniqueFilter(instruction)) },
                )
            }
        }

        val current = state.current
        CardArea(modifier = Modifier.weight(1f)) {
            if (current != null) {
                MeyerSquareCard(current, modifier = it)
            } else {
                Text(stringResource(R.string.library_no_techniques_match), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (current != null) {
            Text(
                stringResource(R.string.library_card_position, state.position.index + 1, state.visibleCards.size),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        BrowseControls(
            onFirst = { state = state.first() },
            onFastBackward = { state = state.fastBackward() },
            onPrevious = { state = state.previous() },
            onNext = { state = state.next() },
            onFastForward = { state = state.fastForward() },
            onLast = { state = state.last() },
            onRandom = { state = state.random() },
        )
    }
}

/** A filter chip with a check mark when selected — Material3's FilterChip doesn't show one by default. */
@Composable
private fun SelectableChip(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
        modifier = modifier,
    )
}

/** First/±10/previous/next/±10/last/random — one row, shared by both tabs. */
@Composable
private fun BrowseControls(
    onFirst: () -> Unit,
    onFastBackward: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFastForward: () -> Unit,
    onLast: () -> Unit,
    onRandom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(onClick = onFirst) { Icon(Icons.Filled.FirstPage, contentDescription = stringResource(R.string.library_nav_first)) }
        IconButton(onClick = onFastBackward) { Icon(Icons.Filled.FastRewind, contentDescription = stringResource(R.string.library_nav_back_ten)) }
        IconButton(onClick = onPrevious) { Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.library_nav_previous)) }
        IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.library_nav_next)) }
        IconButton(onClick = onFastForward) { Icon(Icons.Filled.FastForward, contentDescription = stringResource(R.string.library_nav_forward_ten)) }
        IconButton(onClick = onLast) { Icon(Icons.AutoMirrored.Filled.LastPage, contentDescription = stringResource(R.string.library_nav_last)) }
        IconButton(onClick = onRandom) { Icon(Icons.Filled.Shuffle, contentDescription = stringResource(R.string.library_nav_random)) }
    }
}
