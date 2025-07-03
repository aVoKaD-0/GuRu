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

    @Query("SELECT * FROM practice_attempts ORDER BY attempt_date DESC LIMIT :limit")
    Flow<List<PracticeAttemptEntity>> getRecentAttempts(int limit);

    @Query("SELECT COUNT(*) FROM practice_attempts")
    Flow<Integer> getTotalAttempts();

    @Query("DELETE FROM practice_attempts")
    void deleteAll();

    @androidx.room.Transaction
    default void clearAndInsertAll(List<PracticeAttemptEntity> attempts) {
        deleteAll();
        insertAll(attempts);
    }
} 