package com.ruege.mobile.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.ruege.mobile.data.local.entity.ContentEntity;

import java.util.List;

@Dao
public interface ContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ContentEntity content);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ContentEntity> contents);

    @Update
    void update(ContentEntity content);

    @Query("SELECT * FROM contents WHERE type = :type ORDER BY order_position ASC")
    LiveData<List<ContentEntity>> getContentsByType(String type);

    @Query("UPDATE contents SET is_downloaded = :isDownloaded WHERE content_id = :contentId")
    void updateDownloadStatus(String contentId, boolean isDownloaded);

    @Query("UPDATE contents SET is_downloaded = :isDownloaded WHERE type = :type")
    void updateAllDownloadStatusByType(String type, boolean isDownloaded);

    @Query("SELECT * FROM contents WHERE content_id = :contentId LIMIT 1")
    ContentEntity getContentByIdSync(String contentId);

    @Query("DELETE FROM contents")
    void deleteAll();
}