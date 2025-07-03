package com.ruege.mobile.ui.bottomsheet.helper

import timber.log.Timber
import android.view.View
import com.ruege.mobile.databinding.TaskViewWithNavigationBinding

class TaskUIHelper(private val binding: TaskViewWithNavigationBinding) {

    fun showLoadingState() {
        binding.tasksLoadingProgress.visibility = View.VISIBLE
        binding.tvBottomSheetErrorMessage.visibility = View.GONE
        binding.headerContainer.visibility = View.GONE
        binding.taskContentContainer.visibility = View.GONE
        binding.taskNavigationButtonsContainer.visibility = View.GONE
        Timber.d("TaskDisplayBottomSheet", "Displaying Loading State")
    }

    fun showErrorState(message: String) {
        binding.tasksLoadingProgress.visibility = View.GONE
        binding.tvBottomSheetErrorMessage.text = message
        binding.tvBottomSheetErrorMessage.visibility = View.VISIBLE
        binding.headerContainer.visibility = View.GONE
        binding.taskContentContainer.visibility = View.GONE
        binding.taskNavigationButtonsContainer.visibility = View.GONE
        Timber.d("TaskDisplayBottomSheet", "Displaying Error State: $message")
    }

    fun showEmptyState(message: String = "Нет данных для отображения") {
        binding.tasksLoadingProgress.visibility = View.GONE
        binding.tvBottomSheetErrorMessage.text = message
        binding.tvBottomSheetErrorMessage.visibility = View.VISIBLE
        binding.headerContainer.visibility = View.GONE
        binding.taskContentContainer.visibility = View.GONE
        binding.taskNavigationButtonsContainer.visibility = View.GONE
        Timber.d("TaskDisplayBottomSheet", "Displaying Empty State: $message")
    }

    fun showContentState() {
        binding.tasksLoadingProgress.visibility = View.GONE
        binding.tvBottomSheetErrorMessage.visibility = View.GONE
        binding.headerContainer.visibility = View.VISIBLE
        binding.taskContentContainer.visibility = View.VISIBLE
        binding.taskNavigationButtonsContainer.visibility = View.VISIBLE
        Timber.d("TaskDisplayBottomSheet", "Displaying Content State. headerContainer visible: ${binding.headerContainer.visibility == View.VISIBLE}, taskNavigationButtonsContainer visible: ${binding.taskNavigationButtonsContainer.visibility == View.VISIBLE}")
    }
} 