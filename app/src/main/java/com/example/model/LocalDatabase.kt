package com.example.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "calculation_history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntry)

    @Query("DELETE FROM calculation_history")
    suspend fun clearHistory()

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteEntry(id: Long)
}

@Database(entities = [HistoryEntry::class], version = 1, exportSchema = false)
abstract class CalculatorDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CalculatorDatabase? = null

        fun getDatabase(context: Context): CalculatorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CalculatorDatabase::class.java,
                    "calculator_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class CalculatorRepository(private val historyDao: HistoryDao) {
    val history: Flow<List<HistoryEntry>> = historyDao.getHistory()

    suspend fun insert(entry: HistoryEntry) {
        historyDao.insert(entry)
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    suspend fun deleteEntry(id: Long) {
        historyDao.deleteEntry(id)
    }
}
