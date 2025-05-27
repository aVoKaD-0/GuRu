package com.ruege.mobile.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.room.Ignore;
import com.ruege.mobile.data.local.converter.SyncStatusConverter;

/**
 * Сущность очереди синхронизации прогресса пользователя.
 * Используется для хранения изменений, которые необходимо синхронизировать с сервером.
 */
@Entity(tableName = "progress_sync_queue")
@TypeConverters(SyncStatusConverter.class)
public class ProgressSyncQueueEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @NonNull
    @ColumnInfo(name = "content_id")
    private String contentId;

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
     * Основной конструктор
     */
    @Ignore
    public ProgressSyncQueueEntity(@NonNull String contentId, int percentage, boolean completed, 
                                  long timestamp, long userId, SyncStatus syncStatus) {
        this.contentId = contentId;
        this.percentage = percentage;
        this.completed = completed;
        this.timestamp = timestamp;
        this.userId = userId;
        this.syncStatus = syncStatus;
        this.retryCount = 0;
        this.lastSyncAttempt = 0;
        this.errorMessage = "";
        this.solvedTaskIds = null;
    }

    /**
     * Расширенный конструктор с поддержкой списка решенных заданий
     */
    @Ignore
    public ProgressSyncQueueEntity(@NonNull String contentId, int percentage, boolean completed, 
                                  long timestamp, long userId, SyncStatus syncStatus, String solvedTaskIds) {
        this.contentId = contentId;
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

    @NonNull
    public String getContentId() {
        return contentId;
    }

    public int getPercentage() {
        return percentage;
    }

    public boolean isCompleted() {
        return completed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getUserId() {
        return userId;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public long getLastSyncAttempt() {
        return lastSyncAttempt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getSolvedTaskIds() {
        return solvedTaskIds;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setContentId(@NonNull String contentId) {
        this.contentId = contentId;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void setLastSyncAttempt(long lastSyncAttempt) {
        this.lastSyncAttempt = lastSyncAttempt;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setSolvedTaskIds(String solvedTaskIds) {
        this.solvedTaskIds = solvedTaskIds;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
} 