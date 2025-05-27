package com.ruege.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ruege.mobile.data.local.entity.UserVariantTaskAnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserVariantTaskAnswerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnswer(answer: UserVariantTaskAnswerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnswers(answers: List<UserVariantTaskAnswerEntity>)

    @Query("SELECT * FROM user_variant_task_answers WHERE variant_task_id = :variantTaskId")
    suspend fun getAnswerByTaskId(variantTaskId: Int): UserVariantTaskAnswerEntity?
    
    @Query("SELECT * FROM user_variant_task_answers WHERE variant_task_id = :variantTaskId")
    fun getAnswerByTaskIdFlow(variantTaskId: Int): Flow<UserVariantTaskAnswerEntity?>

    @Query("SELECT * FROM user_variant_task_answers WHERE variant_id = :variantId")
    fun getAnswersForVariant(variantId: Int): Flow<List<UserVariantTaskAnswerEntity>>
    
    @Query("SELECT * FROM user_variant_task_answers WHERE variant_id = :variantId")
    suspend fun getAnswersForVariantList(variantId: Int): List<UserVariantTaskAnswerEntity>

    @Query("UPDATE user_variant_task_answers SET is_submission_correct = :isCorrect, points_awarded = :points WHERE variant_task_id = :variantTaskId")
    suspend fun updateSubmissionResult(variantTaskId: Int, isCorrect: Boolean, points: Int)

    @Query("DELETE FROM user_variant_task_answers WHERE variant_id = :variantId")
    suspend fun deleteAnswersForVariant(variantId: Int)

    @Query("DELETE FROM user_variant_task_answers")
    suspend fun clearAllAnswers() 

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserAnswer(answer: UserVariantTaskAnswerEntity)

    @Query("SELECT * FROM user_variant_task_answers WHERE variant_task_id = :variantTaskId")
    suspend fun getUserAnswerForTask(variantTaskId: Int): UserVariantTaskAnswerEntity?
} 