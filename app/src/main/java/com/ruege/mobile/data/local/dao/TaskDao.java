package com.ruege.mobile.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Transaction;

import com.ruege.mobile.data.local.entity.TaskEntity;
import com.ruege.mobile.data.local.entity.TaskOptionEntity;
import com.ruege.mobile.data.local.relation.TaskWithOptions;

import java.util.List;

@Dao
public interface TaskDao {

    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TaskEntity task);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TaskEntity> tasks);

    @Update
    void update(TaskEntity task);
    
    @Update
    void updateAll(List<TaskEntity> tasks);
    
    @Transaction
    default void insertOrUpdateAll(List<TaskEntity> tasks) {
        for (TaskEntity task : tasks) {
            TaskEntity existing = getTaskByIdSync(task.getId());
            if (existing != null) {
                update(task);
            } else {
                insert(task);
            }
        }
    }

    @Delete
    void delete(TaskEntity task);

    @Query("DELETE FROM tasks WHERE ege_number = :egeNumber")
    void deleteByEgeNumber(String egeNumber);

    @Query("SELECT * FROM tasks WHERE id = :id")
    kotlinx.coroutines.flow.Flow<TaskEntity> getTaskById(int id);

    @Query("SELECT * FROM tasks WHERE id = :id")
    TaskEntity getTaskByIdSync(int id);

    @Query("SELECT * FROM tasks WHERE ege_number = :egeNumber")
    kotlinx.coroutines.flow.Flow<List<TaskEntity>> getTasksByEgeNumber(String egeNumber);

    @Query("SELECT * FROM tasks WHERE ege_number = :egeNumber")
    List<TaskEntity> getTasksByEgeNumberSync(String egeNumber);

    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOption(TaskOptionEntity option);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllOptions(List<TaskOptionEntity> options);

    @Update
    void updateOption(TaskOptionEntity option);

    @Delete
    void deleteOption(TaskOptionEntity option);

    @Query("SELECT * FROM task_options WHERE option_id = :optionId")
    kotlinx.coroutines.flow.Flow<TaskOptionEntity> getOptionById(long optionId);

    @Query("SELECT * FROM task_options WHERE task_id = :taskId ORDER BY order_position ASC")
    kotlinx.coroutines.flow.Flow<List<TaskOptionEntity>> getOptionsByTaskId(String taskId);

    @Query("SELECT * FROM task_options WHERE task_id = :taskId AND is_correct = 1")
    kotlinx.coroutines.flow.Flow<List<TaskOptionEntity>> getCorrectOptionsByTaskId(String taskId);

    
    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    kotlinx.coroutines.flow.Flow<TaskWithOptions> getTaskWithOptions(int id);

    @Query("SELECT * FROM tasks WHERE id IN (:taskIds)")
    List<TaskEntity> getTasksByIds(List<Integer> taskIds);
} 