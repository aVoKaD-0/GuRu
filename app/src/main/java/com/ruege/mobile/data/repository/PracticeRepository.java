package com.ruege.mobile.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ruege.mobile.data.local.dao.PracticeAttemptDao;
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao;
import com.ruege.mobile.data.local.dao.TaskDao;
import com.ruege.mobile.data.local.entity.PracticeAttemptEntity;
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity;
import com.ruege.mobile.data.local.entity.TaskEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import kotlinx.coroutines.flow.Flow;

public class PracticeRepository {
    private final PracticeAttemptDao practiceAttemptDao;
    private final PracticeStatisticsDao practiceStatisticsDao;
    private final TaskDao taskDao;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public PracticeRepository(PracticeAttemptDao practiceAttemptDao, 
                             PracticeStatisticsDao practiceStatisticsDao,
                             TaskDao taskDao) {
        this.practiceAttemptDao = practiceAttemptDao;
        this.practiceStatisticsDao = practiceStatisticsDao;
        this.taskDao = taskDao;
    }

    // Методы для работы с попытками
    public Flow<List<PracticeAttemptEntity>> getAttemptsByTaskId(Integer taskId) {
        return practiceAttemptDao.getAttemptsByTaskId(taskId);
    }

    public Flow<List<PracticeAttemptEntity>> getAttemptsByEgeNumber(String egeNumber) {
        return practiceAttemptDao.getAttemptsByEgeNumber(egeNumber);
    }

    public Flow<List<PracticeAttemptEntity>> getRecentAttempts(int limit) {
        return practiceAttemptDao.getRecentAttempts(limit);
    }

    public Flow<Integer> getAttemptCountForTask(Integer taskId) {
        return practiceAttemptDao.getAttemptCountForTask(taskId);
    }

    public LiveData<Integer> getTotalAttempts() {
        // Используем MutableLiveData как временное решение
        MutableLiveData<Integer> result = new MutableLiveData<>(0);
        executor.execute(() -> {
            // В реальном приложении здесь должен быть код для получения данных из DAO
            result.postValue(0);
        });
        return result;
    }

    public LiveData<Integer> getTotalCorrectAttempts() {
        // Используем MutableLiveData как временное решение
        MutableLiveData<Integer> result = new MutableLiveData<>(0);
        executor.execute(() -> {
            // В реальном приложении здесь должен быть код для получения данных из DAO
            result.postValue(0);
        });
        return result;
    }

    // Методы для работы со статистикой
    public Flow<PracticeStatisticsEntity> getStatisticsByEgeNumber(String egeNumber) {
        return practiceStatisticsDao.getStatisticsByEgeNumber(egeNumber);
    }

    public Flow<List<PracticeStatisticsEntity>> getAllStatisticsByAttemptsDesc() {
        return practiceStatisticsDao.getAllStatisticsByAttemptsDesc();
    }

    public Flow<List<PracticeStatisticsEntity>> getAllStatisticsByRecentActivity() {
        return practiceStatisticsDao.getAllStatisticsByRecentActivity();
    }

    // Базовый метод для сохранения попыток
    public void saveAttempt(TaskEntity task, boolean isCorrect) {
        executor.execute(() -> {
            long currentTime = System.currentTimeMillis();
            
            // 1. Сначала сохраняем попытку
            PracticeAttemptEntity attempt = new PracticeAttemptEntity(
                    task.getId(), 
                    isCorrect, 
                    currentTime
            );
            practiceAttemptDao.insert(attempt);
            
            // 2. Убеждаемся, что запись статистики существует
            practiceStatisticsDao.createStatisticsIfNotExists(task.getEgeNumber());
            
            // 3. Обновляем статистику
            practiceStatisticsDao.updateStatisticsAfterAttempt(
                    task.getEgeNumber(),
                    isCorrect,
                    currentTime
            );
        });
    }
    
    // Расширенный метод для сохранения попыток с дополнительными параметрами
    public void saveAttempt(TaskEntity task, boolean isCorrect, String source, String taskType, String textId) {
        executor.execute(() -> {
            long currentTime = System.currentTimeMillis();
            
            // 1. Сначала сохраняем попытку
            PracticeAttemptEntity attempt = new PracticeAttemptEntity(
                    task.getId(), 
                    isCorrect, 
                    currentTime
            );
            practiceAttemptDao.insert(attempt);
            
            // 2. Убеждаемся, что запись статистики существует
            practiceStatisticsDao.createStatisticsIfNotExists(task.getEgeNumber());
            
            // 3. Обновляем статистику
            practiceStatisticsDao.updateStatisticsAfterAttempt(
                    task.getEgeNumber(),
                    isCorrect,
                    currentTime
            );
            
            // Логирование дополнительных параметров можно добавить при необходимости
        });
    }

    // Получение статистики выполнения по всем заданиям
    public LiveData<List<PracticeStatisticsEntity>> getStatisticsWithAttempts() {
        // Используем MutableLiveData как временное решение
        MutableLiveData<List<PracticeStatisticsEntity>> result = new MutableLiveData<>(new ArrayList<>());
        executor.execute(() -> {
            // В реальном приложении здесь должен быть код для получения данных из DAO
            result.postValue(new ArrayList<>());
        });
        return result;
    }
    
    /**
     * Получает задание по его идентификатору
     * @param taskId идентификатор задания
     * @return задание или null, если не найдено
     */
    public TaskEntity getTaskById(Integer taskId) {
        return taskDao.getTaskByIdSync(taskId);
    }
} 