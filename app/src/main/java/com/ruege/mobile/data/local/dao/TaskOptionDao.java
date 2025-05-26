package com.ruege.mobile.data.local.dao;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.ruege.mobile.data.local.entity.TaskOptionEntity;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface TaskOptionDao {
    /**
     * Вставляет вариант ответа в базу данных
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(@NonNull TaskOptionEntity option);
    
    /**
     * Получает все варианты ответов для задания по его ID
     */
    @Query("SELECT * FROM task_options WHERE task_id = :taskId")
    @NonNull
    Flow<List<TaskOptionEntity>> getOptionsByTaskId(@NonNull String taskId);
}
