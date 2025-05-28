package com.ruege.mobile.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.ColumnInfo;

import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface PracticeStatisticsDao {
    public static class AggregatedPracticeStatistics {
        @ColumnInfo(name = "total_sum_attempts")
        public int totalAttempts;

        @ColumnInfo(name = "total_sum_correct_attempts")
        public int correctAttempts;

        public AggregatedPracticeStatistics(int totalAttempts, int correctAttempts) {
            this.totalAttempts = totalAttempts;
            this.correctAttempts = correctAttempts;
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PracticeStatisticsEntity statistics);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PracticeStatisticsEntity> statisticsList);

    @Update
    void update(PracticeStatisticsEntity statistics);

    @Query("SELECT * FROM practice_statistics WHERE ege_number = :egeNumber")
    Flow<PracticeStatisticsEntity> getStatisticsByEgeNumber(String egeNumber);

    @Query("SELECT * FROM practice_statistics ORDER BY total_attempts DESC")
    Flow<List<PracticeStatisticsEntity>> getAllStatisticsByAttemptsDesc();

    @Query("SELECT * FROM practice_statistics ORDER BY correct_attempts DESC")
    Flow<List<PracticeStatisticsEntity>> getAllStatisticsByCorrectAttemptsDesc();

    @Query("SELECT * FROM practice_statistics ORDER BY last_attempt_date DESC")
    Flow<List<PracticeStatisticsEntity>> getAllStatisticsByRecentActivity();

    @Query("SELECT * FROM practice_statistics WHERE total_attempts > 0")
    Flow<List<PracticeStatisticsEntity>> getStatisticsWithAttempts();

    @Query("SELECT * FROM practice_statistics ORDER BY CAST(ege_number AS INTEGER) ASC")
    Flow<List<PracticeStatisticsEntity>> getAllStatisticsSortedByEgeNumber();

    @Query("SELECT SUM(total_attempts) as total_sum_attempts, SUM(correct_attempts) as total_sum_correct_attempts FROM practice_statistics")
    Flow<AggregatedPracticeStatistics> getOverallAggregatedStatistics();

    @Query("DELETE FROM practice_statistics")
    void clearAllStatistics();

    /**
     * Обновляет статистику заданий определенного номера ЕГЭ при добавлении новой попытки
     */
    @Query("UPDATE practice_statistics SET " +
           "total_attempts = total_attempts + 1, " +
           "correct_attempts = correct_attempts + CASE WHEN :isCorrect THEN 1 ELSE 0 END, " +
           "last_attempt_date = :timestamp " +
           "WHERE ege_number = :egeNumber")
    void updateStatisticsAfterAttempt(String egeNumber, boolean isCorrect, long timestamp);

    /**
     * Создает запись статистики для номера задания ЕГЭ, если еще не существует
     */
    @Query("INSERT OR IGNORE INTO practice_statistics (ege_number, total_attempts, correct_attempts, last_attempt_date) " +
           "VALUES (:egeNumber, 0, 0, 0)")
    void createStatisticsIfNotExists(String egeNumber);
} 