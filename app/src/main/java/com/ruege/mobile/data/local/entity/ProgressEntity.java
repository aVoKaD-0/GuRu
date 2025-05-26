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
    
    // Кэш списка ID задач для избежания повторного парсинга JSON
    @Ignore
    private List<String> solvedTaskIdsCache = null;

    // Конструкторы
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

    // Геттеры
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

    // Сеттеры
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
        // Сбрасываем кэш, так как данные обновились
        this.solvedTaskIdsCache = null;
    }

    /**
     * Получает номер задания ЕГЭ из contentId или title
     * @return номер задания ЕГЭ или -1, если не удалось определить
     */
    public int getEgeNumber() {
        try {
            // Пытаемся извлечь номер из contentId, если он в формате task_group_X
            if (contentId != null && contentId.startsWith("task_group_")) {
                String numberPart = contentId.replace("task_group_", "");
                return Integer.parseInt(numberPart);
            }
            
            // Если не получилось из contentId, пробуем извлечь из title
            if (title != null && title.startsWith("Задание ")) {
                String numberPart = title.replace("Задание ", "");
                return Integer.parseInt(numberPart);
            }
            
            // Если не удалось определить номер, возвращаем -1
            return -1;
        } catch (NumberFormatException e) {
            // В случае ошибки возвращаем -1
            return -1;
        }
    }
    
    /**
     * Возвращает список ID решенных заданий
     * @return список ID решенных заданий или пустой список
     */
    public List<String> getSolvedTaskIdsList() {
        // Если кэш уже заполнен, возвращаем его
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
            // Сохраняем результат в кэш
            solvedTaskIdsCache = taskIds;
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка при парсинге списка решенных заданий", e);
        }
        
        return taskIds;
    }
    
    /**
     * Проверяет, решено ли задание с указанным ID
     * @param taskId ID задания
     * @return true, если задание решено
     */
    public boolean isTaskSolved(String taskId) {
        return getSolvedTaskIdsList().contains(taskId);
    }
    
    /**
     * Преобразует список ID заданий в JSON-строку
     * @param taskIds список ID заданий
     * @return JSON-строка
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