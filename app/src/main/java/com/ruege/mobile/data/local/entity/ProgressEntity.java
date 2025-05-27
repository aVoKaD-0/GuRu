package com.ruege.mobile.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import org.json.JSONArray;
import org.json.JSONException;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

@Entity(
    tableName = "progress",
    indices = {
        @Index(name = "index_progress_content_id", value = "content_id")
    }
)
public class ProgressEntity {

    private static final String TAG = "ProgressEntity";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "content_id")
    private String contentId = "";

    @ColumnInfo(name = "percentage")
    private int percentage;

    @ColumnInfo(name = "last_accessed")
    private long lastAccessed;

    @ColumnInfo(name = "completed")
    private boolean completed;
    
    @Nullable
    @ColumnInfo(name = "title")
    private String title;
    
    @Nullable
    @ColumnInfo(name = "description")
    private String description;
    
    @ColumnInfo(name = "user_id")
    private long userId;
    
    @Nullable
    @ColumnInfo(name = "solved_task_ids")
    private String solvedTaskIds;

    @Ignore
    private List<String> solvedTaskIdsCache = null;

    public ProgressEntity() {
    }

    @Ignore
    public ProgressEntity(@NonNull String contentId, int percentage, 
                         long lastAccessed, boolean completed) {
        this.contentId = contentId;
        this.percentage = percentage;
        this.lastAccessed = lastAccessed;
        this.completed = completed;
    }
    
    @Ignore
    public ProgressEntity(@NonNull String contentId, int percentage, 
                         long lastAccessed, boolean completed, @Nullable String title) {
        this.contentId = contentId;
        this.percentage = percentage;
        this.lastAccessed = lastAccessed;
        this.completed = completed;
        this.title = title;
    }
    
    @Ignore
    public ProgressEntity(@NonNull String contentId, int percentage, 
                         long lastAccessed, boolean completed, 
                         @Nullable String title, long userId) {
        this.contentId = contentId;
        this.percentage = percentage;
        this.lastAccessed = lastAccessed;
        this.completed = completed;
        this.title = title;
        this.userId = userId;
    }
    
    @Ignore
    public ProgressEntity(@NonNull String contentId, int percentage, 
                         long lastAccessed, boolean completed, 
                         @Nullable String title, @Nullable String description, 
                         long userId, @Nullable String solvedTaskIds) {
        this.contentId = contentId;
        this.percentage = percentage;
        this.lastAccessed = lastAccessed;
        this.completed = completed;
        this.title = title;
        this.description = description;
        this.userId = userId;
        this.solvedTaskIds = solvedTaskIds;
    }

    @NonNull
    public String getContentId() {
        return contentId;
    }

    public int getPercentage() {
        return percentage;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public boolean isCompleted() {
        return completed;
    }
    
    @Nullable
    public String getTitle() {
        return title;
    }
    
    @Nullable
    public String getDescription() {
        return description;
    }
    
    public long getUserId() {
        return userId;
    }
    
    @Nullable
    public String getSolvedTaskIds() {
        return solvedTaskIds;
    }

    public void setContentId(@NonNull String contentId) {
        this.contentId = contentId;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public void setTitle(@Nullable String title) {
        this.title = title;
    }
    
    public void setDescription(@Nullable String description) {
        this.description = description;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    public void setSolvedTaskIds(@Nullable String solvedTaskIds) {
        this.solvedTaskIds = solvedTaskIds;
        this.solvedTaskIdsCache = null;
    }

    /**
     * Получает номер задания ЕГЭ из contentId или title
     */
    public int getEgeNumber() {
        try {
            if (contentId != null && contentId.startsWith("task_group_")) {
                String numberPart = contentId.replace("task_group_", "");
                return Integer.parseInt(numberPart);
            }
            
            if (title != null && title.startsWith("Задание ")) {
                String numberPart = title.replace("Задание ", "");
                return Integer.parseInt(numberPart);
            }
            
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Возвращает список ID решенных заданий
     */
    public List<String> getSolvedTaskIdsList() {
        if (solvedTaskIdsCache != null) {
            return solvedTaskIdsCache;
        }
        
        List<String> taskIds = new ArrayList<>();
        
        if (solvedTaskIds == null || solvedTaskIds.isEmpty() || solvedTaskIds.equals("[]")) {
            solvedTaskIdsCache = taskIds;
            return taskIds;
        }
        
        try {
            JSONArray jsonArray = new JSONArray(solvedTaskIds);
            for (int i = 0; i < jsonArray.length(); i++) {
                taskIds.add(jsonArray.getString(i));
            }
            solvedTaskIdsCache = taskIds;
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при парсинге списка решенных заданий", e);
        }
        
        return taskIds;
    }
    
    /**
     * Проверяет, решено ли задание с указанным ID
     */
    public boolean isTaskSolved(String taskId) {
        return getSolvedTaskIdsList().contains(taskId);
    }
    
    /**
     * Преобразует список ID заданий в JSON-строку
     */
    public static String listToJsonString(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return "[]";
        }
        
        JSONArray jsonArray = new JSONArray();
        for (String taskId : taskIds) {
            jsonArray.put(taskId);
        }
        return jsonArray.toString();
    }
} 