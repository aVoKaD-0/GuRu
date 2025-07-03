package com.ruege.mobile.ui.bottomsheet.helper

import android.content.Context
import android.text.Html
import timber.log.Timber
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import com.ruege.mobile.R
import com.ruege.mobile.databinding.TaskViewWithNavigationBinding
import com.ruege.mobile.databinding.ViewTaskContentDetailBinding
import com.ruege.mobile.model.AnswerCheckResult
import com.ruege.mobile.model.TaskItem

class TaskAnswerHelper(
    private val context: Context,
    private val binding: TaskViewWithNavigationBinding
) {
    fun displayAnswerResult(task: TaskItem, result: AnswerCheckResult) {
        Timber.d("TaskDisplayBottomSheet", "displayAnswerResult: task.isCorrect=${task.isCorrect}, task.explanation='${task.explanation}', result.explanation='${result.explanation}', result.correctAnswer='${result.correctAnswer}', result.userAnswer='${result.userAnswer}', result.pointsAwarded=${result.pointsAwarded}")
        val taskContentView = binding.taskContentContainer.getChildAt(0) ?: return
        val contentBinding = ViewTaskContentDetailBinding.bind(taskContentView)

        contentBinding.taskResultContainer.visibility = View.VISIBLE
        contentBinding.answerInputLayout.visibility = View.GONE
        contentBinding.submitAnswer.visibility = View.GONE
        contentBinding.answerInput.isEnabled = false

        contentBinding.resultStatus.text = if (task.isCorrect == true) "Верно!" else "Неверно"
        contentBinding.resultStatus.setTextColor(
            ContextCompat.getColor(
                context,
                if (task.isCorrect == true) R.color.correct_answer_green else R.color.incorrect_answer_red
            )
        )

        var shortInfoText = ""
        if (task.scoreAchieved != null) shortInfoText += "Баллы: ${task.scoreAchieved} из ${task.maxPoints}."
        
        if (task.isCorrect == false) {
             val correctAnswerDisplay = result.correctAnswer ?: "Н/Д"
             shortInfoText += "\nВаш ответ: ${result.userAnswer}\nПравильный ответ: $correctAnswerDisplay"
        } else if (task.isCorrect == true && result.userAnswer != result.correctAnswer && result.userAnswer.equals(result.correctAnswer, ignoreCase = true)) {
             val correctAnswerDisplay = result.correctAnswer ?: "Н/Д"
            shortInfoText += "\nЭталонный ответ: $correctAnswerDisplay"
        }

        contentBinding.resultShortInfo.text = shortInfoText.trim()

        if (!result.explanation.isNullOrBlank()) { 
            Timber.d("TaskDisplayBottomSheet", "Explanation IS NOT BLANK (from result), showing: '${result.explanation}'")
            contentBinding.showExplanationButton.visibility = View.VISIBLE
            contentBinding.taskExplanation.text = Html.fromHtml(result.explanation, Html.FROM_HTML_MODE_LEGACY)
            contentBinding.taskExplanation.visibility = View.GONE
            contentBinding.showExplanationButton.text = "Показать объяснение"
            contentBinding.showExplanationButton.setOnClickListener {button ->
                val isCurrentlyVisible = contentBinding.taskExplanation.visibility == View.VISIBLE
                contentBinding.taskExplanation.visibility = if (isCurrentlyVisible) View.GONE else View.VISIBLE
                (button as Button).text = if (isCurrentlyVisible) "Показать объяснение" else "Скрыть объяснение"
            }
        } else {
            Timber.d("TaskDisplayBottomSheet", "Explanation IS BLANK or NULL (from result). Raw explanation value was: '${result.explanation}'")
            contentBinding.showExplanationButton.visibility = View.GONE
            contentBinding.taskExplanation.visibility = View.GONE
        }
    }
} 