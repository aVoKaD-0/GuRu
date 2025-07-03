package com.ruege.mobile.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(
    tableName = "user_task_attempts",
    foreignKeys = {
        @ForeignKey(
            entity = UserEntity.class,
            parentColumns = "user_id",
            childColumns = "user_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(name = "index_user_task_attempts_user_id", value = "user_id"),
        @Index(name = "index_user_task_attempts_task_id", value = "task_id"),
        @Index(name = "index_user_task_attempts_unique", value = {"user_id", "task_id", "attempt_number"}, unique = true)
    }
)
public class UserTaskAttemptEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "attempt_id")
    private long attemptId;

    @ColumnInfo(name = "user_id")
    private long userId;

    @ColumnInfo(name = "task_id")
    private Integer taskId;

    @ColumnInfo(name = "attempt_number")
    private int attemptNumber;

    @ColumnInfo(name = "points_earned")
    private int pointsEarned;

    @ColumnInfo(name = "is_correct")
    private boolean isCorrect;

    @ColumnInfo(name = "time_spent")
    private int timeSpent; 

    @ColumnInfo(name = "answer_text")
    private String answerText;

    @ColumnInfo(name = "answer_timestamp")
    private long answerTimestamp;

    @ColumnInfo(name = "feedback")
    private String feedback;

    @Ignore
    public UserTaskAttemptEntity() {}

    public UserTaskAttemptEntity(long userId, Integer taskId, int attemptNumber, int pointsEarned, 
                               boolean isCorrect, int timeSpent, String answerText, 
                               long answerTimestamp, String feedback) {
        this.userId = userId;
        this.taskId = taskId;
        this.attemptNumber = attemptNumber;
        this.pointsEarned = pointsEarned;
        this.isCorrect = isCorrect;
        this.timeSpent = timeSpent;
        this.answerText = answerText;
        this.answerTimestamp = answerTimestamp;
        this.feedback = feedback;
    }

    
    public long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(long attemptId) {
        this.attemptId = attemptId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public int getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(int timeSpent) {
        this.timeSpent = timeSpent;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public long getAnswerTimestamp() {
        return answerTimestamp;
    }

    public void setAnswerTimestamp(long answerTimestamp) {
        this.answerTimestamp = answerTimestamp;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
} 