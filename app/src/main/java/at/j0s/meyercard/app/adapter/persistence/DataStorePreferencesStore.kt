package at.j0s.meyercard.app.adapter.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import at.j0s.meyercard.app.application.port.spi.PreferencesStore
import at.j0s.meyercard.app.domain.AlternateHands
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.GenerationPreferences
import at.j0s.meyercard.app.domain.GenerationRule
import at.j0s.meyercard.app.domain.MatchHistoricalDistribution
import at.j0s.meyercard.app.domain.MinimumAngularDistance
import at.j0s.meyercard.app.domain.NoRepeatedDirection
import at.j0s.meyercard.app.domain.OuterRingOnly
import kotlinx.coroutines.flow.first

/**
 * The one place a real `DataStore<Preferences>` gets constructed for the
 * running app — `preferencesDataStore` is itself a singleton-per-file
 * delegate, so this extension property must be declared exactly once
 * top-level, not recreated per call site, or the "multiple DataStores
 * active for this file" crash [DataStorePreferencesStore] itself was built
 * to stay testable without.
 */
val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "fechtkarte_preferences")

/**
 * `dataStore` is injected as an already-built `DataStore<Preferences>`, not
 * constructed here from a `Context` — keeps this class testable without
 * Android's `Context.dataStore` delegate, which crashes at runtime if more
 * than one `DataStore` instance is ever built for the same file. The real
 * app wires one up once, in its composition root.
 */
class DataStorePreferencesStore(private val dataStore: DataStore<Preferences>) : PreferencesStore {

    override suspend fun load(): GenerationPreferences {
        val stored = dataStore.data.first()
        val defaults = GenerationPreferences()
        return GenerationPreferences(
            actionCount = stored[ACTION_COUNT] ?: defaults.actionCount,
            thrustCount = stored[THRUST_COUNT] ?: defaults.thrustCount,
            rightHandPalette = stored[RIGHT_HAND_PALETTE]?.let { CardPalette.valueOf(it) } ?: defaults.rightHandPalette,
            leftHandPalette = stored[LEFT_HAND_PALETTE]?.let { CardPalette.valueOf(it) } ?: defaults.leftHandPalette,
            enabledRules = stored[ENABLED_RULES]?.mapNotNull { it.toGenerationRule() } ?: defaults.enabledRules,
        )
    }

    override suspend fun save(preferences: GenerationPreferences) {
        dataStore.edit { stored ->
            stored[ACTION_COUNT] = preferences.actionCount
            stored[THRUST_COUNT] = preferences.thrustCount
            stored[RIGHT_HAND_PALETTE] = preferences.rightHandPalette.name
            stored[LEFT_HAND_PALETTE] = preferences.leftHandPalette.name
            stored[ENABLED_RULES] = preferences.enabledRules.map { it.toToken() }.toSet()
        }
    }

    private companion object {
        val ACTION_COUNT = intPreferencesKey("actionCount")
        val THRUST_COUNT = intPreferencesKey("thrustCount")
        val RIGHT_HAND_PALETTE = stringPreferencesKey("rightHandPalette")
        val LEFT_HAND_PALETTE = stringPreferencesKey("leftHandPalette")
        val ENABLED_RULES = stringSetPreferencesKey("enabledRules")
    }
}

/**
 * DataStore Preferences only stores primitives (Int/Long/Double/Float/
 * Boolean/String/Set<String>), so [GenerationRule]s need a flat string
 * encoding. The `when`/`is` chain below enumerates concrete rule types, but
 * it's a persistence-boundary concern — turning open objects into storable
 * tokens and back — not the "polymorphic behaviour selection via type
 * switch" T4.2's own doc comment warns against; `GenerationRule` itself
 * still has no `when` over its own implementations anywhere in the
 * generator or its rules.
 */
private fun GenerationRule.toToken(): String = when (this) {
    NoRepeatedDirection -> "NoRepeatedDirection"
    AlternateHands -> "AlternateHands"
    OuterRingOnly -> "OuterRingOnly"
    MatchHistoricalDistribution -> "MatchHistoricalDistribution"
    is MinimumAngularDistance -> "MinimumAngularDistance:$minSteps"
    else -> error("Unrecognised GenerationRule: $this")
}

private fun String.toGenerationRule(): GenerationRule? = when {
    this == "NoRepeatedDirection" -> NoRepeatedDirection
    this == "AlternateHands" -> AlternateHands
    this == "OuterRingOnly" -> OuterRingOnly
    this == "MatchHistoricalDistribution" -> MatchHistoricalDistribution
    startsWith("MinimumAngularDistance:") -> substringAfter(":").toIntOrNull()?.let { MinimumAngularDistance(it) }
    else -> null
}
