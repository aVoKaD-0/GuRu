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
import com.ruege.mobile.ui.viewmodel.ContentViewModel
import com.ruege.mobile.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import com.ruege.mobile.data.repository.ContentRepository
import androidx.core.widget.NestedScrollView

private const val ARG_CATEGORY_ID = "category_id"
private const val ARG_TITLE = "title"
private const val MAX_ATTEMPTS = 3

@AndroidEntryPoint
class TaskDisplayBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val contentViewModel: ContentViewModel by activityViewModels()

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

        this.sheetTitle?.let { title ->
            this.egeNumberForApi = extractEgeNumberFromTitle(title)
            Log.d(TAG_TASK_DISPLAY_BS, "Извлечен EGE номер: ${this.egeNumberForApi} из заголовка: $title")
        }

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
            Log.d(TAG_TASK_DISPLAY_BS, "Tasks Observer: START. Входной список: ${tasks?.size ?: "null"}. Текущий: ${currentTasks.size}. currentActiveTaskIndex: $currentActiveTaskIndex")

            val previousActiveTaskIndex = currentActiveTaskIndex
            val previousScrollX = taskNumbersScroll?.scrollX ?: 0
            var targetActiveIndexAfterUpdate = currentActiveTaskIndex

            val isPagingUpdate = currentTasks.isNotEmpty() && tasks != null && tasks.size > currentTasks.size
            val isInitialLoad = currentTasks.isEmpty() && tasks != null && tasks.isNotEmpty()
            val isEmptyOrError = tasks.isNullOrEmpty()

            if (isInitialLoad) {
                targetActiveIndexAfterUpdate = 0 // Для первой загрузки всегда 0
                Log.d(TAG_TASK_DISPLAY_BS, "Tasks Observer: Режим ПЕРВОЙ ЗАГРУЗКИ. newTargetActiveIndex = 0")
            } else if (isPagingUpdate) {
                // newTargetActiveIndex уже равен currentActiveTaskIndex, это правильно для дозагрузки
                Log.d(TAG_TASK_DISPLAY_BS, "Tasks Observer: Режим ДОЗАГРУЗКИ. newTargetActiveIndex = $currentActiveTaskIndex (сохраненный current), oldScrollX = $previousScrollX")
            } else if (isEmptyOrError) {
                targetActiveIndexAfterUpdate = 0 // Сброс, если данных нет
                Log.d(TAG_TASK_DISPLAY_BS, "Tasks Observer: Режим ПУСТО/ОШИБКА. newTargetActiveIndex = 0")
            } else {
                // Ситуация, когда список задач изменился, но это не первая загрузка и не дозагрузка (например, фильтрация - у нас ее нет, или полный рефреш)
                // Если размер списка изменился, но не увеличился, имеет смысл сбросить на 0 или оставить текущий, если он валиден
                if (tasks != null && targetActiveIndexAfterUpdate >= tasks.size) {
                    targetActiveIndexAfterUpdate = 0
                }
                Log.d(TAG_TASK_DISPLAY_BS, "Tasks Observer: Режим ОБНОВЛЕНИЯ (не первая, не дозагрузка). newTargetActiveIndex = $targetActiveIndexAfterUpdate")
            }

            currentTasks.clear()
            if (tasks != null) {
                currentTasks.addAll(tasks)
            }
            Log.d(TAG_TASK_DISPLAY_BS, "Tasks Observer: currentTasks обновлен, новый размер: ${currentTasks.size}")

            // Устанавливаем currentActiveTaskIndex, проверяя границы
            currentActiveTaskIndex = if (currentTasks.isNotEmpty() && targetActiveIndexAfterUpdate < currentTasks.size) {
                targetActiveIndexAfterUpdate
            } else {
                0
            }
            Log.d(TAG_TASK_DISPLAY_BS, "Tasks Observer: currentActiveTaskIndex установлен в $currentActiveTaskIndex")

            // После того, как currentTasks обновлен и currentActiveTaskIndex установлен:
            // Важно: setupTaskNavigation должен быть вызван ДО showTaskAtIndex, 
            // так как showTaskAtIndex вызовет updateTaskNumberSelection, который использует taskNumberViewIds
            if (currentTasks.isNotEmpty()) {
                taskNumberViewIds = setupTaskNavigation(currentTasks)
                showTaskAtIndex(currentActiveTaskIndex) // Это обновит контент и вызовет updateTaskNumberSelection

                // Логика восстановления прокрутки
                taskNumbersScroll?.post {
                    var finalScrollX = -1
                    if (isPagingUpdate && currentActiveTaskIndex == previousActiveTaskIndex) {
                        // Дозагрузка, и активный элемент не изменился. Пытаемся восстановить старую прокрутку.
                        finalScrollX = previousScrollX
                        Log.d(TAG_TASK_DISPLAY_BS, "Tasks Observer (Paging, same index $currentActiveTaskIndex): Restoring previousScrollX = $finalScrollX")
                    } else {
                        // Либо не дозагрузка, либо активный индекс изменился.
                        // Прокручиваем к текущему активному элементу, чтобы он был слева.
                        taskNumberViewIds?.getOrNull(currentActiveTaskIndex)?.let { viewId ->
                            view?.findViewById<View>(viewId)?.let {
                                finalScrollX = it.left
                                Log.d(TAG_TASK_DISPLAY_BS, "Tasks Observer (Index changed or not paging): Scrolling to left of new active index $currentActiveTaskIndex. Target scrollX = $finalScrollX")
                            }
                        }
                    }
                    if (finalScrollX != -1) {
                        taskNumbersScroll?.smoothScrollTo(finalScrollX, 0)
                    }
                }
            } else {
                Log.d(TAG_TASK_DISPLAY_BS, "Tasks Observer: Список задач ПУСТ после обновления. Показываем пустое состояние.")
                taskNumberViewIds = setupTaskNavigation(emptyList()) // Очищаем навигацию
                val errorMessage = if (egeNumberForApi != null) {
                    contentViewModel.errorMessage.value ?: "Нет доступных заданий для категории $egeNumberForApi."
                } else {
                    contentViewModel.errorMessage.value ?: "Задания не найдены."
                }
                // Используем showErrorState или showEmptyState в зависимости от наличия ошибки от ViewModel
                if (contentViewModel.errorMessage.value != null && ContentRepository.NO_DATA_AND_NETWORK_ISSUE_FLAG != contentViewModel.errorMessage.value) {
                     showErrorState(errorMessage)
                } else if (ContentRepository.NO_DATA_AND_NETWORK_ISSUE_FLAG == contentViewModel.errorMessage.value) {
                    showErrorState("Нет данных или проблема с сетью. Проверьте подключение.")
                }
                else {
                    showEmptyState(errorMessage)
                }
            }
            updateNavigationUi() // Обновляем состояние кнопок Вперед/Назад
            Log.d(TAG_TASK_DISPLAY_BS, "Tasks Observer: END.")
        }

        contentViewModel.tasks.observe(viewLifecycleOwner, tasksObserver!!)

        if (egeNumberForApi != null) {
            val existingTasks = contentViewModel.tasks.value
            if (existingTasks == null || existingTasks.isEmpty() || !isViewCreatedOnce) {
                Log.d(TAG_TASK_DISPLAY_BS, "Список задач пуст, отсутствует или view не создавался. Загружаем для egeNumber: $egeNumberForApi")
                showLoadingState()
                contentViewModel.loadTasksByCategory(egeNumberForApi!!)
            } else {
                Log.d(TAG_TASK_DISPLAY_BS, "Используем существующие задачи (${existingTasks.size} шт.) для egeNumber: $egeNumberForApi")
                if (currentTasks.isNotEmpty()) {
                    Log.d(TAG_TASK_DISPLAY_BS, "Calling setupTaskNavigation for existing tasks in currentTasks.")
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
            Log.e(TAG_TASK_DISPLAY_BS, "egeNumberForApi is null at init. Cannot load tasks initially or use existing.")
            showEmptyState("Не указан номер задания для отображения.")
            updateNavigationUi()
        }
    }

    private fun setupGeneralObservers() {
        errorMessageObserver = Observer { errorMessageValue ->
            if (!isAdded || view == null) return@Observer
            if (currentTasks.isEmpty() && tvBottomSheetErrorMessage?.visibility == View.VISIBLE) {
                val messageToShow = when {
                    ContentRepository.NO_DATA_AND_NETWORK_ISSUE_FLAG == errorMessageValue -> "Ничего не найдено. Проверьте подключение к интернету."
                    !errorMessageValue.isNullOrEmpty() -> errorMessageValue
                    else -> "Произошла ошибка."
                }
                tvBottomSheetErrorMessage?.text = messageToShow
            }
        }
        
        contentViewModel.errorMessage.observe(viewLifecycleOwner, errorMessageObserver!!)
        setupAdditionalTextObservers()
        setupAnswerCheckObserver()

        isLoadingMoreTasksObserver = Observer { isLoading ->
            if (!isAdded || view == null) return@Observer
            loadMoreProgress?.visibility = if (isLoading == true) View.VISIBLE else View.GONE
            if (isLoading == false) {
                isCurrentlyLoadingMoreLocal = false
            }
        }
        contentViewModel.isLoadingMoreTasks.observe(viewLifecycleOwner, isLoadingMoreTasksObserver!!)
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

            val isLoading = contentViewModel.taskAdditionalTextLoading.value == true
            val hasError = !contentViewModel.taskAdditionalTextError.value.isNullOrEmpty()

            if (isLoading || hasError) {
                targetAdditionalTextView?.visibility = View.GONE
                return@Observer
            }

            targetAdditionalTextView?.let { textView ->
                if (text != null) {
                    textView.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
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

        contentViewModel.taskAdditionalText.observe(viewLifecycleOwner, additionalTextObserver!!)
        contentViewModel.taskAdditionalTextLoading.observe(viewLifecycleOwner, additionalTextLoadingObserver!!)
        contentViewModel.taskAdditionalTextError.observe(viewLifecycleOwner, additionalTextErrorObserver!!)
    }
    
    private fun setupAnswerCheckObserver() {
        contentViewModel.answerCheckResultLiveData.observe(viewLifecycleOwner, Observer { result ->
            if (!isAdded || view == null || result == null) return@Observer
            Log.d(TAG_TASK_DISPLAY_BS, "AnswerCheckResult LiveData: Получен результат для taskId: ${result.taskId}")

            val taskIndex = currentTasks.indexOfFirst { it.taskId == result.taskId }
            if (taskIndex == -1) {
                Log.e(TAG_TASK_DISPLAY_BS, "Не найдена задача с taskId: ${result.taskId} в currentTasks")
                return@Observer
            }

            val task = currentTasks[taskIndex]

            // Обновляем состояние задачи
            task.userAnswer = result.userAnswer
            task.attemptsMade++
            task.isCorrect = result.isCorrect
            task.correctAnswer = result.correctAnswer 
            task.explanation = result.explanation // Сохраняем объяснение в TaskItem
            task.scoreAchieved = result.pointsAwarded

            // Задание считается "решенным" (пройденным), как только сделана хотя бы одна попытка.
            // Так как attemptsMade инкрементируется перед этой строкой, оно всегда будет >= 1 здесь.
            task.isSolved = true 

            // Обновляем прогресс пользователя для ЛЮБОГО ответа
            if (egeNumberForApi != null) {
                contentViewModel.updateUserProgressForTask(task, egeNumberForApi!!)
                Log.d(TAG_TASK_DISPLAY_BS, "Прогресс обновлен для задачи ${task.taskId} (любой ответ)")
            } else {
                Log.e(TAG_TASK_DISPLAY_BS, "egeNumberForApi is null, не могу обновить прогресс для задачи ${task.taskId}")
            }

            // Обновляем UI текущей отображаемой задачи, если это она
            if (taskIndex == currentActiveTaskIndex) {
                // СНАЧАЛА обновляем UI панели номеров, чтобы отразить статус isSolved
                updateTaskNumberSelection(currentActiveTaskIndex)
                // Затем показываем детальный результат ответа внутри контента задания
                displayAnswerResult(task, result) 
            }
        })
    }

    private fun displayAnswerResult(task: TaskItem, result: com.ruege.mobile.model.AnswerCheckResult) {
        Log.d(TAG_TASK_DISPLAY_BS, "displayAnswerResult: task.isCorrect=${task.isCorrect}, task.explanation='${task.explanation}', result.explanation='${result.explanation}', result.correctAnswer='${result.correctAnswer}', result.userAnswer='${result.userAnswer}', result.pointsAwarded=${result.pointsAwarded}")
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
        
        if (task.isCorrect == false) { // Показываем правильный ответ только если ответ был неверный
             val correctAnswerDisplay = result.correctAnswer ?: "Н/Д"
             shortInfoText += "\nВаш ответ: ${result.userAnswer}\nПравильный ответ: $correctAnswerDisplay"
        } else if (task.isCorrect == true && result.userAnswer != result.correctAnswer && result.userAnswer.equals(result.correctAnswer, ignoreCase = true)) {
            // Для случаев, когда ответ верный, но отображаемый "правильный ответ" (например, с другим регистром) полезен
             val correctAnswerDisplay = result.correctAnswer ?: "Н/Д"
            shortInfoText += "\nЭталонный ответ: $correctAnswerDisplay"
        }

        resultShortInfoTextView.text = shortInfoText.trim()

        // Используем result.explanation, так как он содержит актуальное объяснение от сервера
        if (!result.explanation.isNullOrBlank()) { 
            Log.d(TAG_TASK_DISPLAY_BS, "Explanation IS NOT BLANK (from result), showing: '${result.explanation}'")
            showExplanationButton.visibility = View.VISIBLE
            taskExplanationTextView.text = Html.fromHtml(result.explanation, Html.FROM_HTML_MODE_LEGACY)
            taskExplanationTextView.visibility = View.GONE // Изначально скрыто
            showExplanationButton.text = "Показать объяснение"
            showExplanationButton.setOnClickListener {button ->
                val isCurrentlyVisible = taskExplanationTextView.visibility == View.VISIBLE
                taskExplanationTextView.visibility = if (isCurrentlyVisible) View.GONE else View.VISIBLE
                (button as Button).text = if (isCurrentlyVisible) "Показать объяснение" else "Скрыть объяснение"
            }
        } else {
            Log.d(TAG_TASK_DISPLAY_BS, "Explanation IS BLANK or NULL (from result). Raw explanation value was: '${result.explanation}'")
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
        Log.d(TAG_TASK_DISPLAY_BS, "Displaying Loading State")
    }

    private fun showErrorState(message: String) {
        tasksLoadingProgress?.visibility = View.GONE
        tvBottomSheetErrorMessage?.text = message
        tvBottomSheetErrorMessage?.visibility = View.VISIBLE
        headerContainer?.visibility = View.GONE
        taskContentContainer?.visibility = View.GONE
        taskNavigationButtonsContainer?.visibility = View.GONE
        Log.d(TAG_TASK_DISPLAY_BS, "Displaying Error State: $message")
    }

    private fun showEmptyState(message: String = "Нет данных для отображения") {
        tasksLoadingProgress?.visibility = View.GONE
        tvBottomSheetErrorMessage?.text = message
        tvBottomSheetErrorMessage?.visibility = View.VISIBLE
        headerContainer?.visibility = View.GONE
        taskContentContainer?.visibility = View.GONE
        taskNavigationButtonsContainer?.visibility = View.GONE
        Log.d(TAG_TASK_DISPLAY_BS, "Displaying Empty State: $message")
    }

    private fun showContentState() {
        tasksLoadingProgress?.visibility = View.GONE
        tvBottomSheetErrorMessage?.visibility = View.GONE
        headerContainer?.visibility = View.VISIBLE
        taskContentContainer?.visibility = View.VISIBLE
        taskNavigationButtonsContainer?.visibility = View.VISIBLE
        Log.d(TAG_TASK_DISPLAY_BS, "Displaying Content State. headerContainer visible: ${headerContainer?.visibility == View.VISIBLE}, taskNavigationButtonsContainer visible: ${taskNavigationButtonsContainer?.visibility == View.VISIBLE}")
    }

    private fun setupTaskNavigation(tasks: List<TaskItem>): IntArray {
        Log.d(TAG_TASK_DISPLAY_BS, "setupTaskNavigation called with ${tasks.size} tasks. CurrentActiveTaskIndex перед перестроением: $currentActiveTaskIndex")
        taskNumbersContainer?.removeAllViews()
        if (tasks.isEmpty()) {
            Log.w(TAG_TASK_DISPLAY_BS, "setupTaskNavigation: tasks list is empty, hiding navigation scroll.")
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

    private fun showTaskAtIndex(index: Int) {
        if (index < 0 || index >= currentTasks.size) {
            Log.e(TAG_TASK_DISPLAY_BS, "Invalid index $index for tasks size ${currentTasks.size}")
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

        contentViewModel.clearAdditionalTextState()
        if (task.textId != null) {
            Log.d(TAG_TASK_DISPLAY_BS, "Запрос дополнительного текста для textId: ${task.textId}")
            contentViewModel.requestTaskTextForCurrentTask(task.taskId)
        }

        taskContentContainer?.removeAllViews()
        val taskContentView = LayoutInflater.from(context).inflate(R.layout.view_task_content_detail, taskContentContainer, false)
        
        // Диагностика с фонами
        // taskContentContainer?.setBackgroundColor(android.graphics.Color.RED) // Родительский контейнер в основном макете
        // taskContentView.setBackgroundColor(android.graphics.Color.BLUE)     // Корневой элемент view_task_content_detail.xml

//        val taskScrollView = taskContentView.findViewById<NestedScrollView>(R.id.task_scroll_view)
        // taskScrollView?.setBackgroundColor(android.graphics.Color.GREEN)
        
        val taskContentTextView = taskContentView.findViewById<TextView>(R.id.task_content)
        // taskContentTextView?.setBackgroundColor(android.graphics.Color.YELLOW)
        val textToggleContainer = taskContentView.findViewById<LinearLayout>(R.id.text_toggle_container)
        val showTextButton = taskContentView.findViewById<TextView>(R.id.show_text_button)
        val hideTextButton = taskContentView.findViewById<TextView>(R.id.hide_text_button)
        val additionalTextView = taskContentView.findViewById<TextView>(R.id.task_additional_text_view)
        val answerInputLayout = taskContentView.findViewById<TextInputLayout>(R.id.answer_input_layout)
        val answerInputEditText = taskContentView.findViewById<TextInputEditText>(R.id.answer_input)
        val submitAnswerButton = taskContentView.findViewById<Button>(R.id.submit_answer)
        val resultContainer = taskContentView.findViewById<LinearLayout>(R.id.task_result_container)
        // val resultStatusTextView = taskContentView.findViewById<TextView>(R.id.result_status) // эти поля используются в displayAnswerResult
        // val resultShortInfoTextView = taskContentView.findViewById<TextView>(R.id.result_short_info)
        // val showExplanationButton = taskContentView.findViewById<Button>(R.id.show_explanation_button)
        // val taskExplanationTextView = taskContentView.findViewById<TextView>(R.id.task_explanation)

        val taskFullTitle = "Задание ${index + 1}: ${task.title}"
        // val formattedTitle = "<b>${Html.escapeHtml(taskFullTitle)}</b>"
        
        taskContentTextView.post {
            // Упрощенная установка текста для теста
            taskContentTextView.text = task.content
            taskContentTextView.requestLayout()
        }

        if (task.textId != null) {
            textToggleContainer.visibility = View.VISIBLE
            additionalTextView.visibility = View.GONE // Возвращаем исходное состояние
            showTextButton.visibility = View.VISIBLE
            hideTextButton.visibility = View.GONE

            showTextButton.setOnClickListener {
                val currentTaskContentView = taskContentContainer?.getChildAt(0) ?: return@setOnClickListener
                val targetAdditionalTextView = currentTaskContentView.findViewById<TextView>(R.id.task_additional_text_view)
                
                showTextButton.visibility = View.GONE
                hideTextButton.visibility = View.VISIBLE

                val loadedText = contentViewModel.taskAdditionalText.value
                val isLoading = contentViewModel.taskAdditionalTextLoading.value == true

                if (loadedText != null) { 
                    targetAdditionalTextView.text = Html.fromHtml(loadedText, Html.FROM_HTML_MODE_LEGACY)
                    targetAdditionalTextView.visibility = View.VISIBLE
                } else if (!isLoading) { 
                    contentViewModel.requestTaskTextForCurrentTask(task.taskId) 
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

        // Показываем поле ввода и кнопку всегда, текст ответа берем из task.userAnswer, если есть
        answerInputLayout.visibility = View.VISIBLE
        submitAnswerButton.visibility = View.VISIBLE
        answerInputEditText.setText(task.userAnswer ?: "") // Если пользователь ранее отвечал, покажем его ответ
        answerInputEditText.isEnabled = true // Поле ввода всегда активно

        // Блок результата по умолчанию скрыт при показе новой задачи
        resultContainer.visibility = View.GONE

        submitAnswerButton.setOnClickListener {
            val userAnswer = answerInputEditText.text.toString().trim()
            if (userAnswer.isNotEmpty()) {
                Log.d(TAG_TASK_DISPLAY_BS, "Ответ пользователя для taskId ${task.taskId}: $userAnswer")
                contentViewModel.checkAnswer(task.taskId, userAnswer)
                // После нажатия кнопки, результат будет показан через displayAnswerResult,
                // который вызовется из Observer'a на answerCheckResultLiveData
            } else {
                Toast.makeText(context, "Введите ваш ответ", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Старый блок if (task.isSolved) { ... } для показа результата здесь больше не нужен,
        // так как displayAnswerResult() будет вызван наблюдателем LiveData.

        taskContentContainer?.addView(taskContentView)
        updateNavigationButtonsState(index)
        updateTaskNumberSelection(index)
    }

    private fun updateTaskNumberSelection(selectedIndex: Int) {
        Log.d(TAG_TASK_DISPLAY_BS, "updateTaskNumberSelection: Вызван для selectedIndex: $selectedIndex. Всего задач: ${currentTasks.size}")
        taskNumberViewIds?.let { ids ->
            if (selectedIndex < 0 || selectedIndex >= ids.size) {
                Log.e(TAG_TASK_DISPLAY_BS, "updateTaskNumberSelection: selectedIndex ($selectedIndex) выходит за пределы ids (size: ${ids.size}). Пропускаем.")
                return
            }
            for (i in ids.indices) {
                val button = view?.findViewById<TextView>(ids[i])
                if (button == null) {
                    Log.w(TAG_TASK_DISPLAY_BS, "updateTaskNumberSelection: Кнопка с ID ${ids[i]} не найдена для индекса $i")
                    continue
                }
                val isSelected = (i == selectedIndex)
                val task = currentTasks.getOrNull(i)

                if (isSelected) {
                    // --- ВЫБРАННАЯ ЗАДАЧА ---
                    // Сначала убедимся, что фоном является селектор
                    button.background = ContextCompat.getDrawable(requireContext(), R.drawable.item_task_number_bg_selector)
                    button.isSelected = true // Активируем селектор для состояния selected
                    button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white)) // Текст: белый

                    // Прокрутка к выбранному элементу
                    button.post {
                        taskNumbersScroll?.smoothScrollTo(button.left, 0)
                        Log.d(TAG_TASK_DISPLAY_BS, "updateTaskNumberSelection (post) for index $i: Scrolling to button.left (${button.left})")
                    }
                } else {
                    // --- НЕВЫБРАННАЯ ЗАДАЧА ---
                    button.isSelected = false // Деактивируем селектор для состояния selected
                    if (task != null && task.isSolved) {
                        // РЕШЕНА и НЕ ВЫБРАНА:
                        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.item_task_number_bg_solved) // Желтый круг
                        button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black)) // Текст: черный (на желтом фоне)
                    } else {
                        // НЕ РЕШЕНА и НЕ ВЫБРАНА:
                        button.background = ContextCompat.getDrawable(requireContext(), R.drawable.item_task_number_bg_selector) // Обычный селектор (прозрачный круг с обводкой)
                        
                        // Цвет текста: используем атрибут темы для адаптивности
                        val textColorAttr = android.R.attr.textColorPrimary
                        val typedValue = android.util.TypedValue()
                        requireContext().theme.resolveAttribute(textColorAttr, typedValue, true)
                        button.setTextColor(ContextCompat.getColor(requireContext(), typedValue.resourceId))
                    }
                }
            }
        } ?: Log.w(TAG_TASK_DISPLAY_BS, "updateTaskNumberSelection: taskNumberViewIds is null.")
    }

    private fun updateNavigationButtonsState(currentIndex: Int) {
        btnPrevTask?.isEnabled = currentIndex > 0
        btnNextTask?.isEnabled = currentIndex < currentTasks.size - 1

        btnPrevTask?.setOnClickListener { if (currentActiveTaskIndex > 0) showTaskAtIndex(currentActiveTaskIndex - 1) }
        btnNextTask?.setOnClickListener { if (currentActiveTaskIndex < currentTasks.size - 1) showTaskAtIndex(currentActiveTaskIndex + 1) }
    }

    private fun setupPaginationScrollListener() {
        taskNumbersScroll?.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            val scrollView = v as? HorizontalScrollView ?: return@setOnScrollChangeListener
            val childView = scrollView.getChildAt(0) ?: return@setOnScrollChangeListener

            // Проверяем, достигнут ли конец прокрутки (например, 90% от полной ширины)
            val scrollPercentage = (scrollX.toDouble() / (childView.width - scrollView.width).toDouble()) * 100
            // Log.d(TAG_TASK_DISPLAY_BS, "ScrollX: $scrollX, ChildWidth: ${childView.width}, ScrollViewWidth: ${scrollView.width}, Percentage: $scrollPercentage")


            if (scrollX >= (childView.width - scrollView.width) * 0.9 && egeNumberForApi != null && !isCurrentlyLoadingMoreLocal) {
                 // Используем hasMoreTasksToLoad(id) если это метод, или hasMoreTasksToLoad.value?.get(id) если LiveData<Map>
                 // Предположим, что hasMoreTasksToLoad(id) - это метод, возвращающий Boolean
                val hasMore = contentViewModel.hasMoreTasksToLoad(egeNumberForApi!!)
                Log.d(TAG_TASK_DISPLAY_BS, "Scroll near end. HasMore: $hasMore, IsLoadingMoreLocal: $isCurrentlyLoadingMoreLocal, isLoadingMoreVM: ${contentViewModel.isLoadingMoreTasks.value}")


                if (hasMore && contentViewModel.isLoadingMoreTasks.value != true) { // Добавили проверку isLoadingMoreTasks из VM
                    Log.d(TAG_TASK_DISPLAY_BS, "Загрузка дополнительных задач для EGE номера: $egeNumberForApi")
                    isCurrentlyLoadingMoreLocal = true
                    // loadMoreProgress?.visibility = View.VISIBLE // Управляется Observer'ом isLoadingMoreTasks
                    contentViewModel.loadMoreTasksByCategory(egeNumberForApi!!) // Предполагаем, что этот метод существует и правильно работает
                }
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
        contentViewModel.tasks.removeObserver(tasksObserver!!)
        contentViewModel.errorMessage.removeObserver(errorMessageObserver!!)
        contentViewModel.taskAdditionalText.removeObserver(additionalTextObserver!!)
        contentViewModel.taskAdditionalTextLoading.removeObserver(additionalTextLoadingObserver!!)
        contentViewModel.taskAdditionalTextError.removeObserver(additionalTextErrorObserver!!)
        isLoadingMoreTasksObserver?.let { contentViewModel.isLoadingMoreTasks.removeObserver(it) }
        taskBottomSheetTitle = null; tasksLoadingProgress = null; tvBottomSheetErrorMessage = null; headerContainer = null; taskNumbersScroll = null; taskNumbersContainer = null; taskContentContainer = null; taskNavigationButtonsContainer = null; btnPrevTask = null; btnNextTask = null; loadMoreProgress = null;
        currentTasks.clear()
        taskNumberViewIds = null
        egeNumberForApi = null
        Log.d(TAG_TASK_DISPLAY_BS, "onDestroyView: Все ресурсы освобождены.")
    }

    private fun extractEgeNumberFromTitle(title: String): String? {
        val regex = Regex("(?i)задание\\s+(\\d+)") 
        val matchResult = regex.find(title)
        return matchResult?.groups?.get(1)?.value
    }

    companion object {
        const val TAG_TASK_DISPLAY_BS = "TaskDisplayBottomSheet"
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
} 