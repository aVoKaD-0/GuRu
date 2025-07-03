package com.ruege.mobile.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(
    tableName = "task_options",
    foreignKeys = {
        @ForeignKey(
            entity = TaskEntity.class,
            parentColumns = "id",
            childColumns = "task_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(name = "index_task_options_task_id", value = "task_id")
    }
)
public class TaskOptionEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "option_id")
    private long optionId;

    @ColumnInfo(name = "task_id")
    private Integer taskId;

    @ColumnInfo(name = "text")
    private String text;

    @ColumnInfo(name = "is_correct")
    private boolean isCorrect;

    @ColumnInfo(name = "explanation")
    private String explanation;

    @ColumnInfo(name = "order_position")
    private int orderPosition;
    
    public long getOptionId() {
        return optionId;
    }

    public void setOptionId(long optionId) {
        this.optionId = optionId;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public int getOrderPosition() {
        return orderPosition;
    }

    public void setOrderPosition(int orderPosition) {
        this.orderPosition = orderPosition;
    }
} 