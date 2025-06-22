package com.ruege.mobile.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "tasks")
public class TaskEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private Integer id;

    @Nullable
    @ColumnInfo(name = "fipi_id")
    private String fipiId;

    @NonNull
    @ColumnInfo(name = "ege_number")
    private String egeNumber;

    @Nullable
    @ColumnInfo(name = "task_text")
    private String taskText;

    @Nullable
    @ColumnInfo(name = "solution")
    private String solution;

    @Nullable
    @ColumnInfo(name = "explanation")
    private String explanation;

    @Nullable
    @ColumnInfo(name = "source")
    private String source;

    @Nullable
    @ColumnInfo(name = "text_id")
    private Integer textId;

    @NonNull
    @ColumnInfo(name = "task_type")
    private String taskType;

    public TaskEntity(@NonNull Integer id, @Nullable String fipiId, @NonNull String egeNumber,
                      @Nullable String taskText, @Nullable String solution, @Nullable String explanation,
                      @Nullable String source, @Nullable Integer textId, @NonNull String taskType) {
        this.id = id;
        this.fipiId = fipiId;
        this.egeNumber = egeNumber;
        this.taskText = taskText;
        this.solution = solution;
        this.explanation = explanation;
        this.source = source;
        this.textId = textId;
        this.taskType = taskType;
    }

    @NonNull
    public Integer getId() {
        return id;
    }

    @Nullable
    public String getFipiId() {
        return fipiId;
    }

    @NonNull
    public String getEgeNumber() {
        return egeNumber;
    }

    @Nullable
    public String getTaskText() {
        return taskText;
    }

    @Nullable
    public String getSolution() {
        return solution;
    }

    @Nullable
    public String getExplanation() {
        return explanation;
    }

    @Nullable
    public String getSource() {
        return source;
    }

    @Nullable
    public Integer getTextId() {
        return textId;
    }

    @NonNull
    public String getTaskType() {
        return taskType;
    }

    public void setId(@NonNull Integer id) {
        this.id = id;
    }

    public void setFipiId(@Nullable String fipiId) {
        this.fipiId = fipiId;
    }

    public void setEgeNumber(@NonNull String egeNumber) {
        this.egeNumber = egeNumber;
    }

    public void setTaskText(@Nullable String taskText) {
        this.taskText = taskText;
    }

    public void setSolution(@Nullable String solution) {
        this.solution = solution;
    }

    public void setExplanation(@Nullable String explanation) {
        this.explanation = explanation;
    }

    public void setSource(@Nullable String source) {
        this.source = source;
    }

    public void setTextId(@Nullable Integer textId) {
        this.textId = textId;
    }

    public void setTaskType(@NonNull String taskType) {
        this.taskType = taskType;
    }

    public com.ruege.mobile.model.TaskItem toTaskItem() {
        String currentContent = this.taskText != null ? this.taskText : "";
        String currentDescription = ""; 

        com.ruege.mobile.model.AnswerType answerType;
        switch (this.taskType) {
            case "SINGLE_CHOICE":
                answerType = com.ruege.mobile.model.AnswerType.SINGLE_CHOICE;
                break;
            case "MULTIPLE_CHOICE":
                answerType = com.ruege.mobile.model.AnswerType.MULTIPLE_CHOICE;
                break;
            default:
                answerType = com.ruege.mobile.model.AnswerType.TEXT;
                break;
        }

        int orderPosition;
        try {
            orderPosition = Integer.parseInt(this.egeNumber);
        } catch (NumberFormatException e) {
            orderPosition = 1;
        }

        return new com.ruege.mobile.model.TaskItem(
            String.valueOf(this.id),
            "Задание " + this.egeNumber,
            this.egeNumber,
            currentDescription,
            currentContent,
            answerType,
            1,
            0,
            null,
            this.solution,
            this.explanation,
            this.textId,
            orderPosition,
            null,
            false,
            null,
            null,
            0
        );
    }
} 