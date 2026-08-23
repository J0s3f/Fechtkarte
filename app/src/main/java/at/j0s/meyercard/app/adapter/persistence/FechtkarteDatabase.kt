package at.j0s.meyercard.app.adapter.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import at.j0s.meyercard.app.BuildConfig

/**
 * [HistoricalCardEntity] is a pure cache of `original_cards.json` — never user-authored, always
 * fully reproducible by re-parsing the bundled asset (see [RoomCardRepository]). Keying the
 * schema version to [BuildConfig.VERSION_CODE] rather than a hand-maintained integer means every
 * app update forces Room to drop and recreate the table, so a corrected dataset shipped in a new
 * version always reaches devices that already had the app installed — a fixed `version = 1`
 * left every historical-card correction silently inert for exactly that case (an app *update*,
 * as opposed to a fresh install) until this was found, since [RoomCardRepository] only ever
 * seeds an empty table and an update leaves the existing database file, and its row count, in
 * place. No manual "remember to bump this too" step to forget on some future data fix.
 */
@Database(entities = [HistoricalCardEntity::class], version = BuildConfig.VERSION_CODE, exportSchema = false)
abstract class FechtkarteDatabase : RoomDatabase() {
    abstract fun historicalCardDao(): HistoricalCardDao

    companion object {
        private const val DEFAULT_NAME = "fechtkarte.db"

        fun create(context: Context, name: String = DEFAULT_NAME): FechtkarteDatabase =
            Room.databaseBuilder(context.applicationContext, FechtkarteDatabase::class.java, name)
                // Safe specifically because the only table here is a disposable cache (see the
                // class doc) - there is no user data in this database to lose.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}

internal fun Context.readOriginalCardsAsset(): String =
    assets.open("original_cards.json").bufferedReader().use { it.readText() }
