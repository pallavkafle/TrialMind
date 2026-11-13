package com.example.trailmind.data.local

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// one saved server answer, stored as JSON under a key such as "destinations" or "weather:27.71,85.35"
@Entity(tableName = "cache_entries")
data class CacheEntry(
    @PrimaryKey @ColumnInfo(name = "cache_key") val cacheKey: String,
    val json: String,
    val savedAt: Long
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache_entries WHERE cache_key = :key")
    suspend fun get(key: String): CacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: CacheEntry)

    @Query("DELETE FROM cache_entries WHERE cache_key = :key")
    suspend fun remove(key: String)

    @Query("DELETE FROM cache_entries")
    suspend fun clear()

    @Query("DELETE FROM cache_entries WHERE savedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Database(entities = [CacheEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}

object OfflineCache {
    private var db: AppDatabase? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        if (db != null) return
        val database = Room.databaseBuilder(
            context.applicationContext, AppDatabase::class.java, "trailmind_cache.db"
        ).fallbackToDestructiveMigration().build()
        db = database
        // forget anything older than 30 days
        scope.launch {
            runCatching {
                database.cacheDao().deleteOlderThan(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
            }
        }
    }

    suspend fun get(key: String): CacheEntry? = withContext(Dispatchers.IO) {
        runCatching { db?.cacheDao()?.get(key) }.getOrNull()
    }

    suspend fun put(key: String, json: String) {
        withContext(Dispatchers.IO) {
            runCatching { db?.cacheDao()?.put(CacheEntry(key, json, System.currentTimeMillis())) }
        }
    }

    suspend fun remove(key: String) {
        withContext(Dispatchers.IO) { runCatching { db?.cacheDao()?.remove(key) } }
    }

    // called when the saved login is cleared (logout, expired session)
    fun clearAsync() {
        OfflineState.set(false)
        scope.launch { runCatching { db?.cacheDao()?.clear() } }
    }
}

// true while the last answer shown came from the saved copy instead of the server
object OfflineState {
    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline

    fun set(value: Boolean) {
        _offline.value = value
    }
}