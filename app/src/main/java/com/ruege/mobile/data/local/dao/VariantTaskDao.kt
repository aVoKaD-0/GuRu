package com.ruege.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruege.mobile.data.local.entity.VariantTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VariantTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTask(task: VariantTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTasks(tasks: List<VariantTaskEntity>)

    @Query("SELECT * FROM variant_tasks WHERE variant_id = :variantId ORDER BY order_in_variant ASC")
    fun getTasksForVariant(variantId: Int): Flow<List<VariantTaskEntity>>

    @Query("SELECT * FROM variant_tasks WHERE variant_task_id = :variantTaskId")
    suspend fun getTaskById(variantTaskId: Int): VariantTaskEntity?

    @Query("DELETE FROM variant_tasks WHERE variant_id = :variantId")
    suspend fun deleteTasksForVariant(variantId: Int)

    @Query("DELETE FROM variant_tasks WHERE variant_task_id = :variantTaskId")
    suspend fun deleteTaskById(variantTaskId: Int)

    @Query("DELETE FROM variant_tasks")
    suspend fun clearAllTasks()

    @Query("SELECT * FROM variant_tasks WHERE variant_id = :variantId ORDER BY order_in_variant ASC")
    fun getTasksByVariantIdFlow(variantId: Int): Flow<List<VariantTaskEntity>>

    @Query("DELETE FROM variant_tasks WHERE variant_id = :variantId")
    suspend fun deleteTasksByVariantId(variantId: Int)
} 