package com.ruege.mobile.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.ruege.mobile.data.local.entity.ProgressEntity;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProgressEntity progress);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProgressEntity> progressList);

    @Update
    void update(ProgressEntity progress);

    @Update
    void updateAll(List<ProgressEntity> progressList);

    @Delete
    void delete(ProgressEntity progress);

    @Query("SELECT * FROM progress WHERE content_id = :contentId")
    LiveData<ProgressEntity> getProgressByContentId(String contentId);
    
    @Query("SELECT * FROM progress WHERE content_id = :contentId")
    ProgressEntity getProgressByContentIdSync(String contentId);

    @Query("SELECT * FROM progress WHERE content_id IN (:contentIds)")
    List<ProgressEntity> getProgressByContentIdsSync(List<String> contentIds);

    @Query("SELECT * FROM progress")
    List<ProgressEntity> getAllProgressListSync();

    @Query("SELECT * FROM progress WHERE user_id = :userId")
    Flow<List<ProgressEntity>> getProgressByUserId(long userId);

    @Query("DELETE FROM progress WHERE user_id = :userId")
    void deleteByUserId(long userId);

    @Query("DELETE FROM progress")
    void deleteAll();

    @Query("UPDATE progress SET percentage = :percentage, last_accessed = :timestamp WHERE content_id = :contentId")
    void updateProgress(String contentId, int percentage, long timestamp);

    @Query("UPDATE progress SET completed = 1, percentage = 100, last_accessed = :timestamp WHERE content_id = :contentId")
    void markAsCompleted(String contentId, long timestamp);
} 