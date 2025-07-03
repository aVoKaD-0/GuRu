package com.ruege.mobile.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ruege.mobile.data.local.dao.PracticeAttemptDao
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.data.repository.TasksRepository
import com.ruege.mobile.model.PracticeStatisticItem
import com.ruege.mobile.model.PracticeAttemptItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class OverallStatisticsUiModel(
    val totalAttempts: Int,
    val correctAttempts: Int,
    val successRate: Int
)

@HiltViewModel
class PracticeStatisticsViewModel @Inject constructor(
    private val practiceStatisticsDao: PracticeStatisticsDao,
    private val practiceAttemptDao: PracticeAttemptDao,
    private val tasksRepository: TasksRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _overallStatisticsUiModel = MutableLiveData<OverallStatisticsUiModel?>()
    val overallStatisticsUiModel: LiveData<OverallStatisticsUiModel?> = _overallStatisticsUiModel

    private val _allTaskStatistics = MutableLiveData<List<PracticeStatisticItem>>()
    val allTaskStatistics: LiveData<List<PracticeStatisticItem>> = _allTaskStatistics

    private val _recentAttempts = MutableLiveData<List<PracticeAttemptItemUiModel>?>(null)
    val recentAttempts: LiveData<List<PracticeAttemptItemUiModel>?> = _recentAttempts

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    init {
        loadStatistics()
        loadRecentAttempts()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            practiceStatisticsDao.getOverallAggregatedStatistics().collect { stats ->
                if (stats != null) {
                    val successRate = if (stats.totalAttempts > 0) {
                        (stats.correctAttempts * 100 / stats.totalAttempts)
                    } else {
                        0
                    }
                    _overallStatisticsUiModel.postValue(
                        OverallStatisticsUiModel(
                            totalAttempts = stats.totalAttempts,
                            correctAttempts = stats.correctAttempts,
                            successRate = successRate
                        )
                    )
                } else {
                     _overallStatisticsUiModel.postValue(OverallStatisticsUiModel(0,0,0))
                }
            }
        }

        viewModelScope.launch {
            practiceStatisticsDao.getAllStatisticsSortedByEgeNumber().collect { entities ->
                val uiItems = entities.map { entity ->
                    mapEntityToUiItem(entity)
                }
                _allTaskStatistics.postValue(uiItems)
            }
        }
    }

    private fun mapEntityToUiItem(entity: PracticeStatisticsEntity): PracticeStatisticItem {
        val successRate = if (entity.totalAttempts > 0) {
            (entity.correctAttempts * 100 / entity.totalAttempts)
        } else {
            0
        }
        val lastAttemptDateFormatted = if (entity.lastAttemptDate > 0) {
            dateFormat.format(Date(entity.lastAttemptDate))
        } else {
            "-"
        }

        return PracticeStatisticItem(
            id = entity.egeNumber,
            egeDisplayNumber = "Задание ${entity.egeNumber}",
            totalAttempts = entity.totalAttempts,
            correctAttempts = entity.correctAttempts,
            successRate = successRate,
            lastAttemptDateFormatted = lastAttemptDateFormatted
        )
    }

    fun loadRecentAttempts(limit: Int = 20) {
        viewModelScope.launch {
            practiceAttemptDao.getRecentAttempts(limit)
                .map { attempts ->
                    if (attempts.isEmpty()) {
                        return@map emptyList<PracticeAttemptItemUiModel>()
                    }
                    val taskIds = attempts.map { it.taskId }.distinct()

                    val tasks = tasksRepository.getTasksByIds(taskIds)
                    val tasksMap = tasks.associateBy { it.id }

                    attempts.map { attemptEntity ->
                        val taskEntity = tasksMap[attemptEntity.taskId]
                        val egeTaskNumberDisplay = taskEntity?.egeNumber?.let { egeNumStr -> "Задание $egeNumStr" } 
                                                  ?: "Задача ID: ${attemptEntity.taskId}"
                        
                        PracticeAttemptItemUiModel(
                            attemptId = attemptEntity.attemptId,
                            taskId = attemptEntity.taskId,
                            egeTaskNumberDisplay = egeTaskNumberDisplay,
                            attemptDateFormatted = dateFormat.format(Date(attemptEntity.attemptDate)),
                            isCorrect = attemptEntity.isCorrect
                        )
                    }
                }
                .collect {
                    _recentAttempts.postValue(it)
                }
        }
    }
}
