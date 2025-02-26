package com.deniscerri.ytdlnis.database.dao

import androidx.room.*
import com.deniscerri.ytdlnis.database.models.CookieItem
import com.deniscerri.ytdlnis.database.models.LogItem
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY id DESC")
    fun getAllLogs() : List<LogItem>

    @Query("SELECT * FROM logs ORDER BY id DESC")
    fun getAllLogsFlow() : Flow<List<LogItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: LogItem) : Long

    @Query("DELETE FROM logs")
    suspend fun deleteAll()

    @Query("DELETE FROM logs WHERE id=:itemId")
    suspend fun delete(itemId: Long)

    @Transaction
    suspend fun deleteItems(list: List<LogItem>){
        list.forEach{
            delete(it.id)
        }
    }

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: LogItem)

    @Query("SELECT * FROM logs WHERE id=:id LIMIT 1")
    fun getByID(id: Long) : LogItem
}