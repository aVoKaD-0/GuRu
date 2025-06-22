package com.ruege.mobile.ui.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ruege.mobile.R
import com.ruege.mobile.model.TaskItem
import com.ruege.mobile.ui.viewmodel.TasksViewModel
import com.ruege.mobile.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import com.ruege.mobile.data.repository.TasksRepository
import androidx.core.widget.NestedScrollView
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import java.util.regex.Pattern

private const val ARG_CATEGORY_ID = "category_id"
private const val ARG_TITLE = "title"
private const val MAX_ATTEMPTS = 3

@AndroidEntryPoint
class TaskDisplayBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG_TASK_BS = "TaskDisplayBottomSheet"

        @JvmStatic
        fun newInstance(categoryId: String, title: String): TaskDisplayBottomSheetDialogFragment {
            return TaskDisplayBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_ID, categoryId)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }

    private val viewModel: TasksViewModel by activityViewModels()

    private var categoryId: String? = null
    private var sheetTitle: String? = null

    private var taskBottomSheetTitle: TextView? = null
    private var tasksLoadingProgress: ProgressBar? = null
    private var tvBottomSheetErrorMessage: TextView? = null
    private var headerContainer: LinearLayout? = null
    private var taskNumbersScroll: HorizontalScrollView? = null
    private var taskNumbersContainer: LinearLayout? = null
    private var taskContentContainer: FrameLayout? = null
    private var taskNavigationButtonsContainer: LinearLayout? = null
    private var btnPrevTask: Button? = null
    private var btnNextTask: Button? = null
    private var loadMoreProgress: ProgressBar? = null
    
    private var currentTasks: MutableList<TaskItem> = mutableListOf()
    private var taskNumberViewIds: IntArray? = null
    private var currentActiveTaskIndex = 0

    private var tasksObserver: Observer<List<TaskItem>?>? = null
    private var errorMessageObserver: Observer<String?>? = null
    private var additionalTextObserver: Observer<String?>? = null
    private var additionalTextLoadingObserver: Observer<Boolean?>? = null
    private var additionalTextErrorObserver: Observer<String?>? = null
    private var isLoadingMoreTasksObserver: Observer<Boolean>? = null

    private var egeNumberForApi: String? = null
    private var isCurrentlyLoadingMoreLocal = false

    private var isViewCreatedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getString(ARG_CATEGORY_ID)
            sheetTitle = it.getString(ARG_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.task_view_with_navigation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        taskBottomSheetTitle?.text = sheetTitle ?: "Задания"

        setInitialUiState()
        initObserversAndLoadInitialData()
        setupPaginationScrollListener()
    }

    private fun bindViews(view: View) {
        taskBottomSheetTitle = view.findViewById(R.id.task_bottom_sheet_title)
        tasksLoadingProgress = view.findViewById(R.id.tasks_loading_progress)
        tvBottomSheetErrorMessage = view.findViewById(R.id.tv_bottom_sheet_error_message)
        headerContainer = view.findViewById(R.id.header_container)
        taskNumbersScroll = view.findViewById(R.id.task_numbers_scroll)
        taskNumbersContainer = view.findViewById(R.id.task_numbers_container)
        taskContentContainer = view.findViewById(R.id.task_content_container)
        taskNavigationButtonsContainer = view.findViewById(R.id.task_navigation_buttons_container)
        btnPrevTask = view.findViewById(R.id.btn_prev_task)
        btnNextTask = view.findViewById(R.id.btn_next_task)
        loadMoreProgress = view.findViewById(R.id.load_more_progress)
    }

    private fun setInitialUiState() {
        tasksLoadingProgress?.visibility = View.VISIBLE
        tvBottomSheetErrorMessage?.visibility = View.GONE
        headerContainer?.visibility = View.GONE
        taskContentContainer?.visibility = View.GONE
        taskNavigationButtonsContainer?.visibility = View.GONE
    }

    private fun initObserversAndLoadInitialData() {
        setupGeneralObservers()

        tasksObserver = Observer { tasks ->
            Log.d(TAG_TASK_BS, "Tasks Observer: START. Received ${tasks?.size ?: "null"} tasks. Current task count: ${currentTasks.size}.")

            val previousTaskId = if (currentTasks.isNotEmpty() && currentActiveTaskIndex < currentTasks.size) {
                currentTasks[currentActiveTaskIndex].taskId
            } else {
                null
            }
            val previousScrollX = taskNumbersScroll?.scrollX ?: 0
            val wasPagingUpdate = tasks != null && currentTasks.isNotEmpty() && tasks.size > currentTasks.size

            currentTasks.clear()
            if (tasks.isNullOrEmpty()) {
                val errorMessage = viewModel.errorMessage.value ?: "Нет доступных заданий для этой категории."
                if (viewModel.errorMessage.value?.contains(TasksRepository.NO_DATA_AND_NETWORK_ISSUE_FLAG) == true) {
                    showErrorState("Нет данных или проблема с сетью. Проверьте подключение.")
                } else {
                    showEmptyState(errorMessage)
                }
                updateNavigationUi()
                return@Observer
            }
            currentTasks.addAll(tasks)

            var newActiveIndex = if (previousTaskId != null) {
                currentTasks.indexOfFirst { it.taskId == previousTaskId }.takeIf { it != -1 } ?: 0
            } else {
                0
            }
            
            if (newActiveIndex >= currentTasks.size) {
                newActiveIndex = 0
            }
            currentActiveTaskIndex = newActiveIndex

            taskNumberViewIds = setupTaskNavigation(currentTasks)
            showTaskAtIndex(currentActiveTaskIndex, !wasPagingUpdate)

            taskNumbersScroll?.post {
                if (wasPagingUpdate) {
                    Log.d(TAG_TASK_BS, "Paging update. Restoring scroll position to $previousScrollX")
                    taskNumbersScroll?.smoothScrollTo(previousScrollX, 0)
                }
            }
            updateNavigationUi()
            Log.d(TAG_TASK_BS, "Tasks Observer: END. Active index: $currentActiveTaskIndex.")
        }

        viewModel.tasks.observe(viewLifecycleOwner, tasksObserver!!)

        val egeNumber = categoryId?.removePrefix("task_group_")
        if (egeNumber != null) {
            this.egeNumberForApi = egeNumber
            val existingTasks = viewModel.tasks.value
            if (existingTasks == null || existingTasks.isEmpty() || !isViewCreatedOnce) {
                Log.d(TAG_TASK_BS, "Список задач пуст, отсутствует или view не создавался. Загружаем для egeNumber: $egeNumber")
                showLoadingState()
                viewModel.loadTasksByCategory(egeNumber)
            } else {
                Log.d(TAG_TASK_BS, "Используем существующие задачи (${existingTasks.size} шт.) для egeNumber: $egeNumber")
                if (currentTasks.isNotEmpty()) {
                    Log.d(TAG_TASK_BS, "Calling setupTaskNavigation for existing tasks in currentTasks.")
                    taskNumberViewIds = setupTaskNavigation(currentTasks)
                    showTaskAtIndex(currentActiveTaskIndex)
                } else if (existingTasks.isNotEmpty()) {
                    currentTasks.addAll(existingTasks)
                    currentActiveTaskIndex = 0
                    taskNumberViewIds = setupTaskNavigation(currentTasks)
                    showTaskAtIndex(currentActiveTaskIndex)
                } else {
                    showEmptyState("Нет сохраненных заданий.")
                    updateNavigationUi()
                }
            }
        } else {
            Log.e(TAG_TASK_BS, "egeNumber is null at init (derived from categoryId). Cannot load tasks initially or use existing.")
            showEmptyState("Не указан номер задания для отображения.")
            updateNavigationUi()
        }
    }

    private fun setupGeneralObservers() {
        errorMessageObserver = Observer { errorMessageValue ->
            if (!isAdded || view == null) return@Observer
            if (currentTasks.isEmpty() && tvBottomSheetErrorMessage?.visibility == View.VISIBLE) {
                val messageToShow = when {
                    TasksRepository.NO_DATA_AND_NETWORK_ISSUE_FLAG == errorMessageValue -> "Ничего не найдено. Проверьте подключение к интернету."
                    !errorMessageValue.isNullOrEmpty() -> errorMessageValue
                    else -> "Произошла ошибка."
                }
                tvBottomSheetErrorMessage?.text = messageToShow
            }
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner, errorMessageObserver!!)
        setupAdditionalTextObservers()
        setupAnswerCheckObserver()

        isLoadingMoreTasksObserver = Observer { isLoading ->
            if (!isAdded || view == null) return@Observer
            loadMoreProgress?.visibility = if (isLoading == true) View.VISIBLE else View.GONE
            if (isLoading == false) {
                isCurrentlyLoadingMoreLocal = false
            }
        }
        viewModel.isLoadingMoreTasks.observe(viewLifecycleOwner, isLoadingMoreTasksObserver!!)
    }

    private fun setupAdditionalTextObservers() {
        additionalTextObserver = Observer { text ->
            if (!isAdded || view == null || currentTasks.isEmpty() || currentActiveTaskIndex >= currentTasks.size) return@Observer
            val task = currentTasks[currentActiveTaskIndex]
            if (task.textId == null) return@Observer

            val currentTaskContentView = taskContentContainer?.getChildAt(0) ?: return@Observer
            val targetAdditionalTextView = currentTaskContentView.findViewById<TextView>(R.id.task_additional_text_view)
            val currentShowTextButton = currentTaskContentView.findViewById<TextView>(R.id.show_text_button)
            val currentHideTextButton = currentTaskContentView.findViewById<TextView>(R.id.hide_text_button)

            val isLoading = viewModel.taskAdditionalTextLoading.value == true
            val hasError = !viewModel.taskAdditionalTextError.value.isNullOrEmpty()

            if (isLoading || hasError) {
                targetAdditionalTextView?.visibility = View.GONE
                return@Observer
            }

            targetAdditionalTextView?.let { textView ->
                if (text != null) {
                    val textToDisplay: CharSequence = if (text.contains("{")) {
                        formatTextWithCurlyBraceHighlights(text)
                    } else {
                        Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
                    }
                    textView.text = textToDisplay
                    if (currentHideTextButton?.visibility == View.VISIBLE) {
                        textView.visibility = View.VISIBLE
                    }
                } else {
                    textView.text = ""
                    textView.visibility = View.GONE
                    currentShowTextButton?.visibility = View.VISIBLE
                    currentHideTextButton?.visibility = View.GONE
                }
            }
        }

        additionalTextLoadingObserver = Observer { isLoading ->
            if (!isAdded || view == null || currentTasks.isEmpty() || currentActiveTaskIndex >= currentTasks.size) return@Observer
            val task = currentTasks[currentActiveTaskIndex]
            if (task.textId == null) return@Observer

            taskContentContainer?.findViewById<ProgressBar>(R.id.task_additional_text_loading)?.visibility = if (isLoading == true) View.VISIBLE else View.GONE
            if (isLoading == true) {
                taskContentContainer?.findViewById<TextView>(R.id.task_additional_text_view)?.visibility = View.GONE
                taskContentContainer?.findViewById<TextView>(R.id.task_additional_text_error)?.visibility = View.GONE
            }
        }

        additionalTextErrorObserver = Observer { error ->
            if (!isAdded || view == null || currentTasks.isEmpty() || currentActiveTaskIndex >= currentTasks.size) return@Observer
            val task = currentTasks[currentActiveTaskIndex]
            if (task.textId == null) return@Observer
            
            taskContentContainer?.findViewById<TextView>(R.id.task_additional_text_error)?.let {
                if (!error.isNullOrEmpty()) {
                    it.text = error
                    it.visibility = View.VISIBLE
                    taskContentContainer?.findViewById<TextView>(R.id.task_additional_text_view)?.visibility = View.GONE
                    taskContentContainer?.findViewById<ProgressBar>(R.id.task_additional_text_loading)?.visibility = View.GONE
                } else {
                    it.visibility = View.GONE
                }

            }

        }

        viewModel.taskAdditionalText.observe(viewLifecycleOwner, additionalTextObserver!!)
        viewModel.taskAdditionalTextLoading.observe(viewLifecycleOwner, additionalTextLoadingObserver!!)
        viewModel.taskAdditionalTextError.observe(viewLifecycleOwner, additionalTextErrorObserver!!)
    }
    
    private fun setupAnswerCheckObserver() {
        viewModel.answerCheckResultLiveData.observe(viewLifecycleOwner, Observer { result ->
            if (!isAdded || view == null || result == null) return@Observer
            Log.d(TAG_TASK_BS, "AnswerCheckResult LiveData: Получен результат для taskId: ${result.taskId}")

            val taskIndex = currentTasks.indexOfFirst { it.taskId == result.taskId }
            if (taskIndex == -1) {
                Log.e(TAG_TASK_BS, "Не найдена задача с taskId: ${result.taskId} в currentTasks")
                return@Observer
            }

            val task = currentTasks[taskIndex]

            task.userAnswer = result.userAnswer
            task.attemptsMade++
            task.isCorrect = result.isCorrect
            task.correctAnswer = result.correctAnswer 
            task.explanation = result.explanation
            task.scoreAchieved = result.pointsAwarded

            task.isSolved = true 

            val egeNumber = categoryId?.removePrefix("task_group_")
            if (egeNumber != null) {
                viewModel.updateUserProgressForTask(task, egeNumber)
                Log.d(TAG_TASK_BS, "Прогресс обновлен для задачи ${task.taskId} (любой ответ)")
            } else {
                Log.e(TAG_TASK_BS, "egeNumber is null, не могу обновить прогресс для задачи ${task.taskId}")
            }

            if (taskIndex == currentActiveTaskIndex) {
                updateTaskNumberSelection(currentActiveTaskIndex)
                displayAnswerResult(task, result) 
            }
        })
    }

    private fun displayAnswerResult(task: TaskItem, result: com.ruege.mobile.model.AnswerCheckResult) {
        Log.d(TAG_TASK_BS, "displayAnswerResult: task.isCorrect=${task.isCorrect}, task.explanation='${task.explanation}', result.explanation='${result.explanation}', result.correctAnswer='${result.correctAnswer}', result.userAnswer='${result.userAnswer}', result.pointsAwarded=${result.pointsAwarded}")
        val taskContentView = taskContentContainer?.getChildAt(0) ?: return
        val answerInputLayout = taskContentView.findViewById<TextInputLayout>(R.id.answer_input_layout)
        val answerInputEditText = taskContentView.findViewById<TextInputEditText>(R.id.answer_input)
        val submitAnswerButton = taskContentView.findViewById<Button>(R.id.submit_answer)
        val resultContainer = taskContentView.findViewById<LinearLayout>(R.id.task_result_container)
        val resultStatusTextView = taskContentView.findViewById<TextView>(R.id.result_status)
        val resultShortInfoTextView = taskContentView.findViewById<TextView>(R.id.result_short_info)
        val showExplanationButton = taskContentView.findViewById<Button>(R.id.show_explanation_button)
        val taskExplanationTextView = taskContentView.findViewById<TextView>(R.id.task_explanation)

        resultContainer.visibility = View.VISIBLE
        answerInputLayout.visibility = View.GONE
        submitAnswerButton.visibility = View.GONE
        answerInputEditText.isEnabled = false

        resultStatusTextView.text = if (task.isCorrect == true) "Верно!" else "Неверно"
        resultStatusTextView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
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

        resultShortInfoTextView.text = shortInfoText.trim()

        if (!result.explanation.isNullOrBlank()) { 
            Log.d(TAG_TASK_BS, "Explanation IS NOT BLANK (from result), showing: '${result.explanation}'")
            showExplanationButton.visibility = View.VISIBLE
            taskExplanationTextView.text = Html.fromHtml(result.explanation, Html.FROM_HTML_MODE_LEGACY)
            taskExplanationTextView.visibility = View.GONE
            showExplanationButton.text = "Показать объяснение"
            showExplanationButton.setOnClickListener {button ->
                val isCurrentlyVisible = taskExplanationTextView.visibility == View.VISIBLE
                taskExplanationTextView.visibility = if (isCurrentlyVisible) View.GONE else View.VISIBLE
                (button as Button).text = if (isCurrentlyVisible) "Показать объяснение" else "Скрыть объяснение"
            }
        } else {
            Log.d(TAG_TASK_BS, "Explanation IS BLANK or NULL (from result). Raw explanation value was: '${result.explanation}'")
            showExplanationButton.visibility = View.GONE
            taskExplanationTextView.visibility = View.GONE
        }
    }

    private fun showLoadingState() {
        tasksLoadingProgress?.visibility = View.VISIBLE
        tvBottomSheetErrorMessage?.visibility = View.GONE
        headerContainer?.visibility = View.GONE
        taskContentContainer?.visibility = View.GONE
        taskNavigationButtonsContainer?.visibility = View.GONE
        Log.d(TAG_TASK_BS, "Displaying Loading State")
    }

    private fun showErrorState(message: String) {
        tasksLoadingProgress?.visibility = View.GONE
        tvBottomSheetErrorMessage?.text = message
        tvBottomSheetErrorMessage?.visibility = View.VISIBLE
        headerContainer?.visibility = View.GONE
        taskContentContainer?.visibility = View.GONE
        taskNavigationButtonsContainer?.visibility = View.GONE
        Log.d(TAG_TASK_BS, "Displaying Error State: $message")
    }

    private fun showEmptyState(message: String = "Нет данных для отображения") {
        tasksLoadingProgress?.visibility = View.GONE
        tvBottomSheetErrorMessage?.text = message
        tvBottomSheetErrorMessage?.visibility = View.VISIBLE
        headerContainer?.visibility = View.GONE
        taskContentContainer?.visibility = View.GONE
        taskNavigationButtonsContainer?.visibility = View.GONE
        Log.d(TAG_TASK_BS, "Displaying Empty State: $message")
    }

    private fun showContentState() {
        tasksLoadingProgress?.visibility = View.GONE
        tvBottomSheetErrorMessage?.visibility = View.GONE
        headerContainer?.visibility = View.VISIBLE
        taskContentContainer?.visibility = View.VISIBLE
        taskNavigationButtonsContainer?.visibility = View.VISIBLE
        Log.d(TAG_TASK_BS, "Displaying Content State. headerContainer visible: ${headerContainer?.visibility == View.VISIBLE}, taskNavigationButtonsContainer visible: ${taskNavigationButtonsContainer?.visibility == View.VISIBLE}")
    }

    private fun setupTaskNavigation(tasks: List<TaskItem>): IntArray {
        Log.d(TAG_TASK_BS, "setupTaskNavigation called with ${tasks.size} tasks. CurrentActiveTaskIndex перед перестроением: $currentActiveTaskIndex")
        taskNumbersContainer?.removeAllViews()
        if (tasks.isEmpty()) {
            Log.w(TAG_TASK_BS, "setupTaskNavigation: tasks list is empty, hiding navigation scroll.")
            taskNumbersScroll?.visibility = View.GONE
            return IntArray(0)
        }
        taskNumbersScroll?.visibility = View.VISIBLE
        val ids = IntArray(tasks.size)
        for (i in tasks.indices) {
            val navView = LayoutInflater.from(context).inflate(R.layout.item_task_number_navigation, taskNumbersContainer, false)
            val button = navView.findViewById<TextView>(R.id.task_number_button)
            ids[i] = View.generateViewId()
            button.id = ids[i]
            button.text = (i + 1).toString()
            button.setOnClickListener { showTaskAtIndex(i) }
            taskNumbersContainer?.addView(navView)
        }
        return ids
    }

    private fun showTaskAtIndex(index: Int, shouldScroll: Boolean = true) {
        if (index < 0 || index >= currentTasks.size) {
            Log.e(TAG_TASK_BS, "Invalid index $index for tasks size ${currentTasks.size}")
            if (currentTasks.isEmpty()){
                showEmptyState("Нет заданий для отображения.")
                taskNumbersScroll?.visibility = View.GONE
                btnPrevTask?.visibility = View.GONE
                btnNextTask?.visibility = View.GONE
            } else {
                showErrorState("Ошибка: Неверный номер задания ($index из ${currentTasks.size})")
            }
            return
        }
        showContentState()

        val task = currentTasks[index]
        currentActiveTaskIndex = index

        viewModel.clearAdditionalTextState()
        if (task.textId != null) {
            Log.d(TAG_TASK_BS, "Запрос дополнительного текста для textId: ${task.textId}")
            viewModel.requestTaskTextForCurrentTask(task.textId)
        }

        taskContentContainer?.removeAllViews()
        val taskContentView = LayoutInflater.from(context).inflate(R.layout.view_task_content_detail, taskContentContainer, false)
        
        val taskContentTextView = taskContentView.findViewById<TextView>(R.id.task_content)
        val textToggleContainer = taskContentView.findViewById<LinearLayout>(R.id.text_toggle_container)
        val showTextButton = taskContentView.findViewById<TextView>(R.id.show_text_button)
        val hideTextButton = taskContentView.findViewById<TextView>(R.id.hide_text_button)
        val additionalTextView = taskContentView.findViewById<TextView>(R.id.task_additional_text_view)
        val answerInputLayout = taskContentView.findViewById<TextInputLayout>(R.id.answer_input_layout)
        val answerInputEditText = taskContentView.findViewById<TextInputEditText>(R.id.answer_input)
        val submitAnswerButton = taskContentView.findViewById<Button>(R.id.submit_answer)
        val resultContainer = taskContentView.findViewById<LinearLayout>(R.id.task_result_container)

        val taskFullTitle = "Задание ${index + 1}: ${task.title}"
        
        taskContentTextView.post {
            val taskText = task.content
            val textToDisplay: CharSequence = if (taskText.contains("{")) {
                formatTextWithCurlyBraceHighlights(taskText)
            } else {
                taskText
            }
            taskContentTextView.text = textToDisplay
            taskContentTextView.requestLayout()
        }

        if (task.textId != null) {
            textToggleContainer.visibility = View.VISIBLE
            additionalTextView.visibility = View.GONE
            showTextButton.visibility = View.VISIBLE
            hideTextButton.visibility = View.GONE

            showTextButton.setOnClickListener {
                val currentTaskContentView = taskContentContainer?.getChildAt(0) ?: return@setOnClickListener
                val targetAdditionalTextView = currentTaskContentView.findViewById<TextView>(R.id.task_additional_text_view)
                
                showTextButton.visibility = View.GONE
                hideTextButton.visibility = View.VISIBLE

                val loadedText = viewModel.taskAdditionalText.value
                val isLoading = viewModel.taskAdditionalTextLoading.value == true

                if (loadedText != null) { 
                    val textToDisplay: CharSequence = if (loadedText.contains("{")) {
                        formatTextWithCurlyBraceHighlights(loadedText)
                    } else {
                        Html.fromHtml(loadedText, Html.FROM_HTML_MODE_LEGACY)
                    }
                    targetAdditionalTextView.text = textToDisplay
                    targetAdditionalTextView.visibility = View.VISIBLE
                } else if (!isLoading) { 
                    viewModel.requestTaskTextForCurrentTask(task.taskId.toInt())
                }
            }
            hideTextButton.setOnClickListener {
                val currentTaskContentView = taskContentContainer?.getChildAt(0) ?: return@setOnClickListener
                val targetAdditionalTextView = currentTaskContentView.findViewById<TextView>(R.id.task_additional_text_view)

                targetAdditionalTextView.visibility = View.GONE
                showTextButton.visibility = View.VISIBLE
                hideTextButton.visibility = View.GONE
            }
        } else {
            textToggleContainer.visibility = View.GONE
        }

        answerInputLayout.visibility = View.VISIBLE
        submitAnswerButton.visibility = View.VISIBLE
        answerInputEditText.setText(task.userAnswer ?: "")
        answerInputEditText.isEnabled = true

        resultContainer.visibility = View.GONE

        submitAnswerButton.setOnClickListener {
            val userAnswer = answerInputEditText.text.toString().trim()
            if (userAnswer.isNotEmpty()) {
                Log.d(TAG_TASK_BS, "Ответ пользователя для taskId ${task.taskId}: $userAnswer")
                
                viewModel.checkAnswer(task.taskId, userAnswer)
            } else {
                Toast.makeText(context, "Введите ваш ответ", Toast.LENGTH_SHORT).show()
            }
        }
        
        taskContentContainer?.addView(taskContentView)
        updateNavigationButtonsState(index)
        updateTaskNumberSelection(index)

        if (shouldScroll) {
            taskNumbersScroll?.post {
                taskNumberViewIds?.getOrNull(index)?.let { viewId ->
                    view?.findViewById<View>(viewId)?.let { numberButton ->
                        val navView = numberButton.parent as? View
                        if (navView != null) {
                            val scrollX = navView.left - (taskNumbersScroll!!.width / 2) + (navView.width / 2)
                            taskNumbersScroll?.smoothScrollTo(scrollX, 0)
                        }
                    }
                }
            }
        }
    }

    private fun updateTaskNumberSelection(selectedIndex: Int) {
        Log.d(TAG_TASK_BS, "updateTaskNumberSelection: Вызван для selectedIndex: $selectedIndex. Всего задач: ${currentTasks.size}")
        taskNumberViewIds?.let { ids ->
            if (selectedIndex < 0 || selectedIndex >= ids.size) {
                Log.e(TAG_TASK_BS, "updateTaskNumberSelection: selectedIndex ($selectedIndex) выходит за пределы ids (size: ${ids.size}). Пропускаем.")
                return
            }
            for (i in ids.indices) {
                val button = view?.findViewById<TextView>(ids[i])
                if (button == null) {
                    Log.w(TAG_TASK_BS, "updateTaskNumberSelection: Кнопка с ID ${ids[i]} не найдена для индекса $i")
                    continue
                }
                val isSelected = (i == selectedIndex)
                val task = currentTasks.getOrNull(i)

                if (isSelected) {
                    button.background = ContextCompat.getDrawable(requireContext(), R.drawable.item_task_number_bg_selector)
                    button.isSelected = true
                    button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

                } else {
                    button.isSelected = false
                    if (task != null && task.isSolved) {
                        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.item_task_number_bg_solved)
                        button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                    } else {
                        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.item_task_number_bg_selector)
                        
                        val textColorAttr = android.R.attr.textColorPrimary
                        val typedValue = android.util.TypedValue()
                        requireContext().theme.resolveAttribute(textColorAttr, typedValue, true)
                        button.setTextColor(ContextCompat.getColor(requireContext(), typedValue.resourceId))
                    }
                }
            }
        } ?: Log.w(TAG_TASK_BS, "updateTaskNumberSelection: taskNumberViewIds is null.")
    }

    private fun updateNavigationButtonsState(currentIndex: Int) {
        btnPrevTask?.isEnabled = currentIndex > 0
        btnNextTask?.isEnabled = currentIndex < currentTasks.size - 1

        btnPrevTask?.setOnClickListener { if (currentActiveTaskIndex > 0) showTaskAtIndex(currentActiveTaskIndex - 1) }
        btnNextTask?.setOnClickListener { if (currentActiveTaskIndex < currentTasks.size - 1) showTaskAtIndex(currentActiveTaskIndex + 1) }
    }

    private fun setupPaginationScrollListener() {
        taskNumbersScroll?.setOnScrollChangeListener { v, scrollX, _, _, _ ->
            val scrollView = v as? HorizontalScrollView ?: return@setOnScrollChangeListener
            val childView = scrollView.getChildAt(0)
            if (childView != null) {
                val diff = (childView.right - (scrollView.width + scrollX))
                if (diff == 0 && !isCurrentlyLoadingMoreLocal) {
                    Log.d(TAG_TASK_BS, "Достигнут конец прокрутки навигатора. Запускаем загрузку.")
                    isCurrentlyLoadingMoreLocal = true 
                    loadMoreTasks()
                }
            }
        }
    }

    private fun loadMoreTasks() {
        egeNumberForApi?.let { egeNum ->
            if (viewModel.hasMoreTasksToLoad(egeNum)) {
                Log.d(TAG_TASK_BS, "Запрос на загрузку доп.заданий для $egeNum")
                viewModel.loadMoreTasksByCategory(egeNum)
            } else {
                Log.d(TAG_TASK_BS, "Больше нет заданий для загрузки для $egeNum")
                isCurrentlyLoadingMoreLocal = false 
            }
        }
    }

    private fun updateNavigationUi() {
        if (currentTasks.isNotEmpty()) {
            taskNumbersScroll?.visibility = View.VISIBLE
            taskNavigationButtonsContainer?.visibility = View.VISIBLE
            updateTaskNumberSelection(currentActiveTaskIndex)
            updateNavigationButtonsState(currentActiveTaskIndex)
        } else {
            taskNumbersScroll?.visibility = View.GONE
            taskNavigationButtonsContainer?.visibility = View.GONE
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bottomSheet ->
                val behavior = BottomSheetBehavior.from(bottomSheet)
                bottomSheet.layoutParams.height = getWindowHeight()
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isFitToContents = false
                behavior.skipCollapsed = true
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) dismiss()
                    }
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })
            }
        }
        return dialog
    }
    
    private fun getWindowHeight(): Int {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val windowMetrics = activity?.windowManager?.currentWindowMetrics
            val insets = windowMetrics?.windowInsets?.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars())
            (windowMetrics?.bounds?.height() ?: DisplayMetrics().heightPixels) - (insets?.top ?: 0) - (insets?.bottom ?: 0)
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tasksObserver?.let { viewModel.tasks.removeObserver(it) }
        errorMessageObserver?.let { viewModel.errorMessage.removeObserver(it) }
        additionalTextObserver?.let { viewModel.taskAdditionalText.removeObserver(it) }
        additionalTextLoadingObserver?.let { viewModel.taskAdditionalTextLoading.removeObserver(it) }
        additionalTextErrorObserver?.let { viewModel.taskAdditionalTextError.removeObserver(it) }
        isLoadingMoreTasksObserver?.let { viewModel.isLoadingMoreTasks.removeObserver(it) }
        taskBottomSheetTitle = null; tasksLoadingProgress = null; tvBottomSheetErrorMessage = null; headerContainer = null; taskNumbersScroll = null; taskNumbersContainer = null; taskContentContainer = null; taskNavigationButtonsContainer = null; btnPrevTask = null; btnNextTask = null; loadMoreProgress = null;
        currentTasks.clear()
        taskNumberViewIds = null
        egeNumberForApi = null
        Log.d(TAG_TASK_BS, "onDestroyView: Все ресурсы освобождены.")
    }

    private fun extractEgeNumberFromTitle(title: String): String? {
        val pattern = "Задание №?(\\d+)".toRegex()
        val matchResult = pattern.find(title)
        return matchResult?.groups?.get(1)?.value
    }

    private fun formatTextWithCurlyBraceHighlights(text: String): SpannableString {
        Log.d(TAG_TASK_BS, "formatTextWithCurlyBraceHighlights - Input text: [$text]")
        val displayText = text.replace(Regex("\\{([^}]+)\\}"), "$1")
        Log.d(TAG_TASK_BS, "formatTextWithCurlyBraceHighlights - Display text (after replace): [$displayText]")
        val spannableString = SpannableString(displayText)
    
        val pattern = Pattern.compile("\\{([^}]+)\\}")
        val matcher = pattern.matcher(text)
    
        var cumulativeOffset = 0
        var matchesFound = 0
    
        while (matcher.find()) {
            matchesFound++
            val wordInBraces = matcher.group(0)!!
            val wordOnly = matcher.group(1)!!
            Log.d(TAG_TASK_BS, "formatTextWithCurlyBraceHighlights - Match $matchesFound: wordInBraces='${wordInBraces}', wordOnly='${wordOnly}'")
    
            val startInDisplayText = matcher.start() - cumulativeOffset
            val endInDisplayText = startInDisplayText + wordOnly.length
            Log.d(TAG_TASK_BS, "formatTextWithCurlyBraceHighlights - Calculated indices for displayText: start=$startInDisplayText, end=$endInDisplayText (cumulativeOffset=$cumulativeOffset)")
    
            if (startInDisplayText >= 0 && endInDisplayText <= spannableString.length) {
                spannableString.setSpan(StyleSpan(Typeface.BOLD), startInDisplayText, endInDisplayText, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                Log.d(TAG_TASK_BS, "formatTextWithCurlyBraceHighlights - Applied BOLD span to '${spannableString.substring(startInDisplayText, endInDisplayText)}'")
            } else {
                Log.w(TAG_TASK_BS, "formatTextWithCurlyBraceHighlights - Invalid indices, skipping span for word '$wordOnly'")
            }
            cumulativeOffset += 2
        }
        if (matchesFound == 0) {
            Log.d(TAG_TASK_BS, "formatTextWithCurlyBraceHighlights - No matches found for {} pattern.")
        }
        Log.d(TAG_TASK_BS, "formatTextWithCurlyBraceHighlights - Returning spannable: $spannableString")
        return spannableString
    }
} 