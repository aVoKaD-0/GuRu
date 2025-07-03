package com.ruege.mobile.data.local.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.ruege.mobile.data.local.entity.TaskOptionEntity;

@Dao
public interface TaskOptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(@NonNull TaskOptionEntity option);

    @Query("DELETE FROM task_options")
    void deleteAll();
}
