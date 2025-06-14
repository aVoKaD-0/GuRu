package com.ruege.mobile.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ruege.mobile.data.local.entity.ShpargalkaEntity

/**
 * DAO для работы с кэшем шпаргалок.
 */
@Dao
interface ShpargalkaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shpargalki: List<ShpargalkaEntity>)

    @Query("SELECT * FROM shpargalki_cache ORDER BY title ASC")
    fun getAllShpargalki(): LiveData<List<ShpargalkaEntity>>
    
    @Query("SELECT * FROM shpargalki_cache")
    suspend fun getAllShpargalkiSync(): List<ShpargalkaEntity>

    @Query("DELETE FROM shpargalki_cache")
    suspend fun clearAll()

    @Query("UPDATE shpargalki_cache SET isDownloaded = :isDownloaded WHERE id = :id")
    suspend fun updateDownloadStatus(id: Int, isDownloaded: Boolean)

    /**
     * Заменяет все данные в таблице на новые.
     * Это гарантирует, что кэш всегда будет синхронизирован с сервером.
     */
    @Transaction
    suspend fun replaceAll(shpargalki: List<ShpargalkaEntity>) {
        clearAll()
        insertAll(shpargalki)
    }
} 