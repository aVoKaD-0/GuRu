package com.ruege.mobile.ui.bottomsheet.helper

import android.content.Context
import android.graphics.Typeface
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.ruege.mobile.R
import com.ruege.mobile.databinding.TaskViewWithNavigationBinding
import com.ruege.mobile.databinding.ViewTaskContentDetailBinding
import com.ruege.mobile.model.TaskItem
import com.ruege.mobile.ui.viewmodel.TasksViewModel
import java.util.regex.Pattern

class TaskDisplayHelper(
    private val context: Context,
    private val binding: TaskViewWithNavigationBinding,
    private val view: View,
    private val viewModel: TasksViewModel,
    private val taskUIHelper: TaskUIHelper,
    private val taskNavigationHelper: TaskNavigationHelper,
    private val getTasks: () -> MutableList<TaskItem>,
    private val getTaskNumberViewIds: () -> IntArray?,
    private val getActiveTaskIndex: () -> Int,
    private val updateActiveTaskIndex: (Int) -> Unit
) {

    fun showTaskAtIndex(index: Int, shouldScroll: Boolean = true) {
        val currentTasks = getTasks()
        if (index < 0 || index >= currentTasks.size) {
            Timber.e("TaskDisplayBottomSheet", "Invalid index $index for tasks size ${currentTasks.size}")
            if (currentTasks.isEmpty()){
                taskUIHelper.showEmptyState("Нет заданий для отображения.")
                binding.taskNumbersScroll.visibility = View.GONE
                binding.btnPrevTask.visibility = View.GONE
                binding.btnNextTask.visibility = View.GONE
            } else {
                taskUIHelper.showErrorState("Ошибка: Неверный номер задания ($index из ${currentTasks.size})")
            }
            return
        }
        taskUIHelper.showContentState()

        val task = currentTasks[index]
        updateActiveTaskIndex(index)

        viewModel.clearAdditionalTextState()
        if (task.textId != null) {
            Timber.d("TaskDisplayBottomSheet", "Запрос дополнительного текста для textId: ${task.textId}")
            viewModel.requestTaskTextForCurrentTask(task.textId)
        }

        binding.taskContentContainer.removeAllViews()
        val taskContentView = LayoutInflater.from(context).inflate(R.layout.view_task_content_detail, binding.taskContentContainer, false)
        val contentBinding = ViewTaskContentDetailBinding.bind(taskContentView)
        
        contentBinding.taskContent.post {
            val taskText = task.content
            val textToDisplay: CharSequence = if (taskText.contains("{")) {
                formatTextWithCurlyBraceHighlights(taskText)
            } else {
                taskText
            }
            contentBinding.taskContent.text = textToDisplay
            contentBinding.taskContent.requestLayout()
        }

        if (task.textId != null) {
            contentBinding.textToggleContainer.visibility = View.VISIBLE
            contentBinding.taskAdditionalTextView.visibility = View.GONE
            contentBinding.showTextButton.visibility = View.VISIBLE
            contentBinding.hideTextButton.visibility = View.GONE

            contentBinding.showTextButton.setOnClickListener {
                contentBinding.showTextButton.visibility = View.GONE
                contentBinding.hideTextButton.visibility = View.VISIBLE

                val loadedText = viewModel.taskAdditionalText.value
                val isLoading = viewModel.taskAdditionalTextLoading.value == true

                if (loadedText != null) { 
                    val textToDisplay: CharSequence = if (loadedText.contains("{")) {
                        formatTextWithCurlyBraceHighlights(loadedText)
                    } else {
                        Html.fromHtml(loadedText, Html.FROM_HTML_MODE_LEGACY)
                    }
                    contentBinding.taskAdditionalTextView.text = textToDisplay
                    contentBinding.taskAdditionalTextView.visibility = View.VISIBLE
                } else if (!isLoading) { 
                    viewModel.requestTaskTextForCurrentTask(task.taskId.toInt())
                }
            }
            contentBinding.hideTextButton.setOnClickListener {
                contentBinding.taskAdditionalTextView.visibility = View.GONE
                contentBinding.showTextButton.visibility = View.VISIBLE
                contentBinding.hideTextButton.visibility = View.GONE
            }
        } else {
            contentBinding.textToggleContainer.visibility = View.GONE
        }

        contentBinding.answerInputLayout.visibility = View.VISIBLE
        contentBinding.submitAnswer.visibility = View.VISIBLE
        contentBinding.answerInput.setText(task.userAnswer ?: "")
        contentBinding.answerInput.isEnabled = true

        contentBinding.taskResultContainer.visibility = View.GONE

        contentBinding.submitAnswer.setOnClickListener {
            val userAnswer = contentBinding.answerInput.text.toString().trim()
            if (userAnswer.isNotEmpty()) {
                Timber.d("TaskDisplayBottomSheet", "Ответ пользователя для taskId ${task.taskId}: $userAnswer")
                
                viewModel.checkAnswer(task.taskId, userAnswer)
            } else {
                Toast.makeText(context, "Введите ваш ответ", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.taskContentContainer.addView(taskContentView)
        taskNavigationHelper.updateNavigationButtonsState(index, currentTasks.size, getActiveTaskIndex())
        taskNavigationHelper.updateTaskNumberSelection(index, getTaskNumberViewIds(), currentTasks)

        if (shouldScroll) {
            binding.taskNumbersScroll.post {
                getTaskNumberViewIds()?.getOrNull(index)?.let { viewId ->
                    view.findViewById<View>(viewId)?.let { numberButton ->
                        val navView = numberButton.parent as? View
                        if (navView != null) {
                            val scrollX = navView.left - (binding.taskNumbersScroll.width / 2) + (navView.width / 2)
                            binding.taskNumbersScroll.smoothScrollTo(scrollX, 0)
                        }
                    }
                }
            }
        }
    }

    fun formatTextWithCurlyBraceHighlights(text: String): SpannableString {
        Timber.d("TaskDisplayBottomSheet", "formatTextWithCurlyBraceHighlights - Input text: [$text]")
        val displayText = text.replace(Regex("\\{([^}]+)\\}"), "$1")
        Timber.d("TaskDisplayBottomSheet", "formatTextWithCurlyBraceHighlights - Display text (after replace): [$displayText]")
        val spannableString = SpannableString(displayText)
    
        val pattern = Pattern.compile("\\{([^}]+)\\}")
        val matcher = pattern.matcher(text)
    
        var cumulativeOffset = 0
        var matchesFound = 0
    
        while (matcher.find()) {
            matchesFound++
            val wordInBraces = matcher.group(0)!!
            val wordOnly = matcher.group(1)!!
            Timber.d("TaskDisplayBottomSheet", "formatTextWithCurlyBraceHighlights - Match $matchesFound: wordInBraces='${wordInBraces}', wordOnly='${wordOnly}'")
    
            val startInDisplayText = matcher.start() - cumulativeOffset
            val endInDisplayText = startInDisplayText + wordOnly.length
            Timber.d("TaskDisplayBottomSheet", "formatTextWithCurlyBraceHighlights - Calculated indices for displayText: start=$startInDisplayText, end=$endInDisplayText (cumulativeOffset=$cumulativeOffset)")
    
            if (startInDisplayText >= 0 && endInDisplayText <= spannableString.length) {
                spannableString.setSpan(StyleSpan(Typeface.BOLD), startInDisplayText, endInDisplayText, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                Timber.d("TaskDisplayBottomSheet", "formatTextWithCurlyBraceHighlights - Applied BOLD span to '${spannableString.substring(startInDisplayText, endInDisplayText)}'")
            } else {
                Timber.w("TaskDisplayBottomSheet", "formatTextWithCurlyBraceHighlights - Invalid indices, skipping span for word '$wordOnly'")
            }
            cumulativeOffset += 2
        }
        if (matchesFound == 0) {
            Timber.d("TaskDisplayBottomSheet", "formatTextWithCurlyBraceHighlights - No matches found for {} pattern.")
        }
        Timber.d("TaskDisplayBottomSheet", "formatTextWithCurlyBraceHighlights - Returning spannable: $spannableString")
        return spannableString
    }
} 