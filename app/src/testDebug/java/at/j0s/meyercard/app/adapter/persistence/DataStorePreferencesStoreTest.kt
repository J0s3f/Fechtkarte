package at.j0s.meyercard.app.adapter.persistence

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.CardPalette
import at.j0s.meyercard.app.domain.GenerationPreferences
import at.j0s.meyercard.app.domain.MinimumAngularDistance
import at.j0s.meyercard.app.domain.NoRepeatedDirection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Robolectric, JUnit 4 bridged via the Vintage engine, debug-only — same
 * reasoning as [RoomCardRepositoryTest]. `PreferenceDataStoreFactory.create`
 * is used directly against a fresh temp file per test rather than
 * `Context.dataStore`, so tests don't collide with each other or need a real
 * `Context` at all.
 */
@RunWith(RobolectricTestRunner::class)
class DataStorePreferencesStoreTest {

    private fun newStore(): DataStorePreferencesStore {
        val file = File.createTempFile("preferences-test-${System.nanoTime()}", ".preferences_pb")
        file.deleteOnExit()
        return DataStorePreferencesStore(PreferenceDataStoreFactory.create(produceFile = { file }))
    }

    @Test
    fun `loading with nothing saved yet returns the documented defaults`() = runBlocking {
        assertEquals(GenerationPreferences(), newStore().load())
    }

    @Test
    fun `saved preferences round-trip exactly, including palettes and rules`() = runBlocking {
        val store = newStore()
        val saved = GenerationPreferences(
            actionCount = 6,
            thrustCount = 2,
            actionCountIsMaximum = true,
            thrustCountIsMaximum = true,
            rightHandPalette = CardPalette.VERDIGRIS,
            leftHandPalette = CardPalette.MOSS,
            enabledRules = listOf(NoRepeatedDirection, MinimumAngularDistance(3)),
            cardLineStyle = CardLineStyle.SEQUENCE,
        )

        store.save(saved)

        assertEquals(saved, store.load())
    }

    @Test
    fun `an unrecognised stored line style token falls back to the default rather than throwing`() = runBlocking {
        val file = File.createTempFile("preferences-test-${System.nanoTime()}", ".preferences_pb")
        file.deleteOnExit()
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        dataStore.edit { it[stringPreferencesKey("cardLineStyle")] = "SomeFutureStyle" }

        assertEquals(CardLineStyle.COMPASS, DataStorePreferencesStore(dataStore).load().cardLineStyle)
    }
}
