package com.ruege.mobile.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "practice_statistics")
public class PracticeStatisticsEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "ege_number")
    private String egeNumber;

    @ColumnInfo(name = "total_attempts")
    private int totalAttempts;

    @ColumnInfo(name = "correct_attempts")
    private int correctAttempts;

    @ColumnInfo(name = "last_attempt_date")
    private long lastAttemptDate;

    // Конструкторы
    public PracticeStatisticsEntity() {
    }

    @Ignore
    public PracticeStatisticsEntity(@NonNull String egeNumber, int totalAttempts, 
                                    int correctAttempts, long lastAttemptDate) {
        this.egeNumber = egeNumber;
        this.totalAttempts = totalAttempts;
        this.correctAttempts = correctAttempts;
        this.lastAttemptDate = lastAttemptDate;
    }

    // Геттеры
    @NonNull
    public String getEgeNumber() {
        return egeNumber;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public int getCorrectAttempts() {
        return correctAttempts;
    }

    public long getLastAttemptDate() {
        return lastAttemptDate;
    }

    // Сеттеры
    public void setEgeNumber(@NonNull String egeNumber) {
        this.egeNumber = egeNumber;
    }

    public void setTotalAttempts(int totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public void setCorrectAttempts(int correctAttempts) {
        this.correctAttempts = correctAttempts;
    }

    public void setLastAttemptDate(long lastAttemptDate) {
        this.lastAttemptDate = lastAttemptDate;
    }

    // Геттер для вычисляемого поля - процент правильных ответов
    public float getSuccessRate() {
        if (totalAttempts == 0) return 0;
        return (float) correctAttempts / totalAttempts * 100;
    }
} 