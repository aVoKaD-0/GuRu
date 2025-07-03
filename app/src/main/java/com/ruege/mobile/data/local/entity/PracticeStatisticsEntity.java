package com.ruege.mobile.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    
    @Nullable
    @ColumnInfo(name = "variant_data")
    private String variantData;

    public PracticeStatisticsEntity() {
    }

    @Ignore
    public PracticeStatisticsEntity(@NonNull String egeNumber, int totalAttempts, 
                                    int correctAttempts, long lastAttemptDate) {
        this.egeNumber = egeNumber;
        this.totalAttempts = totalAttempts;
        this.correctAttempts = correctAttempts;
        this.lastAttemptDate = lastAttemptDate;
        this.variantData = null;
    }
    
    @Ignore
    public PracticeStatisticsEntity(@NonNull String egeNumber, int totalAttempts, 
                                    int correctAttempts, long lastAttemptDate, @Nullable String variantData) {
        this.egeNumber = egeNumber;
        this.totalAttempts = totalAttempts;
        this.correctAttempts = correctAttempts;
        this.lastAttemptDate = lastAttemptDate;
        this.variantData = variantData;
    }

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
    
    @Nullable
    public String getVariantData() {
        return variantData;
    }

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
    
    public void setVariantData(@Nullable String variantData) {
        this.variantData = variantData;
    }

    public float getSuccessRate() {
        if (totalAttempts == 0) return 0;
        return (float) correctAttempts / totalAttempts * 100;
    }
} 