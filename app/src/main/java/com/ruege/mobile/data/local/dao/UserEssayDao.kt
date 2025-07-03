package com.ruege.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruege.mobile.data.local.entity.UserEssayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserEssayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(userEssay: UserEssayEntity)

    @Query("SELECT * FROM user_essays WHERE task_id = :taskId")
    fun getByTaskId(taskId: String): Flow<UserEssayEntity?>

    @Query("SELECT * FROM user_essays WHERE task_id = :taskId")
    suspend fun getByTaskIdSync(taskId: String): UserEssayEntity?

    @Query("DELETE FROM user_essays WHERE task_id = :taskId")
    suspend fun deleteByTaskId(taskId: String)
} 