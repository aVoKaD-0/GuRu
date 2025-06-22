package com.ruege.mobile.ui.bottomsheet

import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.R
import com.ruege.mobile.data.local.entity.VariantEntity
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.data.local.entity.VariantSharedTextEntity
import com.ruege.mobile.data.local.entity.VariantTaskEntity
import com.ruege.mobile.data.local.entity.UserVariantTaskAnswerEntity
import com.ruege.mobile.utilss.Resource
import com.ruege.mobile.ui.viewmodel.VariantViewModel
import com.ruege.mobile.ui.viewmodel.PracticeViewModel
import com.ruege.mobile.model.VariantResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import android.content.DialogInterface

private const val ARG_VARIANT_ID = "variant_id"
private const val ARG_VARIANT_TITLE = "variant_title"
private const val TIMER_DURATION_MS = (3 * 60 * 60 * 1000) + (55 * 60 * 1000).toLong()

@AndroidEntryPoint
class VariantDetailBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG_VARIANT_DETAIL_BS = "VariantDetailBottomSheet_TAG"

        @JvmStatic
        fun newInstance(variantId: String, title: String?): VariantDetailBottomSheetDialogFragment {
            val fragment = VariantDetailBottomSheetDialogFragment()
            val args = Bundle()
            try {
                args.putInt(ARG_VARIANT_ID, variantId.toInt())
            } catch (e: NumberFormatException) {
                Log.e(TAG_VARIANT_DETAIL_BS, "Error parsing variantId '$variantId' to Int", e)
            }
            args.putString(ARG_VARIANT_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }

    private val variantViewModel: VariantViewModel by activityViewModels()
    private val practiceViewModel: PracticeViewModel by activityViewModels()
    private var currentVariantId: Int? = null
    private var currentVariantTitle: String? = null

    private lateinit var svInstructionsArea: NestedScrollView
    private lateinit var btnAcknowledgeInstructions: Button

    private lateinit var flVariantSolvingArea: FrameLayout
    private lateinit var nsvTasksScrollView: NestedScrollView
    private lateinit var tvVariantName: TextView
    private lateinit var tvVariantDescription: TextView
    private lateinit var llDynamicContentContainer: LinearLayout
    private lateinit var btnFinishVariant: Button
    private lateinit var tvTimer: TextView
    private lateinit var btnTimerPauseResume: ImageButton
    
    private lateinit var pbVariantDetailLoading: ProgressBar

    private var countDownTimer: CountDownTimer? = null
    private var timeRemainingInMillis: Long = TIMER_DURATION_MS
    private var instructionsAcknowledged = false
    private val displayedSharedTextIds = mutableSetOf<Int>()

    private var currentVariantEntity: VariantEntity? = null
    private var currentSharedTexts: List<VariantSharedTextEntity>? = null
    private var currentTasks: List<VariantTaskEntity>? = null
    private var currentUserAnswers: Map<Int, UserVariantTaskAnswerEntity>? = null
    private var currentIsChecked: Boolean = false
    private var lastFocusedTaskEditTextId: Int? = null
    private var isPaused = false
    private var timerHasStartedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_VARIANT_ID)) {
                currentVariantId = it.getInt(ARG_VARIANT_ID)
            } else {
                Log.e(TAG_VARIANT_DETAIL_BS, "ARG_VARIANT_ID not found in arguments")
            }
            currentVariantTitle = it.getString(ARG_VARIANT_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_variant_detail, container, false)
        
        pbVariantDetailLoading = view.findViewById<ProgressBar>(R.id.pb_variant_detail_loading)
        
        svInstructionsArea = view.findViewById<NestedScrollView>(R.id.sv_instructions_area)
        btnAcknowledgeInstructions = view.findViewById<Button>(R.id.btn_acknowledge_instructions)
        
        flVariantSolvingArea = view.findViewById(R.id.nsv_variant_solving_area)
        nsvTasksScrollView = view.findViewById(R.id.nsv_tasks_scroll_view)
        tvVariantName = view.findViewById<TextView>(R.id.tv_variant_name_bs)
        tvVariantDescription = view.findViewById<TextView>(R.id.tv_variant_description_bs)
        llDynamicContentContainer = view.findViewById<LinearLayout>(R.id.ll_dynamic_content_container)
        btnFinishVariant = view.findViewById<Button>(R.id.btn_finish_variant)
        tvTimer = view.findViewById<TextView>(R.id.tv_timer)
        btnTimerPauseResume = view.findViewById(R.id.btn_timer_pause_resume)
        
        svInstructionsArea.visibility = View.VISIBLE
        flVariantSolvingArea.visibility = View.GONE
        tvTimer.visibility = View.GONE
        btnTimerPauseResume.visibility = View.GONE
        pbVariantDetailLoading.visibility = View.VISIBLE

        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val currentFocus = activity?.currentFocus
                if (currentFocus is EditText) {
                    val outRect = Rect()
                    currentFocus.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        Log.d(TAG_VARIANT_DETAIL_BS, "Touch outside of EditText, clearing focus and hiding keyboard.")
                        currentFocus.clearFocus()
                        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                        imm?.hideSoftInputFromWindow(v.windowToken, 0)
                    }
                }
            }
            false
        }
        
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { bottomSheet ->
                val behavior = BottomSheetBehavior.from(bottomSheet)
                val layoutParams = bottomSheet.layoutParams
                
                val windowHeight = getWindowHeight()
                if (layoutParams != null) {
                    layoutParams.height = windowHeight
                }
                bottomSheet.layoutParams = layoutParams
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = windowHeight
                behavior.isFitToContents = false
                behavior.skipCollapsed = true
                 behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_DRAGGING && behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                        }
                    }
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })
            }
        }
        return dialog
    }
    
    private fun getWindowHeight(): Int {
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()

        currentVariantId = arguments?.getInt(ARG_VARIANT_ID)
        if (currentVariantId == null) {
            Log.e(TAG_VARIANT_DETAIL_BS, "Variant ID is null, cannot load details.")
            Toast.makeText(requireContext(), "Ошибка: ID варианта не найден", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        currentVariantId?.let {
             variantViewModel.fetchVariantDetails(it)
        }

        btnAcknowledgeInstructions.setOnClickListener {
            instructionsAcknowledged = true
            Log.d(TAG_VARIANT_DETAIL_BS, "Инструкции подтверждены, вызываем checkAndPopulate")
            checkAndPopulate(forceRepopulate = true)
        }
        
        btnFinishVariant.setOnClickListener {
            currentVariantId?.let { variantId ->
                Log.d(TAG_VARIANT_DETAIL_BS, "Нажата кнопка 'Завершить вариант' для variantId: $variantId")
                
                saveVariantResults(variantId)
                
                variantViewModel.checkVariantAnswers(variantId)
            } ?: Log.e(TAG_VARIANT_DETAIL_BS, "Невозможно завершить вариант, currentVariantId is null")
        }

        btnTimerPauseResume.setOnClickListener {
            isPaused = !isPaused
            updatePauseState()
        }
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        if (!instructionsAcknowledged || variantViewModel.variantCheckedState.value || isPaused) {
             Log.d(TAG_VARIANT_DETAIL_BS, "Таймер не запущен: инструкции ($instructionsAcknowledged), проверен (${variantViewModel.variantCheckedState.value}), на паузе ($isPaused)")
            return
        }

        val startTime = if (timerHasStartedOnce) {
            timeRemainingInMillis
        } else {
            currentVariantEntity?.remainingTimeMillis?.takeIf { it > 0 } ?: TIMER_DURATION_MS
        }

        if (startTime <= 0) {
            tvTimer.text = "00:00:00"
            return
        }
        
        timeRemainingInMillis = startTime
        timerHasStartedOnce = true
        
        tvTimer.visibility = View.VISIBLE
        btnTimerPauseResume.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(startTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingInMillis = millisUntilFinished
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                timeRemainingInMillis = 0
                                  tvTimer.text = "00:00:00"
                  Toast.makeText(requireContext(), "Время вышло!", Toast.LENGTH_LONG).show()
                  currentVariantId?.let { 
                    saveVariantResults(it)
                    variantViewModel.checkVariantAnswers(it) 
                 }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        currentVariantId?.let {
            Log.d(TAG_VARIANT_DETAIL_BS, "onDestroyView: Saving timer for variantId: $it")
            variantViewModel.updateVariantTimer(it, timeRemainingInMillis)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        Log.d(TAG_VARIANT_DETAIL_BS, "onDismiss called, consuming variant details and saving timer.")
        currentVariantId?.let { variantViewModel.updateVariantTimer(it, timeRemainingInMillis) }
        variantViewModel.consumeVariantDetails()
        countDownTimer?.cancel()
        countDownTimer = null

        timerHasStartedOnce = false
        isPaused = false

        Log.d(TAG_VARIANT_DETAIL_BS, "Timer cancelled and states reset in onDismiss.")
    }

    private fun observeViewModel() {
        Log.d(TAG_VARIANT_DETAIL_BS, "observeViewModel called")

        variantViewModel.variantDetailsLiveData.observe(viewLifecycleOwner) { resource ->
            Log.d(TAG_VARIANT_DETAIL_BS, "Observed variantDetailsLiveData: type=${resource::class.simpleName}, data.present=${resource.data != null}, error=${resource.message}")
            currentVariantEntity = if (resource is Resource.Success) resource.data else null
            checkAndPopulate()
        }

        variantViewModel.sharedTextsLiveData.observe(viewLifecycleOwner) { resource ->
            Log.d(TAG_VARIANT_DETAIL_BS, "Observed sharedTextsLiveData: type=${resource::class.simpleName}, data.present=${resource.data != null}, error=${resource.message}")
            currentSharedTexts = if (resource is Resource.Success) resource.data else null
            checkAndPopulate()
        }

        variantViewModel.tasksLiveData.observe(viewLifecycleOwner) { resource ->
            Log.d(TAG_VARIANT_DETAIL_BS, "Observed tasksLiveData: type=${resource::class.simpleName}, data.present=${resource.data != null}, error=${resource.message}")
            currentTasks = if (resource is Resource.Success) resource.data else null
            checkAndPopulate()
        }

        variantViewModel.userAnswersForCurrentVariantLiveData.observe(viewLifecycleOwner) { resource ->
            Log.d(TAG_VARIANT_DETAIL_BS, "Observed userAnswersForCurrentVariantLiveData: type=${resource::class.simpleName}, data.size=${(resource.data as? Map<*,*>)?.size}, error=${resource.message}")
            currentUserAnswers = if (resource is Resource.Success) resource.data else null
            checkAndPopulate()
        }

        variantViewModel.variantCheckedState.asLiveData().observe(viewLifecycleOwner) { isChecked ->
            Log.d(TAG_VARIANT_DETAIL_BS, "Observed variantCheckedState: $isChecked")
            currentIsChecked = isChecked
            if (isChecked) {
                tvTimer.visibility = View.GONE
                countDownTimer?.cancel()
                btnFinishVariant.isEnabled = false
                btnTimerPauseResume.visibility = View.GONE
                btnAcknowledgeInstructions.visibility = View.GONE
                Log.d(TAG_VARIANT_DETAIL_BS, "Variant is checked, calling checkAndPopulate to show results.")
                checkAndPopulate(forceRepopulate = true)

                currentVariantId?.let { variantId ->
                    Log.d(TAG_VARIANT_DETAIL_BS, "Variant $variantId is checked. Preparing for sync or cleanup.")
                }

                timeRemainingInMillis = TIMER_DURATION_MS
                Log.d(TAG_VARIANT_DETAIL_BS, "Local timer value has been reset to default to prevent overwrite on dismiss.")

            } else {
                checkAndPopulate()
            }
        }
    }
    
    private fun checkAndPopulate(forceRepopulate: Boolean = false) {
        Log.d(TAG_VARIANT_DETAIL_BS, "checkAndPopulate called. Force: $forceRepopulate, Ack: $instructionsAcknowledged, Checked: $currentIsChecked")

        val variant = currentVariantEntity
        val texts = currentSharedTexts
        val tasks = currentTasks
        val answersMap = currentUserAnswers

        val variantResource = variantViewModel.variantDetailsLiveData.value
        val textsResource = variantViewModel.sharedTextsLiveData.value
        val tasksResource = variantViewModel.tasksLiveData.value
        val answersResource = variantViewModel.userAnswersForCurrentVariantLiveData.value

        val essentialDataAvailable = variant != null && texts != null && tasks != null && answersMap != null
        
        Log.d(TAG_VARIANT_DETAIL_BS, "checkAndPopulate - Conditions: currentIsChecked=$currentIsChecked, tasks.size=${tasks?.size}, essentialDataAvailable=$essentialDataAvailable, instructionsAcknowledged=$instructionsAcknowledged")

        val readyToShowSolving = instructionsAcknowledged && essentialDataAvailable

        if (readyToShowSolving) {
            svInstructionsArea.visibility = View.GONE
            pbVariantDetailLoading.visibility = View.GONE
            flVariantSolvingArea.visibility = View.VISIBLE

            populateDynamicContent(variant!!, texts!!, tasks!!, answersMap!!, currentIsChecked)

            if (currentIsChecked) {
                btnFinishVariant.isEnabled = false
                btnFinishVariant.text = "Вариант проверен"
                countDownTimer?.cancel()
                tvTimer.visibility = View.GONE
                btnTimerPauseResume.visibility = View.GONE
                disableAnswerFields()
            } else {
                btnFinishVariant.isEnabled = true
                btnFinishVariant.text = "Завершить вариант"
                if (tasks?.isNotEmpty() == true) {
                    updatePauseState()
                } else {
                    tvTimer.visibility = View.GONE
                    btnTimerPauseResume.visibility = View.GONE
                    btnFinishVariant.isEnabled = false
                }
            }
        } else {
            flVariantSolvingArea.visibility = View.GONE
            svInstructionsArea.visibility = View.VISIBLE

            val hasError = variantResource is Resource.Error || textsResource is Resource.Error || tasksResource is Resource.Error || answersResource is Resource.Error
            if (hasError) {
                pbVariantDetailLoading.visibility = View.GONE
                val errorMsg = (variantResource as? Resource.Error)?.message ?:
                               (textsResource as? Resource.Error)?.message ?:
                               (tasksResource as? Resource.Error)?.message ?:
                               (answersResource as? Resource.Error)?.message ?: "Неизвестная ошибка загрузки данных."
                Toast.makeText(context, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                btnAcknowledgeInstructions.text = "Ошибка загрузки"
                btnAcknowledgeInstructions.isEnabled = false
            } else if (essentialDataAvailable) {
                pbVariantDetailLoading.visibility = View.GONE
                btnAcknowledgeInstructions.isEnabled = true
                btnAcknowledgeInstructions.text = "Я прочитал(а) и готов(а) начать"
            } else {
                pbVariantDetailLoading.visibility = View.VISIBLE
                btnAcknowledgeInstructions.isEnabled = false
                btnAcknowledgeInstructions.text = "Загрузка данных..."
            }
        }
    }

    private fun showErrorStateInSolvingArea(message: String) {
        flVariantSolvingArea.visibility = View.VISIBLE
        llDynamicContentContainer.removeAllViews()
        val errorTextView = TextView(requireContext()).apply {
            text = message
            setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
            gravity = android.view.Gravity.CENTER
            setPadding(16,16,16,16)
        }
        llDynamicContentContainer.addView(errorTextView)
        btnFinishVariant.isEnabled = false
        tvTimer.visibility = View.GONE
        btnTimerPauseResume.visibility = View.GONE
    }

    private fun disableAnswerFields() {
        for (i in 0 until llDynamicContentContainer.childCount) {
            val view = llDynamicContentContainer.getChildAt(i)
            if (view is ViewGroup && view.tag is Int) {
                findEditTextsRecursively(view).forEach {
                    it.isFocusable = false
                    it.isClickable = false
                    it.isFocusableInTouchMode = false
                    it.isEnabled = false
                }
            }
        }
    }
    
    private fun findEditTextsRecursively(viewGroup: ViewGroup): List<EditText> {
        val editTexts = mutableListOf<EditText>()
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is EditText) {
                editTexts.add(child)
            } else if (child is ViewGroup) {
                editTexts.addAll(findEditTextsRecursively(child))
            }
        }
        return editTexts
    }

    private fun populateDynamicContent(
        variant: VariantEntity,
        sharedTexts: List<VariantSharedTextEntity>,
        tasks: List<VariantTaskEntity>,
        userAnswers: Map<Int, UserVariantTaskAnswerEntity>,
        isChecked: Boolean
    ) {
        if (llDynamicContentContainer.childCount > 0 && !isChecked) {
            Log.d(TAG_VARIANT_DETAIL_BS, "populateDynamicContent: Skipping full redraw for ongoing input. Child count: ${llDynamicContentContainer.childCount}, isChecked: $isChecked")
            return
        }

        llDynamicContentContainer.removeAllViews()
        displayedSharedTextIds.clear()
        Log.d(TAG_VARIANT_DETAIL_BS, "populateDynamicContent: Performing full redraw. isChecked: $isChecked. SharedTexts: ${sharedTexts.size}, Tasks: ${tasks.size}, Answers: ${userAnswers.size}")
        
        var addedPart1Instruction = false
        var addedTask1_3Instruction = false
        var addedPart2Instruction = false
        
        val textIdForTasks22_26 = tasks.find { 
            (it.egeNumber.startsWith("22") || it.egeNumber.startsWith("23") || it.egeNumber.startsWith("24") || it.egeNumber.startsWith("25") || it.egeNumber.startsWith("26")) && it.variantSharedTextId != null 
        }?.variantSharedTextId

        if (!addedPart1Instruction) {
            val part1Title = createStyledTextView("Часть 1", styleResId = android.R.style.TextAppearance_Material_Title, isBold = true, isCentered = true, bottomMarginDp = 4)
            llDynamicContentContainer.addView(part1Title)
            val part1Desc = createStyledTextView(
                "Ответами к заданиям 1-26 являются цифра (число), или слово (несколько слов), или последовательность цифр (чисел). Ответ запишите в поле ответа в тексте работы, а затем перенесите в БЛАНК ОТВЕТОВ № 1 справа от номера задания, начиная с первой клеточки, без пробелов, запятых и других дополнительных символов. Каждую букву или цифру пишите в отдельной клеточке в соответствии с приведёнными в бланке образцами.", 
                styleResId = android.R.style.TextAppearance_Material_Body2, 
                bottomMarginDp = 16
            )
            llDynamicContentContainer.addView(part1Desc)
            addSeparator(llDynamicContentContainer)
            addedPart1Instruction = true
        }

        val uniqueTextIdsInTasks = tasks.mapNotNull { it.variantSharedTextId }.distinct()
        val textsToDisplay = sharedTexts.filter { uniqueTextIdsInTasks.contains(it.variantSharedTextId) }
                                      .sortedBy { it.variantSharedTextId } 

        textsToDisplay.forEach { textEntity ->
            if (textEntity.variantSharedTextId == textIdForTasks22_26) {
                return@forEach
            }

            if (displayedSharedTextIds.add(textEntity.variantSharedTextId)) { 
                if (tasks.any { it.variantSharedTextId == textEntity.variantSharedTextId && it.orderInVariant <= 3 && it.orderInVariant >=1 } && !addedTask1_3Instruction) {
                    llDynamicContentContainer.addView(createStyledTextView("Прочитайте текст и выполните задания 1-3.", isBold = true, styleResId = android.R.style.TextAppearance_Material_Subhead, bottomMarginDp = 8))
                    addedTask1_3Instruction = true
                }
                
                val originalTextContent = textEntity.textContent ?: ""
                val textToDisplay: CharSequence = if (originalTextContent.contains("{")) {
                    formatTextWithCurlyBraceHighlights(originalTextContent)
                } else {
                    originalTextContent
                }

                val tvSharedText = createStyledTextView(
                    textToDisplay, 
                    styleResId = android.R.style.TextAppearance_Material_Body1, 
                    bottomMarginDp = 16
                )
                llDynamicContentContainer.addView(tvSharedText)
            }
        }
        
        var lastDisplayedOrder = 0
        val sortedTasks = tasks.sortedBy { it.orderInVariant }
        sortedTasks.forEach { taskEntity ->
            if (taskEntity.orderInVariant == 27 && !addedPart2Instruction) {
                addSeparator(llDynamicContentContainer)
                llDynamicContentContainer.addView(createStyledTextView("Часть 2", styleResId = android.R.style.TextAppearance_Material_Title, isBold = true, isCentered = true, topMarginDp = 16, bottomMarginDp = 4))
                llDynamicContentContainer.addView(createStyledTextView("Для ответа на задание 27 используйте БЛАНК ОТВЕТОВ № 2.", styleResId = android.R.style.TextAppearance_Material_Body2, isCentered = true, bottomMarginDp = 16))
                addSeparator(llDynamicContentContainer)
                addedPart2Instruction = true
            }

            val taskContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 8, 0, 8)
                tag = taskEntity.variantTaskId 
            }

            val taskFullTitle = SpannableString("Задание ${taskEntity.egeNumber ?: taskEntity.orderInVariant}. (${taskEntity.maxPoints} балл.) ${taskEntity.title ?: ""}")
            val tvTaskTitle = createStyledTextView(taskFullTitle, styleResId = android.R.style.TextAppearance_Material_Subhead, isBold = true, bottomMarginDp = 8)
            taskContainer.addView(tvTaskTitle)

            if (!taskEntity.taskStatement.isNullOrBlank()){
                val taskStatementView: View
                if (taskEntity.egeNumber == "8" || taskEntity.egeNumber == "22") {
                    taskStatementView = createTaskTableLayout(requireContext(), taskEntity.taskStatement!!)
                } else {
                    taskStatementView = createStyledTextView(taskEntity.taskStatement!!, styleResId = android.R.style.TextAppearance_Material_Body2, bottomMarginDp = 8)
                }
                taskContainer.addView(taskStatementView)
            }
            
            val etAnswer = EditText(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                hint = "Введите ваш ответ"
                id = View.generateViewId()
                val userAnswerEntity = userAnswers[taskEntity.variantTaskId]
                setText(userAnswerEntity?.userSubmittedAnswer ?: "")
                
                isEnabled = !isChecked
                isFocusable = !isChecked
                isClickable = !isChecked
                isFocusableInTouchMode = !isChecked

                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        lastFocusedTaskEditTextId = taskEntity.variantTaskId
                        Log.d(TAG_VARIANT_DETAIL_BS, "EditText for task ${taskEntity.variantTaskId} (viewId: ${this.id}) gained focus. lastFocusedTaskEditTextId set to ${taskEntity.variantTaskId}.")
                    }
                }

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (!isChecked) {
                            currentVariantId?.let {
                                variantViewModel.saveUserAnswerAndCheck(
                                    variantId = it,
                                    taskId = taskEntity.variantTaskId,
                                    answer = s.toString()
                                )
                            }
                        }
                    }
                })
            }
            taskContainer.addView(etAnswer)

            if (isChecked) {
                val userAnswerEntity = userAnswers[taskEntity.variantTaskId]
                val isCorrect = userAnswerEntity?.isSubmissionCorrect
                val points = userAnswerEntity?.pointsAwarded ?: 0

                val resultTextView = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setPadding(8, 4, 8, 4)
                    textSize = 14f
                }

                if (isCorrect == true) {
                    etAnswer.setBackgroundColor(getThemeColor(R.color.correct_answer_background_light))
                    resultTextView.text = "Верно (+${points} балл(ов))"
                    resultTextView.setTextColor(getThemeColor(R.color.correct_answer_green))
                } else {
                    etAnswer.setBackgroundColor(getThemeColor(R.color.incorrect_answer_background_light))
                    resultTextView.text = "Неверно (${points} балл(ов))"
                    resultTextView.setTextColor(getThemeColor(R.color.incorrect_answer_red))
                }
                taskContainer.addView(resultTextView)

                val tvShowSolution = TextView(requireContext()).apply {
                    text = "Показать ответ и объяснение"
                    setTextColor(getThemeColor(R.color.show_answer_link_color))
                    setTypeface(null, Typeface.BOLD)
                    setPadding(8, 8, 8, 8)
                    isClickable = true
                    isFocusable = true
                    id = View.generateViewId()
                }
                taskContainer.addView(tvShowSolution)

                val solutionExplanationContainer = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setPadding(8,4,8,8)
                    setBackgroundColor(getThemeColor(R.color.solution_explanation_background_light))
                    visibility = View.GONE
                    id = View.generateViewId()
                }
                
                if (!taskEntity.solutionText.isNullOrBlank()) {
                    val tvCorrectAnswer = TextView(requireContext()).apply {
                        text = "Правильный ответ: ${taskEntity.solutionText}"
                        setPadding(0,0,0,4)
                        id = View.generateViewId()
                    }
                    solutionExplanationContainer.addView(tvCorrectAnswer)
                }

                if (!taskEntity.explanationText.isNullOrBlank()) {
                    val tvExplanation = TextView(requireContext()).apply {
                        text = "Объяснение: ${taskEntity.explanationText}"
                         id = View.generateViewId()
                    }
                    solutionExplanationContainer.addView(tvExplanation)
                }
                
                if (solutionExplanationContainer.childCount > 0) {
                    taskContainer.addView(solutionExplanationContainer)
                    tvShowSolution.setOnClickListener {
                        solutionExplanationContainer.visibility =
                            if (solutionExplanationContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    }
                } else {
                     tvShowSolution.visibility = View.GONE
                }
            }

            llDynamicContentContainer.addView(taskContainer)

            if (taskEntity.egeNumber.startsWith("22") && textIdForTasks22_26 != null) {
                val sharedTextForTasksAfter22 = sharedTexts.find { it.variantSharedTextId == textIdForTasks22_26 }
                if (sharedTextForTasksAfter22 != null && displayedSharedTextIds.add(textIdForTasks22_26)) {
                    addSeparator(llDynamicContentContainer) 

                    llDynamicContentContainer.addView(
                        createStyledTextView(
                            "Прочитайте следующий текст и выполните задания 23-26.", 
                            isBold = true, styleResId = android.R.style.TextAppearance_Material_Subhead, bottomMarginDp = 8
                        )
                    )
                    val tvSharedTextInjected = createStyledTextView(
                        formatFootnotes(sharedTextForTasksAfter22.textContent ?: ""), 
                        styleResId = android.R.style.TextAppearance_Material_Body1, 
                        bottomMarginDp = 16
                    )
                    llDynamicContentContainer.addView(tvSharedTextInjected)
                }
            }

            if (taskEntity != sortedTasks.lastOrNull()) { 
                 addSeparator(llDynamicContentContainer)
            }
            lastDisplayedOrder = taskEntity.orderInVariant
        }

        addSeparator(llDynamicContentContainer)
        llDynamicContentContainer.addView(createStyledTextView(
            "Не забудьте перенести все ответы в бланк ответов № 1 (и № 2 для сочинения) в соответствии с инструкцией по выполнению работы. Проверьте, чтобы каждый ответ был записан в строке с номером соответствующего задания.",
            styleResId = android.R.style.TextAppearance_Material_Body2, 
            isBold = true, 
            topMarginDp = 16, 
            bottomMarginDp = 24
        ))
        
        Log.d(TAG_VARIANT_DETAIL_BS, "populateDynamicContent finished. Children in llDynamicContentContainer: ${llDynamicContentContainer.childCount}")

        if (!isChecked && lastFocusedTaskEditTextId != null) {
            Log.d(TAG_VARIANT_DETAIL_BS, "Attempting to restore focus to EditText in task container with ID: $lastFocusedTaskEditTextId after full redraw.")
            val containerToFocus = llDynamicContentContainer.findViewWithTag<ViewGroup>(lastFocusedTaskEditTextId)
            containerToFocus?.let { taskLayout ->
                findEditTextsRecursively(taskLayout).firstOrNull()?.let { editText ->
                    Log.d(TAG_VARIANT_DETAIL_BS, "Found EditText (viewId: ${editText.id}) in task $lastFocusedTaskEditTextId, posting requestFocus.")
                    editText.post {
                        Log.d(TAG_VARIANT_DETAIL_BS, "Executing posted requestFocus for EditText (viewId: ${editText.id}) in task $lastFocusedTaskEditTextId.")
                        editText.requestFocus()
                        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                        imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                        Log.d(TAG_VARIANT_DETAIL_BS, "Explicitly tried to show keyboard for EditText (viewId: ${editText.id}) in task $lastFocusedTaskEditTextId.")
                    }
                } ?: run {
                    Log.w(TAG_VARIANT_DETAIL_BS, "Could not find EditText in container for task ID: $lastFocusedTaskEditTextId to restore focus.")
                }
            } ?: run {
                Log.w(TAG_VARIANT_DETAIL_BS, "Could not find task container with ID: $lastFocusedTaskEditTextId to restore focus.")
            }
        }
    }

    private fun createStyledTextView(text: CharSequence, styleResId: Int = android.R.style.TextAppearance_Material_Body1, isBold: Boolean = false, isCentered: Boolean = false, topMarginDp: Int = 0, bottomMarginDp: Int = 8): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            TextViewCompat.setTextAppearance(this, styleResId)
            if (isBold) this.typeface = Typeface.DEFAULT_BOLD
            if (isCentered) this.gravity = android.view.Gravity.CENTER_HORIZONTAL
            
            val scale = resources.displayMetrics.density
            val topMarginPx = (topMarginDp * scale + 0.5f).toInt()
            val bottomMarginPx = (bottomMarginDp * scale + 0.5f).toInt()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, topMarginPx, 0, bottomMarginPx)
            }
            id = View.generateViewId()
        }
    }
    
    private fun addSeparator(container: LinearLayout) {
        val separator = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.dpToPx()
            ).apply {
                val margin = 8.dpToPx()
                setMargins(0, margin, 0, margin)
            }
            setBackgroundColor(getThemeColor(R.color.divider_light))
            id = View.generateViewId()
        }
        container.addView(separator)
    }

    private fun formatFootnotes(text: String): SpannableString {
        val spannableString = SpannableString(text)
        val pattern = Pattern.compile("(\\s|\\[|\\()(\\d+)(\\]|\\))")
        val matcher = pattern.matcher(text)

        while (matcher.find()) {
            val startIndex = matcher.start(2)
            val endIndex = matcher.end(2)

            spannableString.setSpan(
                TextAppearanceSpan(requireContext(), R.style.FootnoteText),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannableString
    }

    fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun getThemeColor(@ColorRes colorRes: Int): Int {
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    private fun updatePauseState() {
        if (isPaused) {
            countDownTimer?.cancel()
            nsvTasksScrollView.visibility = View.GONE
            btnFinishVariant.visibility = View.GONE
            btnTimerPauseResume.setImageResource(android.R.drawable.ic_media_play)
            Log.d(TAG_VARIANT_DETAIL_BS, "Таймер на паузе. Осталось: $timeRemainingInMillis")
        } else {
            if (instructionsAcknowledged) {
                 nsvTasksScrollView.visibility = View.VISIBLE
                 btnFinishVariant.visibility = View.VISIBLE
            }
            btnTimerPauseResume.setImageResource(android.R.drawable.ic_media_pause)
            startTimer()
            Log.d(TAG_VARIANT_DETAIL_BS, "Таймер возобновлен.")
        }
    }

    /**
     * Сохраняет результаты выполнения варианта в статистику заданий
     */
    private fun saveVariantResults(variantId: Int) {
        val tasks = currentTasks ?: return
        val userAnswers = currentUserAnswers ?: return
        val variantEntity = currentVariantEntity ?: return

        if (tasks.isEmpty()) {
            Log.d(TAG_VARIANT_DETAIL_BS, "Нет заданий для сохранения результатов варианта")
            return
        }

        val completionTime = TIMER_DURATION_MS - timeRemainingInMillis
        val timestamp = System.currentTimeMillis()

        val allTaskAnswers = tasks.map { task ->
            val userAnswer = userAnswers[task.variantTaskId]
            VariantResult.TaskAnswer(
                taskId = task.variantTaskId.toString(),
                userAnswer = userAnswer?.userSubmittedAnswer ?: "-",
                correctAnswer = task.solutionText ?: "-",
                isCorrect = userAnswer?.isSubmissionCorrect ?: false
            )
        }

        val totalScore = allTaskAnswers.count { it.isCorrect }
        val maxScore = allTaskAnswers.size

        val variantResult = VariantResult(
            variantId = variantId.toString(),
            tasks = allTaskAnswers,
            score = totalScore,
            maxScore = maxScore,
            completionTime = completionTime,
            timestamp = timestamp
        )

        val variantStatEntity = PracticeStatisticsEntity(
            "${variantEntity.name}_${timestamp}",
            maxScore, 
            totalScore,
            timestamp,
            variantResult.toJsonString()
        )

        practiceViewModel.saveVariantStatistics(variantStatEntity)

        Toast.makeText(requireContext(), "Результаты варианта сохранены", Toast.LENGTH_SHORT).show()
    }

    private fun createTaskTableLayout(context: Context, statement: String): View {
        val mainLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val horizontalLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            (this.layoutParams as LinearLayout.LayoutParams).topMargin = 8.dpToPx()
        }

        val column1Layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, 0, 4.dpToPx(), 0)
        }

        val column2Layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(4.dpToPx(), 0, 0, 0)
        }

        val lines = statement.split('\n').map { it.trim() }
        val column1Items = mutableListOf<String>()
        val column2Items = mutableListOf<String>()
        var switchedToColumn2 = false

        for (line in lines) {
            if (line.isEmpty()) continue

            if (!switchedToColumn2 && line.matches(Regex("^[А-ЯЁ]\\s*\\).*"))) {
                column1Items.add(line)
            } else if (line.matches(Regex("^\\d+\\s*\\).*"))) {
                switchedToColumn2 = true
                column2Items.add(line)
            } else if (switchedToColumn2) {
                if (column2Items.isNotEmpty()) {
                    column2Items[column2Items.size - 1] = column2Items.last() + "\n" + line
                } else {
                     column2Items.add(line)
                }
            } else {
                if (column1Items.isNotEmpty()) {
                    column1Items[column1Items.size - 1] = column1Items.last() + "\n" + line
                } else {
                    column1Items.add(line)
                }
            }
        }

        column1Items.forEach { item ->
            column1Layout.addView(createStyledTextView(item, styleResId = android.R.style.TextAppearance_Material_Body2, bottomMarginDp = 4))
        }
        column2Items.forEach { item ->
            column2Layout.addView(createStyledTextView(item, styleResId = android.R.style.TextAppearance_Material_Body2, bottomMarginDp = 4))
        }

        if (column1Layout.childCount == 0 && column2Layout.childCount == 0 && statement.isNotEmpty()) {
             val fallbackTextView = createStyledTextView(statement, styleResId = android.R.style.TextAppearance_Material_Body2, bottomMarginDp = 8)
             mainLayout.addView(fallbackTextView)
        } else {
            if (column1Layout.childCount > 0) horizontalLayout.addView(column1Layout)
            if (column2Layout.childCount > 0) horizontalLayout.addView(column2Layout)
            if (horizontalLayout.childCount > 0) mainLayout.addView(horizontalLayout)
        }
        
        return mainLayout
    }

    private fun formatTextWithCurlyBraceHighlights(text: String): SpannableString {
        Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Input text: [$text]")
        val displayText = text.replace(Regex("\\{([^}]+)\\}"), "$1")
        Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Display text (after replace): [$displayText]")
        val spannableString = SpannableString(displayText)
    
        val pattern = Pattern.compile("\\{([^}]+)\\}")
        val matcher = pattern.matcher(text)
    
        var cumulativeOffset = 0
        var matchesFound = 0
    
        while (matcher.find()) {
            matchesFound++
            val wordInBraces = matcher.group(0)!!
            val wordOnly = matcher.group(1)!!
            Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Match $matchesFound: wordInBraces='${wordInBraces}', wordOnly='${wordOnly}'")
    
            val startInDisplayText = matcher.start() - cumulativeOffset
            val endInDisplayText = startInDisplayText + wordOnly.length
            Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Calculated indices for displayText: start=$startInDisplayText, end=$endInDisplayText (cumulativeOffset=$cumulativeOffset)")
    
            if (startInDisplayText >= 0 && endInDisplayText <= spannableString.length) {
                spannableString.setSpan(StyleSpan(Typeface.BOLD), startInDisplayText, endInDisplayText, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Applied BOLD span to '${spannableString.substring(startInDisplayText, endInDisplayText)}'")
            } else {
                Log.w(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Invalid indices, skipping span for word '$wordOnly'")
            }
            cumulativeOffset += 2
        }
        if (matchesFound == 0) {
            Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - No matches found for {} pattern.")
        }
        Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Returning spannable: $spannableString")
        return spannableString
    }
}
 