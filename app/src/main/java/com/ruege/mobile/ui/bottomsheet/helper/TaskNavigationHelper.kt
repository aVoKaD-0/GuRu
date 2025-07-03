package com.ruege.mobile.ui.bottomsheet.helper

import android.content.Context
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.ruege.mobile.R
import com.ruege.mobile.databinding.TaskViewWithNavigationBinding
import com.ruege.mobile.model.TaskItem

class TaskNavigationHelper(
    private val context: Context,
    private val binding: TaskViewWithNavigationBinding,
    private val view: View,
    private val onTaskSelected: (Int) -> Unit
) {

    fun setupTaskNavigation(tasks: List<TaskItem>): IntArray {
        Timber.d("TaskDisplayBottomSheet", "setupTaskNavigation called with ${tasks.size} tasks.")
        binding.taskNumbersContainer.removeAllViews()
        if (tasks.isEmpty()) {
            Timber.w("TaskDisplayBottomSheet", "setupTaskNavigation: tasks list is empty, hiding navigation scroll.")
            binding.taskNumbersScroll.visibility = View.GONE
            return IntArray(0)
        }
        binding.taskNumbersScroll.visibility = View.VISIBLE
        val ids = IntArray(tasks.size)
        for (i in tasks.indices) {
            val navView = LayoutInflater.from(context).inflate(R.layout.item_task_number_navigation, binding.taskNumbersContainer, false)
            val button = navView.findViewById<TextView>(R.id.task_number_button)
            ids[i] = View.generateViewId()
            button.id = ids[i]
            button.text = (i + 1).toString()
            button.setOnClickListener { onTaskSelected(i) }
            binding.taskNumbersContainer.addView(navView)
        }
        return ids
    }

    fun updateTaskNumberSelection(selectedIndex: Int, taskNumberViewIds: IntArray?, currentTasks: List<TaskItem>) {
        Timber.d("TaskDisplayBottomSheet", "updateTaskNumberSelection: Вызван для selectedIndex: $selectedIndex. Всего задач: ${currentTasks.size}")
        taskNumberViewIds?.let { ids ->
            if (selectedIndex < 0 || selectedIndex >= ids.size) {
                Timber.e("TaskDisplayBottomSheet", "updateTaskNumberSelection: selectedIndex ($selectedIndex) выходит за пределы ids (size: ${ids.size}). Пропускаем.")
                return
            }
            for (i in ids.indices) {
                val button = view.findViewById<TextView>(ids[i])
                if (button == null) {
                    Timber.w("TaskDisplayBottomSheet", "updateTaskNumberSelection: Кнопка с ID ${ids[i]} не найдена для индекса $i")
                    continue
                }
                val isSelected = (i == selectedIndex)
                val task = currentTasks.getOrNull(i)

                if (isSelected) {
                    button.background = ContextCompat.getDrawable(context, R.drawable.item_task_number_bg_selector)
                    button.isSelected = true
                    button.setTextColor(ContextCompat.getColor(context, android.R.color.white))

                } else {
                    button.isSelected = false
                    if (task != null && task.isSolved) {
                        button.background = ContextCompat.getDrawable(context, R.drawable.item_task_number_bg_solved)
                        button.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    } else {
                        button.background = ContextCompat.getDrawable(context, R.drawable.item_task_number_bg_selector)
                        
                        val textColorAttr = android.R.attr.textColorPrimary
                        val typedValue = android.util.TypedValue()
                        context.theme.resolveAttribute(textColorAttr, typedValue, true)
                        button.setTextColor(ContextCompat.getColor(context, typedValue.resourceId))
                    }
                }
            }
        } ?: Timber.w("TaskDisplayBottomSheet", "updateTaskNumberSelection: taskNumberViewIds is null.")
    }

    fun updateNavigationButtonsState(currentIndex: Int, tasksSize: Int, currentActiveTaskIndex: Int) {
        binding.btnPrevTask.isEnabled = currentIndex > 0
        binding.btnNextTask.isEnabled = currentIndex < tasksSize - 1

        binding.btnPrevTask.setOnClickListener { if (currentActiveTaskIndex > 0) onTaskSelected(currentActiveTaskIndex - 1) }
        binding.btnNextTask.setOnClickListener { if (currentActiveTaskIndex < tasksSize - 1) onTaskSelected(currentActiveTaskIndex + 1) }
    }

    fun updateNavigationUi(currentTasks: List<TaskItem>, currentActiveTaskIndex: Int, taskNumberViewIds: IntArray?) {
        if (currentTasks.isNotEmpty()) {
            binding.taskNumbersScroll.visibility = View.VISIBLE
            binding.taskNavigationButtonsContainer.visibility = View.VISIBLE
            updateTaskNumberSelection(currentActiveTaskIndex, taskNumberViewIds, currentTasks)
            updateNavigationButtonsState(currentActiveTaskIndex, currentTasks.size, currentActiveTaskIndex)
        } else {
            binding.taskNumbersScroll.visibility = View.GONE
            binding.taskNavigationButtonsContainer.visibility = View.GONE
        }
    }
} 