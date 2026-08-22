package at.j0s.meyercard.app.adapter.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * No `@Transaction`-wrapped "seed if empty" default method here — tried that
 * (a default method calling `count()`/`insertAll()` on `this`), and Room's
 * KSP processor failed with `IllegalStateException: unexpected jvm signature
 * V` compiling it, a real KSP/Kotlin-default-method interop failure rather
 * than a mistake in the DAO itself. [RoomCardRepository] does the
 * count-then-insert check instead, guarded by its own process-wide `Mutex`
 * (T8.4) — a device rotation during the narrow first-launch seeding window
 * genuinely can produce two concurrent callers (see that class's doc
 * comment), not just a theoretical one.
 */
@Dao
interface HistoricalCardDao {
    @Query("SELECT * FROM historical_cards ORDER BY id")
    suspend fun getAll(): List<HistoricalCardEntity>

    @Query("SELECT COUNT(*) FROM historical_cards")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<HistoricalCardEntity>)
}
