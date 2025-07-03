package com.ruege.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruege.mobile.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с очередью синхронизации
 */
@Dao
interface SyncQueueDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(syncQueueItem: SyncQueueEntity): Long
    
    @Query("SELECT * FROM sync_queue ORDER BY created_at ASC")
    fun getSyncQueue(): Flow<List<SyncQueueEntity>>
    
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()
} 