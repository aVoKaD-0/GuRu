package com.ruege.mobile.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "practice_attempts",
    foreignKeys = {
        @ForeignKey(
            entity = TaskEntity.class,
            parentColumns = "id",
            childColumns = "task_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(name = "index_practice_attempts_task_id", value = "task_id")
    }
)
public class PracticeAttemptEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "attempt_id")
    private long attemptId;

    @ColumnInfo(name = "task_id")
    private Integer taskId;

    @ColumnInfo(name = "is_correct")
    private boolean isCorrect;

    @ColumnInfo(name = "attempt_date")
    private long attemptDate;

    public PracticeAttemptEntity() {
    }

    @Ignore
    public PracticeAttemptEntity(Integer taskId, boolean isCorrect, long attemptDate) {
        this.taskId = taskId;
        this.isCorrect = isCorrect;
        this.attemptDate = attemptDate;
    }

    public long getAttemptId() {
        return attemptId;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public long getAttemptDate() {
        return attemptDate;
    }

    public void setAttemptId(long attemptId) {
        this.attemptId = attemptId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public void setAttemptDate(long attemptDate) {
        this.attemptDate = attemptDate;
    }
} 