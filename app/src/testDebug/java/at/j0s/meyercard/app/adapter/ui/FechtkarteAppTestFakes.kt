package at.j0s.meyercard.app.adapter.ui

import at.j0s.meyercard.app.application.port.api.BrowseHistoricalCards
import at.j0s.meyercard.app.application.port.api.ExportCard
import at.j0s.meyercard.app.application.port.api.ShareCard
import at.j0s.meyercard.app.application.port.spi.ExportResult
import at.j0s.meyercard.app.application.port.spi.PreferencesStore
import at.j0s.meyercard.app.application.port.spi.ShareableCard
import at.j0s.meyercard.app.domain.CardLineStyle
import at.j0s.meyercard.app.domain.GenerationPreferences
import at.j0s.meyercard.app.domain.HistoricalDrill
import at.j0s.meyercard.app.domain.MeyerCard

/**
 * Shared fakes for [FechtkarteApp] tests. `internal`, not `private` per file: two test files in
 * this package each declaring their own `private` copy of the same names hit a real Kotlin
 * compiler limitation (`private` top-level declarations still collide by name across files in
 * the same package) — found by the compiler itself, not by choice.
 */

/** No drills/technique cards needed — the routes under test here never render an individual card's content. */
internal object FakeBrowseHistoricalCards : BrowseHistoricalCards {
    override suspend fun drills(): List<HistoricalDrill> = emptyList()
    override suspend fun techniqueCards(): List<MeyerCard> = emptyList()
}

internal class FakePreferencesStore(initial: GenerationPreferences = GenerationPreferences()) : PreferencesStore {
    private var preferences = initial

    /** How many times [load] has been called — a proxy for how many times Train has (re)generated a card. */
    var loadCallCount = 0
        private set

    override suspend fun load(): GenerationPreferences {
        loadCallCount++
        return preferences
    }

    override suspend fun save(preferences: GenerationPreferences) {
        this.preferences = preferences
    }
}

/** Never actually invoked by these tests — none of them click Save/Share — so failing loudly beats a silent stub. */
internal object FakeExportCard : ExportCard {
    override suspend fun asPng(card: MeyerCard, lineStyle: CardLineStyle): ExportResult = error("not exercised by this test")
    override suspend fun asPdf(card: MeyerCard, lineStyle: CardLineStyle): ExportResult = error("not exercised by this test")
}

internal object FakeShareCard : ShareCard {
    override suspend fun prepare(card: MeyerCard, lineStyle: CardLineStyle): ShareableCard = error("not exercised by this test")
}
