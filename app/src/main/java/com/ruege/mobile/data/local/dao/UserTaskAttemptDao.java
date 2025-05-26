package com.ruege.mobile.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.ruege.mobile.data.local.entity.UserTaskAttemptEntity;

import java.util.List;

@Dao
public interface UserTaskAttemptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserTaskAttemptEntity attempt);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UserTaskAttemptEntity> attempts);

    @Update
    void update(UserTaskAttemptEntity attempt);

    @Delete
    void delete(UserTaskAttemptEntity attempt);

    @Query("SELECT * FROM user_task_attempts WHERE attempt_id = :attemptId")
    kotlinx.coroutines.flow.Flow<UserTaskAttemptEntity> getAttemptById(long attemptId);

    @Query("SELECT * FROM user_task_attempts WHERE user_id = :userId")
    kotlinx.coroutines.flow.Flow<List<UserTaskAttemptEntity>> getAttemptsByUserId(long userId);

    @Query("SELECT * FROM user_task_attempts WHERE task_id = :taskId")
    kotlinx.coroutines.flow.Flow<List<UserTaskAttemptEntity>> getAttemptsByTaskId(String taskId);

    @Query("SELECT * FROM user_task_attempts WHERE user_id = :userId AND task_id = :taskId ORDER BY attempt_number DESC")
    kotlinx.coroutines.flow.Flow<List<UserTaskAttemptEntity>> getUserAttemptsByTaskId(long userId, String taskId);

    @Query("SELECT * FROM user_task_attempts WHERE user_id = :userId AND task_id = :taskId AND attempt_number = :attemptNumber")
    kotlinx.coroutines.flow.Flow<UserTaskAttemptEntity> getSpecificAttempt(long userId, String taskId, int attemptNumber);

    @Query("SELECT COUNT(*) FROM user_task_attempts WHERE user_id = :userId AND task_id = :taskId")
    kotlinx.coroutines.flow.Flow<Integer> getAttemptCount(long userId, String taskId);

    @Query("SELECT MAX(points_earned) FROM user_task_attempts WHERE user_id = :userId AND task_id = :taskId")
    kotlinx.coroutines.flow.Flow<Integer> getMaxPointsForTask(long userId, String taskId);

    @Query("SELECT * FROM user_task_attempts WHERE user_id = :userId AND is_correct = 1")
    kotlinx.coroutines.flow.Flow<List<UserTaskAttemptEntity>> getSuccessfulAttempts(long userId);

    @Query("SELECT AVG(points_earned) FROM user_task_attempts WHERE user_id = :userId")
    kotlinx.coroutines.flow.Flow<Float> getAveragePointsForUser(long userId);

    @Query("SELECT SUM(points_earned) FROM user_task_attempts WHERE user_id = :userId")
    kotlinx.coroutines.flow.Flow<Integer> getTotalPointsForUser(long userId);
} 