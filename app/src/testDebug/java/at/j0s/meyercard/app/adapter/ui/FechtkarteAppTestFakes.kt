package at.j0s.meyercard.app.adapter.ui

import android.net.Uri
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
    /** Read directly rather than through the suspending [load] -- lets a plain (non-coroutine) test body check what the last [save] call actually stored. */
    var preferences = initial
        private set

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

/**
 * Tracks call counts rather than returning anything meaningful -- these tests check that a
 * button click reached the port, not what it did with the card. [shouldThrow] exists for the
 * one other thing worth checking here: that a failed export surfaces as the error toast
 * `TrainRoute.save` promises, not a silent failure or an uncaught crash.
 */
internal class FakeExportCard(private val shouldThrow: Boolean = false) : ExportCard {
    var pngCallCount = 0
        private set
    var pdfCallCount = 0
        private set

    override suspend fun asPng(card: MeyerCard, lineStyle: CardLineStyle): ExportResult {
        pngCallCount++
        if (shouldThrow) error("export failed")
        return ExportResult(Uri.EMPTY, "fake.png")
    }

    override suspend fun asPdf(card: MeyerCard, lineStyle: CardLineStyle): ExportResult {
        pdfCallCount++
        if (shouldThrow) error("export failed")
        return ExportResult(Uri.EMPTY, "fake.pdf")
    }
}

/** [shouldThrow]: see [FakeExportCard] -- same reasoning, for `TrainRoute.share`'s own error toast. */
internal class FakeShareCard(private val shouldThrow: Boolean = false) : ShareCard {
    var callCount = 0
        private set

    override suspend fun prepare(card: MeyerCard, lineStyle: CardLineStyle): ShareableCard {
        callCount++
        if (shouldThrow) error("share failed")
        return ShareableCard(Uri.EMPTY, "image/png")
    }
}
