package at.j0s.meyercard.app.adapter.ui.configure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import at.j0s.meyercard.app.BuildConfig
import at.j0s.meyercard.app.R
import at.j0s.meyercard.app.domain.AlternateHands
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.MatchHistoricalDistribution
import at.j0s.meyercard.app.domain.MinimumAngularDistance
import at.j0s.meyercard.app.domain.NoRepeatedDirection
import at.j0s.meyercard.app.domain.OuterRingOnly

/**
 * The Configure screen: sliders for `actionCount` and
 * `thrustCount`, palette pickers per hand, a card line style picker, and generation rule
 * toggles. [state] and [onStateChange] follow the same hoisted-state shape as the
 * Library screen's tabs — the caller owns persistence (saving to
 * `PreferencesStore`), this composable only edits in memory.
 */
@Composable
fun ConfigureScreen(state: ConfigureScreenState, onStateChange: (ConfigureScreenState) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(
                if (state.preferences.actionCountIsMaximum) {
                    pluralStringResource(R.plurals.configure_actions_maximum, state.preferences.actionCount, state.preferences.actionCount)
                } else {
                    stringResource(R.string.configure_actions, state.preferences.actionCount)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Slider(
                value = state.preferences.actionCount.toFloat(),
                onValueChange = { onStateChange(state.withActionCount(it.toInt())) },
                valueRange = 1f..8f,
                steps = 6,
            )
            RuleToggleRow(
                label = stringResource(R.string.configure_count_is_maximum),
                checked = state.preferences.actionCountIsMaximum,
                onCheckedChange = { onStateChange(state.toggleActionCountIsMaximum()) },
            )
        }

        Column {
            Text(
                if (state.preferences.thrustCountIsMaximum) {
                    pluralStringResource(R.plurals.configure_thrusts_maximum, state.preferences.thrustCount, state.preferences.thrustCount)
                } else {
                    stringResource(R.string.configure_thrusts, state.preferences.thrustCount)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Slider(
                value = state.preferences.thrustCount.toFloat(),
                onValueChange = { onStateChange(state.withThrustCount(it.toInt())) },
                valueRange = 0f..state.preferences.actionCount.toFloat(),
                steps = (state.preferences.actionCount - 1).coerceAtLeast(0),
            )
            RuleToggleRow(
                label = stringResource(R.string.configure_count_is_maximum),
                checked = state.preferences.thrustCountIsMaximum,
                onCheckedChange = { onStateChange(state.toggleThrustCountIsMaximum()) },
            )
        }

        HorizontalDivider()

        PalettePicker(
            label = stringResource(R.string.configure_right_hand_colour),
            selected = state.preferences.rightHandPalette,
            onSelect = { onStateChange(state.withRightHandPalette(it)) },
        )
        PalettePicker(
            label = stringResource(R.string.configure_left_hand_colour),
            selected = state.preferences.leftHandPalette,
            onSelect = { onStateChange(state.withLeftHandPalette(it)) },
        )

        HorizontalDivider()

        Text(stringResource(R.string.configure_card_appearance), style = MaterialTheme.typography.titleMedium)
        LineStylePicker(
            selected = state.preferences.cardLineStyle,
            onSelect = { onStateChange(state.withCardLineStyle(it)) },
        )

        HorizontalDivider()

        Text(stringResource(R.string.configure_interaction), style = MaterialTheme.typography.titleMedium)
        RuleToggleRow(
            label = stringResource(R.string.configure_shake_to_generate),
            checked = state.preferences.shakeToGenerateEnabled,
            onCheckedChange = { onStateChange(state.toggleShakeToGenerate()) },
        )

        HorizontalDivider()

        Text(stringResource(R.string.configure_generation_rules), style = MaterialTheme.typography.titleMedium)
        RuleToggleRow(
            label = stringResource(R.string.rule_no_repeated_direction),
            checked = state.isRuleEnabled(NoRepeatedDirection),
            onCheckedChange = { onStateChange(state.toggleRule(NoRepeatedDirection)) },
        )
        RuleToggleRow(
            label = stringResource(R.string.rule_minimum_angular_distance),
            checked = state.isMinimumAngularDistanceEnabled(),
            onCheckedChange = { onStateChange(state.toggleMinimumAngularDistance()) },
        )
        if (state.isMinimumAngularDistanceEnabled()) {
            val steps = state.preferences.enabledRules.filterIsInstance<MinimumAngularDistance>().first().minSteps
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(stringResource(R.string.rule_minimum_steps_apart, steps))
                Slider(
                    value = steps.toFloat(),
                    onValueChange = { onStateChange(state.withMinimumAngularDistanceSteps(it.toInt())) },
                    valueRange = 1f..4f,
                    steps = 2,
                )
            }
        }
        RuleToggleRow(
            label = stringResource(R.string.rule_alternate_hands),
            checked = state.isRuleEnabled(AlternateHands),
            onCheckedChange = { onStateChange(state.toggleRule(AlternateHands)) },
        )
        RuleToggleRow(
            label = stringResource(R.string.rule_outer_ring_only),
            checked = state.isRuleEnabled(OuterRingOnly),
            onCheckedChange = { onStateChange(state.toggleRule(OuterRingOnly)) },
        )
        RuleToggleRow(
            label = stringResource(R.string.rule_match_historical_distribution),
            checked = state.isRuleEnabled(MatchHistoricalDistribution),
            onCheckedChange = { onStateChange(state.toggleRule(MatchHistoricalDistribution)) },
        )

        HorizontalDivider()

        LanguagePicker()

        // Lets a user (or a bug report) confirm which build is actually running, rather than
        // assuming an app-store/sideloaded update applied just because it was triggered — an
        // update can silently not take effect from the data's point of view even when the APK
        // itself installed fine (an already-seeded local database isn't reseeded from a newer
        // bundled dataset on its own; see FechtkarteDatabase's own doc comment).
        Text(
            stringResource(R.string.configure_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * App language, independent of the device language (T9.8).
 *
 * Deliberately not part of [ConfigureScreenState]/`GenerationPreferences`: unlike every other
 * setting on this screen, this one isn't the app's data to own. `AppCompatDelegate` persists it
 * itself, and on Android 13+ hands it to the platform, so the same choice also appears in
 * Android's own per-app language settings — storing a second copy in DataStore would just be a
 * value that could drift out of sync with the one actually in effect.
 *
 * The selection is read back from [AppCompatDelegate.getApplicationLocales] rather than held in
 * local state, so it stays correct when the user changes it from system settings instead.
 */
@Composable
private fun LanguagePicker(modifier: Modifier = Modifier) {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')

    Column(modifier = modifier) {
        Text(stringResource(R.string.configure_language), style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { language ->
                FilterChip(
                    selected = current == language.tag,
                    onClick = {
                        AppCompatDelegate.setApplicationLocales(
                            if (language.tag.isEmpty()) {
                                LocaleListCompat.getEmptyLocaleList()
                            } else {
                                LocaleListCompat.forLanguageTags(language.tag)
                            },
                        )
                    },
                    label = { Text(language.label()) },
                )
            }
        }
    }
}

/**
 * `tag` is empty for "follow the system", which is what an empty `LocaleListCompat` means to
 * `AppCompatDelegate` — the default, so a user who never touches this setting keeps getting the
 * device language. The other labels are deliberately each written in their own language
 * (`translatable="false"` endonyms) rather than translated: someone who has the app in a
 * language they can't read needs to recognise their own language in this list to get out again.
 */
private enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    GERMAN("de"),
    FRENCH("fr"),
    ;

    @Composable
    fun label(): String = when (this) {
        SYSTEM -> stringResource(R.string.language_system_default)
        ENGLISH -> "English"
        GERMAN -> "Deutsch"
        FRENCH -> "Français"
    }
}

@Composable
private fun RuleToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        // testTag, not just the label text: this bare Row has no semantics boundary of its own
        // (no clickable/mergeDescendants), so Text and Switch flatten into the whole screen's
        // single, undifferentiated sibling list rather than being grouped as "this row's two
        // children" -- confirmed against the real semantics tree, not assumed. The tag is the
        // only reliable way a test can single out one specific row's own switch among several
        // otherwise-identical ones.
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(label))
    }
}

@Composable
private fun PalettePicker(label: String, selected: CardPalette, onSelect: (CardPalette) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardPalette.entries.forEach { palette ->
                val isSelected = palette == selected
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(palette.light))
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onSelect(palette) },
                )
            }
        }
    }
}

/**
 * A dropdown, not a switch: [CardLineStyle] is deliberately an enum, not a boolean, because more
 * styles than these two are expected (docs/LINE_STYLE_DESIGN.md's `BRIDGE`/`NONE`) — a picker
 * that already reads as "choose one of several" needs no rework when a third option arrives,
 * where a switch would need replacing outright. `ExposedDropdownMenuBox` over a `FilterChip` row
 * (this screen's usual precedent, see [PalettePicker]/[LanguagePicker]) for the same reason: a
 * chip row grows wider with every option, a dropdown doesn't.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineStylePicker(selected: CardLineStyle, onSelect: (CardLineStyle) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        TextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.configure_line_style)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CardLineStyle.entries.forEach { style ->
                DropdownMenuItem(
                    text = { Text(style.displayName()) },
                    onClick = {
                        onSelect(style)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun CardLineStyle.displayName(): String = when (this) {
    CardLineStyle.COMPASS -> stringResource(R.string.line_style_compass)
    CardLineStyle.SEQUENCE -> stringResource(R.string.line_style_sequence)
}
