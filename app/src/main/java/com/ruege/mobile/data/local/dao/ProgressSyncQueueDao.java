package com.ruege.mobile.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.ruege.mobile.data.local.entity.ProgressSyncQueueEntity;

import java.util.List;

@Dao
public interface ProgressSyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ProgressSyncQueueEntity item);

    @Update
    void update(ProgressSyncQueueEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<ProgressSyncQueueEntity> items);

    @Query("DELETE FROM progress_and_static_sync_queue")
    void deleteAll();

    @Query("SELECT * FROM progress_and_static_sync_queue WHERE sync_status = :status ORDER BY timestamp ASC LIMIT :limit")
    List<ProgressSyncQueueEntity> getItemsByStatusSync(String status, int limit);

    @Query("SELECT * FROM progress_and_static_sync_queue WHERE sync_status IN (:statuses) ORDER BY timestamp ASC LIMIT :limit")
    List<ProgressSyncQueueEntity> getItemsByStatusesSync(List<String> statuses, int limit);

    @Query("SELECT COUNT(*) FROM progress_and_static_sync_queue WHERE sync_status = :status")
    LiveData<Integer> getCountByStatus(String status);

    @Query("SELECT COUNT(*) FROM progress_and_static_sync_queue WHERE sync_status = :status")
    int getCountByStatusSync(String status);

    @Query("SELECT * FROM progress_and_static_sync_queue WHERE item_id = :itemId")
    ProgressSyncQueueEntity getItemByItemId(String itemId);

    @Query("DELETE FROM progress_and_static_sync_queue WHERE sync_status = :status")
    void deleteByStatus(String status);

    @Query("DELETE FROM progress_and_static_sync_queue WHERE user_id = :userId")
    void deleteByUserId(long userId);

    @Query("SELECT * FROM progress_and_static_sync_queue ORDER BY timestamp DESC")
    LiveData<List<ProgressSyncQueueEntity>> getAllItems();

    @Query("SELECT * FROM progress_and_static_sync_queue ORDER BY timestamp DESC")
    List<ProgressSyncQueueEntity> getAllItemsSync();
} 