package com.ruege.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruege.mobile.data.local.entity.VariantTaskOptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VariantTaskOptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateOptions(options: List<VariantTaskOptionEntity>)

    @Query("SELECT * FROM variant_task_options WHERE variant_task_id = :variantTaskId")
    suspend fun getOptionsForTask(variantTaskId: Int): List<VariantTaskOptionEntity>

    @Query("DELETE FROM variant_task_options WHERE variant_task_id IN (SELECT id FROM variant_tasks WHERE variant_id = :variantId)")
    suspend fun deleteOptionsByVariantId(variantId: Int)

    @Query("DELETE FROM variant_task_options WHERE variant_task_id = :variantTaskId")
    suspend fun deleteOptionsByTaskId(variantTaskId: Int)

    @Query("SELECT * FROM variant_task_options WHERE variant_task_id = :variantTaskId ORDER BY id ASC")
    fun getOptionsByTaskIdFlow(variantTaskId: Int): Flow<List<VariantTaskOptionEntity>>

    // Возможно, понадобится метод для удаления опций по списку ID задач, если задания удаляются не все сразу
} 