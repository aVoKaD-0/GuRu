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

    @Query("DELETE FROM user_task_attempts")
    void deleteAll();
} 