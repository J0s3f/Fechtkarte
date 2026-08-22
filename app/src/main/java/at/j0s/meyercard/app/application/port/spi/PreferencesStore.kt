package at.j0s.meyercard.app.application.port.spi

import at.j0s.meyercard.app.domain.GenerationPreferences

/** Persists the user's generator configuration. */
interface PreferencesStore {
    suspend fun load(): GenerationPreferences
    suspend fun save(preferences: GenerationPreferences)
}
