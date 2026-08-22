package at.j0s.meyercard.app.adapter.persistence

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric, not instrumentation — an emulator inside the build container
 * is more trouble than it is worth, same reasoning as T2.4's screenshot
 * tests. JUnit 4, bridged into this project's JUnit 5 test task via the
 * Vintage engine, and debug-only (`src/testDebug/`) for the same reason
 * those were: nothing under test here differs between build types.
 *
 * Uses the real bundled asset via [readOriginalCardsAsset], not a manually
 * supplied JSON string — the point is proving the actual seed path works,
 * not just the parsing logic T3.1 already covers directly.
 */
@RunWith(RobolectricTestRunner::class)
class RoomCardRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "test-${System.nanoTime()}.db"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `seeding the historical cards is idempotent across two app starts`() = runBlocking {
        val firstStart = FechtkarteDatabase.create(context, databaseName)
        val firstCards = RoomCardRepository(firstStart.historicalCardDao()) { context.readOriginalCardsAsset() }
            .allCards()
        firstStart.close()

        val secondStart = FechtkarteDatabase.create(context, databaseName)
        val secondCards = RoomCardRepository(secondStart.historicalCardDao()) { context.readOriginalCardsAsset() }
            .allCards()
        secondStart.close()

        assertEquals(109, firstCards.size)
        assertEquals(firstCards, secondCards)
    }

    /**
     * T8.4: [HistoricalCardDao]'s own doc comment names the risk this reproduces — the
     * count-then-insert seed check isn't atomic, so two [RoomCardRepository] instances built
     * against the *same* underlying database (exactly what happens across an `Activity`
     * recreation, e.g. a device rotation mid-seed — [at.j0s.meyercard.app.adapter.ui.MainActivity]
     * builds a fresh instance in `onCreate` every time) can race to seed it concurrently.
     *
     * `OnConflictStrategy.IGNORE` on `insertAll` already prevents that race from crashing or
     * duplicating rows — this test isn't guarding against corruption, it's guarding against
     * redundant work: without serialising the check, many concurrent first-time callers can
     * each decide the database is empty and each re-parse+re-insert the same 109 cards.
     */
    @Test
    fun `many concurrent repository instances seed the same database only once`() = runBlocking {
        val database = FechtkarteDatabase.create(context, databaseName)
        val parseCount = AtomicInteger(0)
        fun repository() = RoomCardRepository(database.historicalCardDao()) {
            parseCount.incrementAndGet()
            context.readOriginalCardsAsset()
        }

        val results = List(10) { async(Dispatchers.IO) { repository().allCards() } }.awaitAll()
        database.close()

        assertEquals(1, parseCount.get())
        assertTrue(results.all { it.size == 109 })
    }
}
