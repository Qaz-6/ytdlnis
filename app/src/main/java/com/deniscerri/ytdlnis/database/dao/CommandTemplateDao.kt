package com.deniscerri.ytdlnis.database.dao

import androidx.room.*
import com.deniscerri.ytdlnis.database.models.CommandTemplate
import com.deniscerri.ytdlnis.database.models.TemplateShortcut
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandTemplateDao {
    @Query("SELECT * FROM commandTemplates ORDER BY id DESC")
    fun getAllTemplates() : List<CommandTemplate>

    @Query("SELECT * FROM commandTemplates ORDER BY id DESC")
    fun getAllTemplatesFlow() : Flow<List<CommandTemplate>>
    @Query("SELECT content FROM commandTemplates WHERE useAsExtraCommand is 1")
    fun getAllTemplatesAsExtraCommands() : List<String>

    @Query("SELECT * FROM templateShortcuts ORDER BY id DESC")
    fun getAllShortcutsFlow() : Flow<List<TemplateShortcut>>

    @Query("SELECT * FROM templateShortcuts ORDER BY id DESC")
    fun getAllShortcuts() : List<TemplateShortcut>

    @Query("SELECT COUNT(id) FROM commandTemplates")
    fun getTotalNumber() : Int

    @Query("SELECT COUNT(id) FROM templateShortcuts")
    fun getTotalShortcutNumber() : Int

    @Query("SELECT * FROM commandTemplates WHERE id=:id LIMIT 1")
    fun getTemplate(id: Long) : CommandTemplate

    @Query("SELECT * FROM commandTemplates ORDER BY id DESC LIMIT 1")
    fun getFirst() : CommandTemplate

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: CommandTemplate)

    @Query("DELETE FROM commandTemplates")
    suspend fun deleteAll()

    @Query("DELETE FROM commandTemplates WHERE id=:itemId")
    suspend fun delete(itemId: Long)

    @Insert(TemplateShortcut::class, OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: TemplateShortcut)

    @Query("SELECT COUNT(id) FROM templateShortcuts WHERE content=:content")
    suspend fun checkExistingShortcut(content: String) : Int

    @Query("DELETE FROM templateShortcuts WHERE id=:itemId")
    suspend fun deleteShortcut(itemId: Long)

    @Update
    suspend fun update(item: CommandTemplate)
}