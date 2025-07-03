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
    suspend fun deleteAll()

    @Query("UPDATE shpargalki_cache SET isDownloaded = :isDownloaded WHERE id = :id")
    suspend fun updateDownloadStatus(id: Int, isDownloaded: Boolean)

    @Transaction
    suspend fun replaceAll(shpargalki: List<ShpargalkaEntity>) {
        deleteAll()
        insertAll(shpargalki)
    }
} 