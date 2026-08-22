package at.j0s.meyercard.app.adapter.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HistoricalCardEntity::class], version = 1, exportSchema = false)
abstract class FechtkarteDatabase : RoomDatabase() {
    abstract fun historicalCardDao(): HistoricalCardDao

    companion object {
        private const val DEFAULT_NAME = "fechtkarte.db"

        fun create(context: Context, name: String = DEFAULT_NAME): FechtkarteDatabase =
            Room.databaseBuilder(context.applicationContext, FechtkarteDatabase::class.java, name).build()
    }
}

internal fun Context.readOriginalCardsAsset(): String =
    assets.open("original_cards.json").bufferedReader().use { it.readText() }
