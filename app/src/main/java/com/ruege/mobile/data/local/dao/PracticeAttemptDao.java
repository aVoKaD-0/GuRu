package com.ruege.mobile.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.ruege.mobile.data.local.entity.PracticeAttemptEntity;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface PracticeAttemptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PracticeAttemptEntity attempt);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PracticeAttemptEntity> attempts);

    @Update
    void update(PracticeAttemptEntity attempt);

    @Delete
    void delete(PracticeAttemptEntity attempt);

    @Query("SELECT * FROM practice_attempts WHERE attempt_id = :attemptId")
    Flow<PracticeAttemptEntity> getAttemptById(long attemptId);

    @Query("SELECT * FROM practice_attempts WHERE task_id = :taskId ORDER BY attempt_date DESC")
    Flow<List<PracticeAttemptEntity>> getAttemptsByTaskId(Integer taskId);

    @Query("SELECT COUNT(*) FROM practice_attempts WHERE task_id = :taskId")
    Flow<Integer> getAttemptCountForTask(Integer taskId);

    @Query("SELECT COUNT(*) FROM practice_attempts WHERE task_id = :taskId AND is_correct = 1")
    Flow<Integer> getCorrectAttemptCountForTask(Integer taskId);

    @Query("SELECT * FROM practice_attempts WHERE task_id IN " +
           "(SELECT id FROM tasks WHERE ege_number = :egeNumber) " +
           "ORDER BY attempt_date DESC")
    Flow<List<PracticeAttemptEntity>> getAttemptsByEgeNumber(String egeNumber);

    @Query("SELECT * FROM practice_attempts ORDER BY attempt_date DESC LIMIT :limit")
    Flow<List<PracticeAttemptEntity>> getRecentAttempts(int limit);

    @Query("SELECT COUNT(*) FROM practice_attempts WHERE is_correct = 1")
    Flow<Integer> getTotalCorrectAttempts();

    @Query("SELECT COUNT(*) FROM practice_attempts")
    Flow<Integer> getTotalAttempts();

    @Query("DELETE FROM practice_attempts")
    void clearAllAttempts();

    @Query("SELECT EXISTS(SELECT 1 FROM practice_attempts WHERE task_id = :taskId LIMIT 1)")
    boolean wasAttemptsForTask(int taskId);
} 