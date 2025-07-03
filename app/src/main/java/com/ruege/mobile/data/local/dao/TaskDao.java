package com.ruege.mobile.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Transaction;

import com.ruege.mobile.data.local.entity.TaskEntity;

import java.util.List;

import kotlinx.coroutines.flow.Flow;

@Dao
public interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TaskEntity task);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TaskEntity> tasks);

    @Update
    void update(TaskEntity task);
    
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
    void deleteTasksByEgeNumber(String egeNumber);

    @Transaction
    default void deleteTasksByEgeNumberAndInsert(String egeNumber, List<TaskEntity> tasks) {
        deleteTasksByEgeNumber(egeNumber);
        insertAll(tasks);
    }

    @Query("SELECT * FROM tasks WHERE id = :id")
    TaskEntity getTaskByIdSync(int id);

    @Query("SELECT * FROM tasks WHERE ege_number = :egeNumber")
    List<TaskEntity> getTasksByEgeNumberSync(String egeNumber);

    @Query("SELECT COUNT(DISTINCT id) FROM tasks WHERE ege_number = :egeNumber")
    int getTaskCountByEgeNumberSync(String egeNumber);

    @Query("SELECT * FROM tasks WHERE id IN (:taskIds)")
    List<TaskEntity> getTasksByIds(List<Integer> taskIds);

    @Query("SELECT DISTINCT ege_number FROM tasks WHERE ege_number IS NOT NULL")
    Flow<List<String>> getDownloadedEgeNumbersStream();

    @Query("DELETE FROM tasks")
    void deleteAll();

    @Query("SELECT * FROM tasks WHERE ege_number IN (:egeNumbers)")
    List<TaskEntity> getTasksByEgeNumbers(List<String> egeNumbers);
} 