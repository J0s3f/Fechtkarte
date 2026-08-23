package at.j0s.meyercard.app.adapter.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Stand-ins for [FechtkarteDatabase] at two different literal schema versions, so a test can
 * simulate "the app was updated" (which changes [at.j0s.meyercard.app.BuildConfig.VERSION_CODE],
 * and so the real schema version) without depending on the actual build's version code, which
 * is fixed for the whole test run. Same entity, same table — only the version differs, which is
 * exactly what an app update does to the real database file on disk.
 */
@Database(entities = [HistoricalCardEntity::class], version = 1, exportSchema = false)
internal abstract class DatabaseAtVersionOne : RoomDatabase() {
    abstract fun historicalCardDao(): HistoricalCardDao
}

@Database(entities = [HistoricalCardEntity::class], version = 2, exportSchema = false)
internal abstract class DatabaseAtVersionTwo : RoomDatabase() {
    abstract fun historicalCardDao(): HistoricalCardDao
}

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

    /**
     * Reproduces the bug this file's [DatabaseAtVersionOne]/[DatabaseAtVersionTwo] exist for:
     * an app *update* (not a fresh install) leaves the previous database file, and its row
     * count, in place, so [RoomCardRepository]'s count-then-insert seed check never re-runs —
     * any dataset correction shipped in the update never reaches an already-installed device.
     * [FechtkarteDatabase] fixes this by tying its schema version to
     * [at.j0s.meyercard.app.BuildConfig.VERSION_CODE] with
     * `fallbackToDestructiveMigration(dropAllTables = true)`, so every update forces a reseed
     * regardless of the existing row count — simulated here by opening the same underlying file
     * at two different literal versions, standing in for two different app builds.
     */
    @Test
    fun `opening the database at a higher schema version reseeds from the current bundled asset`() = runBlocking {
        val beforeUpdate = Room.databaseBuilder(context, DatabaseAtVersionOne::class.java, databaseName).build()
        // A row the real bundled asset could never produce (id 999 is outside 1..109) - proof,
        // not just an assumption, that this specific row is gone after the "update".
        beforeUpdate.historicalCardDao().insertAll(
            listOf(HistoricalCardEntity(id = 999L, hand = "RIGHT", instructionName = null, actionsJson = "[]", sourceNote = "stale")),
        )
        beforeUpdate.close()

        val afterUpdate = Room.databaseBuilder(context, DatabaseAtVersionTwo::class.java, databaseName)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        val cards = RoomCardRepository(afterUpdate.historicalCardDao()) { context.readOriginalCardsAsset() }.allCards()
        afterUpdate.close()

        assertEquals(109, cards.size)
        assertFalse("the stale pre-update row survived the schema-version bump", cards.any { it.id.value == 999L })
    }

    /**
     * The previous test proves the *mechanism* (schema-version bump + destructive migration
     * reseeds) works; this one proves [FechtkarteDatabase] actually wires into it, rather than
     * silently reverting to a hand-maintained literal version that would reintroduce this bug.
     * `@Database`'s `version` isn't retained for reflection at runtime, so this checks the
     * artifact Room actually produces instead: SQLite's own `PRAGMA user_version`, which Room
     * writes to on open and is what a *real* migration decision is based on.
     */
    @Test
    fun `FechtkarteDatabase's on-disk schema version is the app's own version code`() {
        val database = FechtkarteDatabase.create(context, databaseName)
        val persistedVersion = database.openHelper.readableDatabase.version
        database.close()

        assertEquals(at.j0s.meyercard.app.BuildConfig.VERSION_CODE, persistedVersion)
    }
}
