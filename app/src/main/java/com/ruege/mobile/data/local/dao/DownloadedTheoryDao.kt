package com.ruege.mobile.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruege.mobile.data.local.entity.DownloadedTheoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedTheoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(theory: DownloadedTheoryEntity)

    @Query("SELECT * FROM downloaded_theory WHERE content_id = :id")
    fun getDownloadedTheoryById(id: String): LiveData<DownloadedTheoryEntity>

    @Query("SELECT content_id FROM downloaded_theory")
    suspend fun getAllIds(): List<String>

    @Query("SELECT content_id FROM downloaded_theory")
    fun getAllIdsAsFlow(): Flow<List<String>>

    @Query("DELETE FROM downloaded_theory WHERE content_id = :id")
    suspend fun deleteById(id: String)
} 