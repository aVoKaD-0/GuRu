package com.ruege.mobile.ui.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.text.Html
import android.util.DisplayMetrics
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.R
import com.ruege.mobile.model.TaskItem
import com.ruege.mobile.ui.viewmodel.TasksViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.ruege.mobile.data.repository.TasksRepository
import com.ruege.mobile.databinding.TaskViewWithNavigationBinding
import com.ruege.mobile.ui.bottomsheet.helper.TaskAnswerHelper
import com.ruege.mobile.ui.bottomsheet.helper.TaskDisplayHelper
import com.ruege.mobile.ui.bottomsheet.helper.TaskNavigationHelper
import com.ruege.mobile.ui.bottomsheet.helper.TaskUIHelper

private const val ARG_CATEGORY_ID = "category_id"
private const val ARG_TITLE = "title"

@AndroidEntryPoint
class TaskDisplayBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "TaskDisplayBottomSheet"

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

    private var _binding: TaskViewWithNavigationBinding? = null
    private val binding get() = _binding!!

    private var categoryId: String? = null
    private var sheetTitle: String? = null
    
    private var taskUIHelper: TaskUIHelper? = null
    private var taskAnswerHelper: TaskAnswerHelper? = null
    private var taskNavigationHelper: TaskNavigationHelper? = null
    private var taskDisplayHelper: TaskDisplayHelper? = null
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
    ): View {
        _binding = TaskViewWithNavigationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.taskBottomSheetTitle.text = sheetTitle ?: "Задания"

        taskUIHelper = TaskUIHelper(binding)
        taskAnswerHelper = TaskAnswerHelper(requireContext(), binding)
        taskNavigationHelper = TaskNavigationHelper(requireContext(), binding, requireView()) { index -> showTaskAtIndex(index) }
        taskDisplayHelper = TaskDisplayHelper(
            requireContext(),
            binding,
            requireView(),
            viewModel,
            taskUIHelper!!,
            taskNavigationHelper!!,
            { currentTasks },
            { taskNumberViewIds },
            { currentActiveTaskIndex },
            { index -> currentActiveTaskIndex = index }
        )
        setInitialUiState()
        initObserversAndLoadInitialData()
        setupPaginationScrollListener()
    }

    private fun setInitialUiState() {
        taskUIHelper?.showLoadingState()
    }

    private fun initObserversAndLoadInitialData() {
        setupGeneralObservers()

        tasksObserver = Observer { tasks ->
            Timber.d("Tasks Observer: START. Received \${tasks?.size ?: null} tasks. Current task count: \${currentTasks.size}.")

            val previousTaskId = if (currentTasks.isNotEmpty() && currentActiveTaskIndex < currentTasks.size) {
                currentTasks[currentActiveTaskIndex].taskId
            } else {
                null
            }
            val previousScrollX = binding.taskNumbersScroll.scrollX
            val wasPagingUpdate = tasks != null && currentTasks.isNotEmpty() && tasks.size > currentTasks.size

            currentTasks.clear()
            if (tasks.isNullOrEmpty()) {
                val errorMessage = viewModel.errorMessage.value ?: "Нет доступных заданий для этой категории."
                if (viewModel.errorMessage.value?.contains(TasksRepository.NO_DATA_AND_NETWORK_ISSUE_FLAG) == true) {
                    taskUIHelper?.showErrorState("Нет данных или проблема с сетью. Проверьте подключение.")
                } else {
                    taskUIHelper?.showEmptyState(errorMessage)
                }
                taskNavigationHelper?.updateNavigationUi(currentTasks, currentActiveTaskIndex, taskNumberViewIds)
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

            taskNumberViewIds = taskNavigationHelper?.setupTaskNavigation(currentTasks)
            showTaskAtIndex(currentActiveTaskIndex, !wasPagingUpdate)

            binding.taskNumbersScroll.post {
                if (wasPagingUpdate) {
                    Timber.d("Paging update. Restoring scroll position to \$previousScrollX")
                    binding.taskNumbersScroll.smoothScrollTo(previousScrollX, 0)
                }
            }
            taskNavigationHelper?.updateNavigationUi(currentTasks, currentActiveTaskIndex, taskNumberViewIds)
            Timber.d("Tasks Observer: END. Active index: \$currentActiveTaskIndex.")
        }

        viewModel.tasks.observe(viewLifecycleOwner, tasksObserver!!)

        val egeNumber = categoryId?.removePrefix("task_group_")
        if (egeNumber != null) {
            this.egeNumberForApi = egeNumber
            val existingTasks = viewModel.tasks.value
            if (existingTasks == null || existingTasks.isEmpty() || !isViewCreatedOnce) {
                Timber.d("Список задач пуст, отсутствует или view не создавался. Загружаем для egeNumber: \$egeNumber")
                taskUIHelper?.showLoadingState()
                viewModel.loadTasksByCategory(egeNumber)
            } else {
                Timber.d("Используем существующие задачи (\${existingTasks.size} шт.) для egeNumber: \$egeNumber")
                if (currentTasks.isNotEmpty()) {
                    Timber.d("Calling setupTaskNavigation for existing tasks in currentTasks.")
                    taskNumberViewIds = taskNavigationHelper?.setupTaskNavigation(currentTasks)
                    showTaskAtIndex(currentActiveTaskIndex)
                } else if (existingTasks.isNotEmpty()) {
                    currentTasks.addAll(existingTasks)
                    currentActiveTaskIndex = 0
                    taskNumberViewIds = taskNavigationHelper?.setupTaskNavigation(currentTasks)
                    showTaskAtIndex(currentActiveTaskIndex)
                } else {
                    taskUIHelper?.showEmptyState("Нет сохраненных заданий.")
                    taskNavigationHelper?.updateNavigationUi(currentTasks, currentActiveTaskIndex, taskNumberViewIds)
                }
            }
        } else {
            Timber.d("egeNumber is null at init (derived from categoryId). Cannot load tasks initially or use existing.")
            taskUIHelper?.showEmptyState("Не указан номер задания для отображения.")
            taskNavigationHelper?.updateNavigationUi(currentTasks, currentActiveTaskIndex, taskNumberViewIds)
        }
    }

    private fun setupGeneralObservers() {
        errorMessageObserver = Observer { errorMessageValue ->
            if (!isAdded || view == null) return@Observer
            if (currentTasks.isEmpty() && binding.tvBottomSheetErrorMessage.visibility == View.VISIBLE) {
                val messageToShow = when {
                    TasksRepository.NO_DATA_AND_NETWORK_ISSUE_FLAG == errorMessageValue -> "Ничего не найдено. Проверьте подключение к интернету."
                    !errorMessageValue.isNullOrEmpty() -> errorMessageValue
                    else -> "Произошла ошибка."
                }
                binding.tvBottomSheetErrorMessage.text = messageToShow
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner, errorMessageObserver!!)
        setupAdditionalTextObservers()
        setupAnswerCheckObserver()

        isLoadingMoreTasksObserver = Observer { isLoading ->
            if (!isAdded || view == null) return@Observer
            binding.loadMoreProgress.visibility = if (isLoading == true) View.VISIBLE else View.GONE
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

            val currentTaskContentView = binding.taskContentContainer.getChildAt(0) ?: return@Observer
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
                        taskDisplayHelper!!.formatTextWithCurlyBraceHighlights(text)
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

            binding.taskContentContainer.findViewById<ProgressBar>(R.id.task_additional_text_loading)?.visibility = if (isLoading == true) View.VISIBLE else View.GONE
            if (isLoading == true) {
                binding.taskContentContainer.findViewById<TextView>(R.id.task_additional_text_view)?.visibility = View.GONE
                binding.taskContentContainer.findViewById<TextView>(R.id.task_additional_text_error)?.visibility = View.GONE
            }
        }

        additionalTextErrorObserver = Observer { error ->
            if (!isAdded || view == null || currentTasks.isEmpty() || currentActiveTaskIndex >= currentTasks.size) return@Observer
            val task = currentTasks[currentActiveTaskIndex]
            if (task.textId == null) return@Observer

            binding.taskContentContainer.findViewById<TextView>(R.id.task_additional_text_error)?.let {
                if (!error.isNullOrEmpty()) {
                    it.text = error
                    it.visibility = View.VISIBLE
                    binding.taskContentContainer.findViewById<TextView>(R.id.task_additional_text_view)?.visibility = View.GONE
                    binding.taskContentContainer.findViewById<ProgressBar>(R.id.task_additional_text_loading)?.visibility = View.GONE
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
            Timber.d("AnswerCheckResult LiveData: Получен результат для taskId: ${result.taskId}")

            val taskIndex = currentTasks.indexOfFirst { it.taskId == result.taskId }
            if (taskIndex == -1) {
                Timber.d("Не найдена задача с taskId: ${result.taskId} в currentTasks")
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
                Timber.d("Прогресс обновлен для задачи ${task.taskId} (любой ответ)")
            } else {
                Timber.d("egeNumber is null, не могу обновить прогресс для задачи ${task.taskId}")
            }

            if (taskIndex == currentActiveTaskIndex) {
                taskNavigationHelper?.updateTaskNumberSelection(currentActiveTaskIndex, taskNumberViewIds, currentTasks)
                taskAnswerHelper?.displayAnswerResult(task, result)
            }
        })
    }

    private fun showLoadingState() {
        binding.tasksLoadingProgress.visibility = View.VISIBLE
        binding.tvBottomSheetErrorMessage.visibility = View.GONE
        binding.headerContainer.visibility = View.GONE
        binding.taskContentContainer.visibility = View.GONE
        binding.taskNavigationButtonsContainer.visibility = View.GONE
        Timber.d("Displaying Loading State")
    }

    private fun showErrorState(message: String) {
        binding.tasksLoadingProgress.visibility = View.GONE
        binding.tvBottomSheetErrorMessage.text = message
        binding.tvBottomSheetErrorMessage.visibility = View.VISIBLE
        binding.headerContainer.visibility = View.GONE
        binding.taskContentContainer.visibility = View.GONE
        binding.taskNavigationButtonsContainer.visibility = View.GONE
        Timber.d("Displaying Error State: $message")
    }

    private fun showEmptyState(message: String = "Нет данных для отображения") {
        binding.tasksLoadingProgress.visibility = View.GONE
        binding.tvBottomSheetErrorMessage.text = message
        binding.tvBottomSheetErrorMessage.visibility = View.VISIBLE
        binding.headerContainer.visibility = View.GONE
        binding.taskContentContainer.visibility = View.GONE
        binding.taskNavigationButtonsContainer.visibility = View.GONE
        Timber.d("Displaying Empty State: $message")
    }

    private fun showContentState() {
        binding.tasksLoadingProgress.visibility = View.GONE
        binding.tvBottomSheetErrorMessage.visibility = View.GONE
        binding.headerContainer.visibility = View.VISIBLE
        binding.taskContentContainer.visibility = View.VISIBLE
        binding.taskNavigationButtonsContainer.visibility = View.VISIBLE
        Timber.d("Displaying Content State. headerContainer visible: ${binding.headerContainer.visibility == View.VISIBLE}, taskNavigationButtonsContainer visible: ${binding.taskNavigationButtonsContainer.visibility == View.VISIBLE}")
    }

    private fun showTaskAtIndex(index: Int, shouldScroll: Boolean = true) {
        taskDisplayHelper?.showTaskAtIndex(index, shouldScroll)
    }

    private fun setupPaginationScrollListener() {
        binding.taskNumbersScroll.setOnScrollChangeListener { v, scrollX, _, _, _ ->
            val scrollView = v as? HorizontalScrollView ?: return@setOnScrollChangeListener
            val childView = scrollView.getChildAt(0)
            if (childView != null) {
                val diff = (childView.right - (scrollView.width + scrollX))
                if (diff == 0 && !isCurrentlyLoadingMoreLocal) {
                    Timber.d("Достигнут конец прокрутки навигатора. Запускаем загрузку.")
                    isCurrentlyLoadingMoreLocal = true 
                    loadMoreTasks()
                }
            }
        }
    }

    private fun loadMoreTasks() {
        egeNumberForApi?.let { egeNum ->
            if (viewModel.hasMoreTasksToLoad(egeNum)) {
                Timber.d("Запрос на загрузку доп.заданий для $egeNum")
                viewModel.loadMoreTasksByCategory(egeNum)
            } else {
                Timber.d("Больше нет заданий для загрузки для $egeNum")
                isCurrentlyLoadingMoreLocal = false 
            }
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
        _binding = null
        currentTasks.clear()
        taskNumberViewIds = null
        egeNumberForApi = null
        taskUIHelper = null
        taskAnswerHelper = null
        taskNavigationHelper = null
        taskDisplayHelper = null
        Timber.d("onDestroyView: Все ресурсы освобождены.")
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

    private fun extractEgeNumberFromTitle(title: String): String? {
        val pattern = "Задание №?(\\d+)".toRegex()
        val matchResult = pattern.find(title)
        return matchResult?.groups?.get(1)?.value
    }
} 