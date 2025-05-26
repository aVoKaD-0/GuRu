package com.ruege.mobile.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Transaction;

import com.ruege.mobile.data.local.entity.ContentEntity;
import com.ruege.mobile.data.local.relation.ContentWithTasks;

import java.util.List;

@Dao
public interface ContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ContentEntity content);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ContentEntity> contents);

    @Update
    void update(ContentEntity content);

    @Update
    void updateAll(List<ContentEntity> contents);

    @Delete
    void delete(ContentEntity content);

    @Query("SELECT * FROM contents WHERE content_id = :contentId")
    LiveData<ContentEntity> getContentById(String contentId);

    @Query("SELECT * FROM contents WHERE parent_id = :parentId ORDER BY order_position ASC")
    LiveData<List<ContentEntity>> getContentsByParentId(String parentId);

    @Query("SELECT * FROM contents WHERE type = :type ORDER BY order_position ASC")
    LiveData<List<ContentEntity>> getContentsByType(String type);

    @Query("SELECT * FROM contents WHERE is_downloaded = 1 ORDER BY order_position ASC")
    LiveData<List<ContentEntity>> getDownloadedContents();

    @Query("SELECT * FROM contents WHERE is_new = 1 ORDER BY order_position ASC")
    LiveData<List<ContentEntity>> getNewContents();

    @Query("UPDATE contents SET is_downloaded = :isDownloaded WHERE content_id = :contentId")
    void updateDownloadStatus(String contentId, boolean isDownloaded);

    @Query("UPDATE contents SET is_new = :isNew WHERE content_id = :contentId")
    void updateNewStatus(String contentId, boolean isNew);

    @Query("UPDATE contents SET description = :description WHERE content_id = :contentId")
    void updateDescription(String contentId, String description);

    @Query("UPDATE contents SET is_downloaded = :isDownloaded, description = :description WHERE content_id = :contentId")
    void updateDownloadStatusAndDescription(String contentId, boolean isDownloaded, String description);

    @Transaction
    @Query("SELECT * FROM contents WHERE content_id = :contentId")
    LiveData<ContentWithTasks> getContentWithTasks(String contentId);

    @Transaction
    @Query("SELECT * FROM contents WHERE type = 'task' ORDER BY order_position ASC")
    LiveData<List<ContentWithTasks>> getAllTaskContentsWithTasks();

    @Query("SELECT * FROM contents ORDER BY content_id ASC")
    List<ContentEntity> getAllContents();

    @Query("SELECT * FROM contents ORDER BY order_position ASC")
    LiveData<List<ContentEntity>> getAllContentsLiveData();

    
    @Query("SELECT * FROM contents WHERE content_id = :contentId LIMIT 1")
    ContentEntity getContentByIdSync(String contentId);
    
    @Query("SELECT content_id FROM contents")
    List<String> getAllContentIds();
    
    @Query("SELECT content_id FROM contents WHERE type LIKE 'task%' OR type LIKE 'theory%'")
    List<String> getAllProgressContentIds();
    
    @Query("SELECT * FROM contents WHERE type = :type ORDER BY order_position ASC")
    List<ContentEntity> getContentsByTypeSync(String type);
    
    @Query("SELECT content_id FROM contents WHERE type = :type")
    List<String> getContentIdsByType(String type);
} 