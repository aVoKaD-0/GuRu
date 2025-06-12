package com.ruege.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruege.mobile.data.local.entity.TaskTextEntity

@Dao
interface TaskTextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(taskTexts: List<TaskTextEntity>)

    @Query("SELECT * FROM task_texts WHERE textId = :textId")
    suspend fun getTaskTextById(textId: String): TaskTextEntity?
    
    @Query("DELETE FROM task_texts WHERE textId IN (:textIds)")
    suspend fun deleteByIds(textIds: List<String>)
} 