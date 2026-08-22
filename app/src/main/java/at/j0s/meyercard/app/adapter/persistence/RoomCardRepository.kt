package at.j0s.meyercard.app.adapter.persistence

import at.j0s.meyercard.app.application.port.spi.CardRepository
import at.j0s.meyercard.app.domain.MeyerCard
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Seeds [dao] from the bundled historical dataset on first access, then
 * serves everything from Room. [originalCardsJson] supplies the dataset's
 * raw text as a lambda rather than a `Context` — this keeps the class
 * constructible (and testable) without Android's asset-loading machinery;
 * the real app wires it to [readOriginalCardsAsset].
 *
 * The count-then-insert seed check is guarded by [seedMutex] — a *companion object*
 * `Mutex`, deliberately shared across every instance in the process rather than one held
 * per instance. A per-instance lock wouldn't cover the actual race (T8.4): `MainActivity`
 * builds a fresh `RoomCardRepository` in `onCreate`, which reruns on a config change like a
 * device rotation, so two separate instances — each with their own lock if it were
 * per-instance — can end up pointed at the very same underlying database file during the
 * narrow first-launch seeding window. `OnConflictStrategy.IGNORE` on `insertAll` already
 * keeps that race from corrupting data or crashing; this mutex closes it entirely rather
 * than leaning on that as an accidental safety net, and avoids redundant JSON parsing and
 * insert attempts. See `RoomCardRepositoryTest`'s concurrent-instances test.
 */
class RoomCardRepository(
    private val dao: HistoricalCardDao,
    private val originalCardsJson: () -> String,
) : CardRepository {

    override suspend fun allCards(): List<MeyerCard> {
        seedMutex.withLock {
            if (dao.count() == 0) {
                val cards = OriginalCardsDataSource.parse(originalCardsJson())
                dao.insertAll(cards.map { it.toEntity() })
            }
        }
        return dao.getAll().map { it.toDomain() }
    }

    private companion object {
        val seedMutex = Mutex()
    }
}
