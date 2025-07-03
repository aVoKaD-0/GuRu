package com.ruege.mobile.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.room.Ignore;
import com.ruege.mobile.data.local.converter.SyncStatusConverter;

/**
 * Сущность очереди синхронизации прогресса и статистики.
 * Используется для хранения изменений, которые необходимо синхронизировать с сервером.
 */
@Entity(tableName = "progress_and_static_sync_queue")
@TypeConverters(SyncStatusConverter.class)
public class ProgressSyncQueueEntity {

    public static final String ITEM_TYPE_PROGRESS = "progress";
    public static final String ITEM_TYPE_STATISTICS = "statistics";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @NonNull
    @ColumnInfo(name = "item_id")
    private String itemId;

    @NonNull
    @ColumnInfo(name = "item_type")
    private String itemType;

    @ColumnInfo(name = "percentage")
    private int percentage;

    @ColumnInfo(name = "completed")
    private boolean completed;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "user_id")
    private long userId;

    @ColumnInfo(name = "sync_status")
    private SyncStatus syncStatus;

    @ColumnInfo(name = "retry_count")
    private int retryCount;

    @ColumnInfo(name = "last_sync_attempt")
    private long lastSyncAttempt;

    @ColumnInfo(name = "error_message")
    private String errorMessage;

    @ColumnInfo(name = "solved_task_ids")
    private String solvedTaskIds;

    /**
     * Конструктор по умолчанию, требуется для Room
     */
    public ProgressSyncQueueEntity() {
    }

    /**
     * Расширенный конструктор с поддержкой списка решенных заданий
     */
    @Ignore
    public ProgressSyncQueueEntity(@NonNull String itemId, @NonNull String itemType, int percentage, boolean completed,
                                  long timestamp, long userId, SyncStatus syncStatus, String solvedTaskIds) {
        this.itemId = itemId;
        this.itemType = itemType;
        this.percentage = percentage;
        this.completed = completed;
        this.timestamp = timestamp;
        this.userId = userId;
        this.syncStatus = syncStatus;
        this.retryCount = 0;
        this.lastSyncAttempt = 0;
        this.errorMessage = "";
        this.solvedTaskIds = solvedTaskIds;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getItemId() {
        return itemId;
    }

    public void setItemId(@NonNull String itemId) {
        this.itemId = itemId;
    }

    @NonNull
    public String getItemType() {
        return itemType;
    }

    public void setItemType(@NonNull String itemType) {
        this.itemType = itemType;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public long getLastSyncAttempt() {
        return lastSyncAttempt;
    }

    public void setLastSyncAttempt(long lastSyncAttempt) {
        this.lastSyncAttempt = lastSyncAttempt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSolvedTaskIds() {
        return solvedTaskIds;
    }

    public void setSolvedTaskIds(String solvedTaskIds) {
        this.solvedTaskIds = solvedTaskIds;
    }
} 