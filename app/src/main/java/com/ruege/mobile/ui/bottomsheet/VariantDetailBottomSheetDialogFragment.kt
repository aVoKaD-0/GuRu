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
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.R
import com.ruege.mobile.data.local.entity.VariantEntity
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.data.local.entity.VariantSharedTextEntity
import com.ruege.mobile.data.local.entity.VariantTaskEntity
import com.ruege.mobile.data.local.entity.UserVariantTaskAnswerEntity
import com.ruege.mobile.databinding.BottomSheetVariantDetailBinding
import com.ruege.mobile.utils.Resource
import com.ruege.mobile.ui.viewmodel.VariantViewModel
import com.ruege.mobile.ui.viewmodel.PracticeViewModel
import com.ruege.mobile.model.VariantResult
import com.ruege.mobile.ui.bottomsheet.helper.VariantContentBuilder
import com.ruege.mobile.ui.bottomsheet.helper.VariantContentHolder
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import android.content.DialogInterface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import timber.log.Timber

private const val ARG_VARIANT_ID = "variant_id"
private const val ARG_VARIANT_TITLE = "variant_title"
private const val TIMER_DURATION_MS = (3 * 60 * 60 * 1000) + (55 * 60 * 1000).toLong()

@AndroidEntryPoint
class VariantDetailBottomSheetDialogFragment : BottomSheetDialogFragment(), VariantContentBuilder.Listeners {

    companion object {
        const val TAG_VARIANT_DETAIL_BS = "VariantDetailBottomSheet_TAG"

        @JvmStatic
        fun newInstance(variantId: String, title: String?): VariantDetailBottomSheetDialogFragment {
            val fragment = VariantDetailBottomSheetDialogFragment()
            val args = Bundle()
            try {
                args.putInt(ARG_VARIANT_ID, variantId.toInt())
            } catch (e: NumberFormatException) {
                Timber.e(TAG_VARIANT_DETAIL_BS, "Error parsing variantId '$variantId' to Int", e)
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

    private var _binding: BottomSheetVariantDetailBinding? = null
    private val binding get() = _binding!!
    
    private var countDownTimer: CountDownTimer? = null
    private var timeRemainingInMillis: Long = TIMER_DURATION_MS
    private var instructionsAcknowledged = false

    private var contentBuilder: VariantContentBuilder? = null
    private var contentHolder: VariantContentHolder? = null

    private var currentVariantEntity: VariantEntity? = null
    private var currentSharedTexts: List<VariantSharedTextEntity>? = null
    private var currentTasks: List<VariantTaskEntity>? = null
    private var currentUserAnswers: Map<Int, UserVariantTaskAnswerEntity>? = null
    private var currentIsChecked: Boolean = false
    private var lastFocusedTaskEditTextId: Int? = null
    private var isPaused = false
    private var timerHasStartedOnce = false
    private var essayIsChecked = false
    private var markwon: Markwon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_VARIANT_ID)) {
                currentVariantId = it.getInt(ARG_VARIANT_ID)
            } else {
                Timber.e(TAG_VARIANT_DETAIL_BS, "ARG_VARIANT_ID not found in arguments")
            }
            currentVariantTitle = it.getString(ARG_VARIANT_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetVariantDetailBinding.inflate(inflater, container, false)
        
        markwon = Markwon.builder(requireContext())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(requireContext()))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build()
            
        contentBuilder = VariantContentBuilder(requireContext(), markwon!!, this)

        binding.svInstructionsArea.visibility = View.VISIBLE
        binding.nsvVariantSolvingArea.visibility = View.GONE
        binding.tvTimer.visibility = View.GONE
        binding.btnTimerPauseResume.visibility = View.GONE
        binding.pbVariantDetailLoading.visibility = View.VISIBLE

        binding.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val currentFocus = activity?.currentFocus
                if (currentFocus is EditText) {
                    val outRect = Rect()
                    currentFocus.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        Timber.d(TAG_VARIANT_DETAIL_BS, "Touch outside of EditText, clearing focus and hiding keyboard.")
                        currentFocus.clearFocus()
                        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                        imm?.hideSoftInputFromWindow(v.windowToken, 0)
                    }
                }
            }
            false
        }
        
        return binding.root
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
        observeEssayCheckState()

        currentVariantId = arguments?.getInt(ARG_VARIANT_ID)
        if (currentVariantId == null) {
            Timber.e(TAG_VARIANT_DETAIL_BS, "Variant ID is null, cannot load details.")
            Toast.makeText(requireContext(), "Ошибка: ID варианта не найден", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        currentVariantId?.let {
             variantViewModel.fetchVariantDetails(it)
        }

        binding.btnAcknowledgeInstructions.setOnClickListener {
            instructionsAcknowledged = true
            Timber.d(TAG_VARIANT_DETAIL_BS, "Инструкции подтверждены, вызываем checkAndPopulate")
            checkAndPopulate(forceRepopulate = true)
        }
        
        binding.btnFinishVariant.setOnClickListener {
            currentVariantId?.let { variantId ->
                Timber.d(TAG_VARIANT_DETAIL_BS, "Нажата кнопка 'Завершить вариант' для variantId: $variantId")
                
                saveVariantResults(variantId)
                
                variantViewModel.checkVariantAnswers(variantId)
            } ?: Timber.e(TAG_VARIANT_DETAIL_BS, "Невозможно завершить вариант, currentVariantId is null")
        }

        binding.btnTimerPauseResume.setOnClickListener {
            isPaused = !isPaused
            updatePauseState()
        }
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        if (!instructionsAcknowledged || variantViewModel.variantCheckedState.value || isPaused) {
             Timber.d(TAG_VARIANT_DETAIL_BS, "Таймер не запущен: инструкции ($instructionsAcknowledged), проверен (${variantViewModel.variantCheckedState.value}), на паузе ($isPaused)")
            return
        }

        val startTime = if (timerHasStartedOnce) {
            timeRemainingInMillis
        } else {
            currentVariantEntity?.remainingTimeMillis?.takeIf { it > 0 } ?: TIMER_DURATION_MS
        }

        if (startTime <= 0) {
            binding.tvTimer.text = "00:00:00"
            return
        }
        
        timeRemainingInMillis = startTime
        timerHasStartedOnce = true
        
        binding.tvTimer.visibility = View.VISIBLE
        binding.btnTimerPauseResume.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(startTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingInMillis = millisUntilFinished
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                timeRemainingInMillis = 0
                                  binding.tvTimer.text = "00:00:00"
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
            Timber.d(TAG_VARIANT_DETAIL_BS, "onDestroyView: Saving timer for variantId: $it")
            variantViewModel.updateVariantTimer(it, timeRemainingInMillis)
        }
        variantViewModel.clearEssayCheckState()
        contentHolder = null
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        Timber.d(TAG_VARIANT_DETAIL_BS, "onDismiss called, consuming variant details and saving timer.")
        currentVariantId?.let { variantId ->
            variantViewModel.updateVariantTimer(variantId, timeRemainingInMillis)
            if (currentIsChecked) {
                variantViewModel.clearAnswersForCompletedVariant(variantId)
            }
        }
        variantViewModel.consumeVariantDetails()
        variantViewModel.clearEssayCheckState()
        countDownTimer?.cancel()
        countDownTimer = null

        timerHasStartedOnce = false
        isPaused = false

        Timber.d(TAG_VARIANT_DETAIL_BS, "Timer cancelled and states reset in onDismiss.")
    }

    private fun observeViewModel() {
        Timber.d(TAG_VARIANT_DETAIL_BS, "observeViewModel called")

        variantViewModel.variantDetailsLiveData.observe(viewLifecycleOwner) { resource ->
            Timber.d(TAG_VARIANT_DETAIL_BS, "Observed variantDetailsLiveData: type=${resource::class.simpleName}, data.present=${resource.data != null}, error=${resource.message}")
            currentVariantEntity = if (resource is Resource.Success) resource.data else null
            checkAndPopulate()
        }

        variantViewModel.sharedTextsLiveData.observe(viewLifecycleOwner) { resource ->
            Timber.d(TAG_VARIANT_DETAIL_BS, "Observed sharedTextsLiveData: type=${resource::class.simpleName}, data.present=${resource.data != null}, error=${resource.message}")
            currentSharedTexts = if (resource is Resource.Success) resource.data else null
            checkAndPopulate()
        }

        variantViewModel.tasksLiveData.observe(viewLifecycleOwner) { resource ->
            Timber.d(TAG_VARIANT_DETAIL_BS, "Observed tasksLiveData: type=${resource::class.simpleName}, data.present=${resource.data != null}, error=${resource.message}")
            currentTasks = if (resource is Resource.Success) resource.data else null
            checkAndPopulate()
        }

        variantViewModel.userAnswersForCurrentVariantLiveData.observe(viewLifecycleOwner) { resource ->
            Timber.d(TAG_VARIANT_DETAIL_BS, "Observed userAnswersForCurrentVariantLiveData: type=${resource::class.simpleName}, data.size=${(resource.data as? Map<*,*>)?.size}, error=${resource.message}")
            currentUserAnswers = if (resource is Resource.Success) resource.data else null
            checkAndPopulate()
        }

        variantViewModel.variantCheckedState.asLiveData().observe(viewLifecycleOwner) { isChecked ->
            Timber.d(TAG_VARIANT_DETAIL_BS, "Observed variantCheckedState: $isChecked")
            currentIsChecked = isChecked
            if (isChecked) {
                binding.tvTimer.visibility = View.GONE
                countDownTimer?.cancel()
                binding.btnFinishVariant.isEnabled = false
                binding.btnTimerPauseResume.visibility = View.GONE
                binding.btnAcknowledgeInstructions.visibility = View.GONE
                Timber.d(TAG_VARIANT_DETAIL_BS, "Variant is checked, calling checkAndPopulate to show results.")
                checkAndPopulate(forceRepopulate = true)

                currentVariantId?.let { variantId ->
                    Timber.d(TAG_VARIANT_DETAIL_BS, "Variant $variantId is checked. Preparing for sync or cleanup.")
                }

                timeRemainingInMillis = TIMER_DURATION_MS
                Timber.d(TAG_VARIANT_DETAIL_BS, "Local timer value has been reset to default to prevent overwrite on dismiss.")

            } else {
                checkAndPopulate()
            }
        }
    }
    
    private fun observeEssayCheckState() {
        lifecycleScope.launch {
            variantViewModel.essayCheckState.collectLatest { resource ->
                val holder = contentHolder ?: return@collectLatest

                when (resource) {
                    is Resource.Loading -> {
                        holder.essayProgressBar?.visibility = View.VISIBLE
                        holder.essayCheckButton?.isEnabled = false
                        holder.essayInput?.isEnabled = false
                        holder.essayResultText?.text = resource.data?.detail ?: "Идет проверка..."
                        holder.essayResultText?.visibility = View.VISIBLE
                        binding.btnFinishVariant.isEnabled = false
                    }
                    is Resource.Success -> {
                        holder.essayProgressBar?.visibility = View.GONE
                        holder.essayResultText?.visibility = View.VISIBLE
                        markwon?.setMarkdown(holder.essayResultText!!, resource.data?.result ?: "")
                        holder.essayCheckButton?.text = "Сочинение проверено"
                        holder.essayCheckButton?.isEnabled = false
                        holder.essayInput?.isEnabled = false
                        binding.btnFinishVariant.isEnabled = true
                        essayIsChecked = true
                    }
                    is Resource.Error -> {
                        holder.essayProgressBar?.visibility = View.GONE
                        holder.essayResultText?.visibility = View.VISIBLE
                        holder.essayResultText?.text = "Ошибка: ${resource.message}"
                        holder.essayCheckButton?.isEnabled = true
                        holder.essayInput?.isEnabled = true
                        binding.btnFinishVariant.isEnabled = false
                    }
                    null -> {
                        holder.essayProgressBar?.visibility = View.GONE
                        holder.essayResultText?.visibility = View.GONE
                        holder.essayInput?.isEnabled = !currentIsChecked
                        holder.essayCheckButton?.text = "Проверить сочинение"
                        
                        val wordCount = holder.essayInput?.text.toString().split(Regex("\\s+")).filter { it.isNotBlank() }.size
                        val isLongEnough = wordCount >= 150

                        if (!essayIsChecked) {
                            holder.essayCheckButton?.isEnabled = isLongEnough
                            binding.btnFinishVariant.isEnabled = !isLongEnough
                        }
                        essayIsChecked = false
                    }
                }
            }
        }
    }

    private fun checkAndPopulate(forceRepopulate: Boolean = false) {
        Timber.d(TAG_VARIANT_DETAIL_BS, "checkAndPopulate called. Force: $forceRepopulate, Ack: $instructionsAcknowledged, Checked: $currentIsChecked")

        val variant = currentVariantEntity
        val texts = currentSharedTexts
        val tasks = currentTasks
        val answersMap = currentUserAnswers

        val variantResource = variantViewModel.variantDetailsLiveData.value
        val textsResource = variantViewModel.sharedTextsLiveData.value
        val tasksResource = variantViewModel.tasksLiveData.value
        val answersResource = variantViewModel.userAnswersForCurrentVariantLiveData.value

        val essentialDataAvailable = variant != null && texts != null && tasks != null && answersMap != null
        
        Timber.d(TAG_VARIANT_DETAIL_BS, "checkAndPopulate - Conditions: currentIsChecked=$currentIsChecked, tasks.size=${tasks?.size}, essentialDataAvailable=$essentialDataAvailable, instructionsAcknowledged=$instructionsAcknowledged")

        val readyToShowSolving = instructionsAcknowledged && essentialDataAvailable

        if (readyToShowSolving) {
            binding.svInstructionsArea.visibility = View.GONE
            binding.pbVariantDetailLoading.visibility = View.GONE
            binding.nsvVariantSolvingArea.visibility = View.VISIBLE

            if (binding.llDynamicContentContainer.childCount == 0 || forceRepopulate || currentIsChecked) {
                 binding.llDynamicContentContainer.removeAllViews()

                 val essayTask = tasks!!.find { it.egeNumber == "27" }
                 val essayAnswer = essayTask?.let { answersMap!![it.variantTaskId] }
                 
                 val essayIsCurrentlyLoadingInVM = variantViewModel.essayCheckState.value is Resource.Loading
                 val essayHasPersistedResult = essayAnswer?.checkResult != null
                 val essayIsInFlightFromDB = essayTask?.checkId != null && !essayHasPersistedResult
                 
                 val finalEssayIsBeingChecked = essayIsCurrentlyLoadingInVM || essayIsInFlightFromDB
                 val finalEssayHasResult = essayHasPersistedResult

                 essayIsChecked = finalEssayHasResult

                 val (contentView, holder) = contentBuilder!!.buildContent(
                    userAnswers = answersMap!!,
                    tasks = tasks,
                    sharedTexts = texts!!,
                    isChecked = currentIsChecked,
                    essayIsBeingChecked = finalEssayIsBeingChecked,
                    essayHasResult = finalEssayHasResult
                 )
                 binding.llDynamicContentContainer.addView(contentView)
                 this.contentHolder = holder
                 
                 if(currentIsChecked){
                    updateUiForCheckedState(answersMap, tasks)
                 } else if (!finalEssayIsBeingChecked && !finalEssayHasResult) {
                    val initialEssayText = holder.essayInput?.text?.toString() ?: ""
                    val wordCount = initialEssayText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                    holder.essayCharCount?.text = "Слов: $wordCount"
                    val isLongEnough = wordCount >= 150
                    holder.essayCheckButton?.isEnabled = isLongEnough
                    binding.btnFinishVariant.isEnabled = initialEssayText.isBlank()
                 } else if(finalEssayIsBeingChecked){
                     binding.btnFinishVariant.isEnabled = false
                 }
            }

            if (currentIsChecked) {
                binding.btnFinishVariant.isEnabled = false
                binding.btnFinishVariant.text = "Вариант проверен"
                countDownTimer?.cancel()
                binding.tvTimer.visibility = View.GONE
                binding.btnTimerPauseResume.visibility = View.GONE
            } else {
                binding.btnFinishVariant.isEnabled = true
                binding.btnFinishVariant.text = "Завершить вариант"
                if (tasks?.isNotEmpty() == true) {
                    updatePauseState()
                } else {
                    binding.tvTimer.visibility = View.GONE
                    binding.btnTimerPauseResume.visibility = View.GONE
                    binding.btnFinishVariant.isEnabled = false
                }
            }
            
            restoreFocus()

        } else {
            if (binding.llDynamicContentContainer.childCount > 0) {
                Timber.d(TAG_VARIANT_DETAIL_BS, "checkAndPopulate: Data is being refreshed, but content is already visible. Skipping UI switch to loading screen.")
                return
            }
            
            binding.nsvVariantSolvingArea.visibility = View.GONE
            binding.svInstructionsArea.visibility = View.VISIBLE

            val hasError = variantResource is Resource.Error || textsResource is Resource.Error || tasksResource is Resource.Error || answersResource is Resource.Error
            if (hasError) {
                binding.pbVariantDetailLoading.visibility = View.GONE
                val errorMsg = (variantResource as? Resource.Error)?.message ?:
                               (textsResource as? Resource.Error)?.message ?:
                               (tasksResource as? Resource.Error)?.message ?:
                               (answersResource as? Resource.Error)?.message ?: "Неизвестная ошибка загрузки данных."
                Toast.makeText(context, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                binding.btnAcknowledgeInstructions.text = "Ошибка загрузки"
                binding.btnAcknowledgeInstructions.isEnabled = false
            } else if (essentialDataAvailable) {
                binding.pbVariantDetailLoading.visibility = View.GONE
                binding.btnAcknowledgeInstructions.isEnabled = true
                binding.btnAcknowledgeInstructions.text = "Я прочитал(а) и готов(а) начать"
            } else {
                binding.pbVariantDetailLoading.visibility = View.VISIBLE
                binding.btnAcknowledgeInstructions.isEnabled = false
                binding.btnAcknowledgeInstructions.text = "Загрузка данных..."
            }
        }
    }

    private fun updateUiForCheckedState(
        userAnswers: Map<Int, UserVariantTaskAnswerEntity>,
        tasks: List<VariantTaskEntity>
    ) {
        val holder = contentHolder ?: return
        tasks.forEach { task ->
            if (task.egeNumber == "27") {
                val userAnswer = userAnswers[task.variantTaskId]
                val essayResultFromState = (variantViewModel.essayCheckState.value as? Resource.Success)?.data?.result
                val finalResult = essayResultFromState ?: userAnswer?.checkResult
                
                if (finalResult != null) {
                    holder.essayResultText?.visibility = View.VISIBLE
                    markwon?.setMarkdown(holder.essayResultText!!, finalResult)
                    holder.essayCheckButton?.text = "Сочинение проверено"
                }
            } else {
                val userAnswer = userAnswers[task.variantTaskId]
                val isCorrect = userAnswer?.isSubmissionCorrect
                val points = userAnswer?.pointsAwarded ?: 0
                val resultTextView = holder.resultTextViews[task.variantTaskId]
                val answerEditText = holder.answerEditTexts[task.variantTaskId]

                    if (isCorrect == true) {
                    answerEditText?.setBackgroundColor(getThemeColor(R.color.correct_answer_background_light))
                    resultTextView?.text = "Верно (+${points} балл(ов))"
                    resultTextView?.setTextColor(getThemeColor(R.color.correct_answer_green))
                    } else {
                    answerEditText?.setBackgroundColor(getThemeColor(R.color.incorrect_answer_background_light))
                    resultTextView?.text = "Неверно (${points} балл(ов))"
                    resultTextView?.setTextColor(getThemeColor(R.color.incorrect_answer_red))
                    }
            }
        }
    }

    private fun restoreFocus() {
        if (!currentIsChecked && lastFocusedTaskEditTextId != null) {
            Timber.d(TAG_VARIANT_DETAIL_BS, "Attempting to restore focus to EditText in task container with ID: $lastFocusedTaskEditTextId")
            
            val editTextToFocus = if (lastFocusedTaskEditTextId == contentHolder?.essayInput?.id) {
                 val essayTask = currentTasks?.find { it.egeNumber == "27" }
                 if (essayTask?.variantTaskId == lastFocusedTaskEditTextId) contentHolder?.essayInput else null
                    } else {
                contentHolder?.answerEditTexts?.get(lastFocusedTaskEditTextId)
            }

            editTextToFocus?.post {
                Timber.d(TAG_VARIANT_DETAIL_BS, "Executing posted requestFocus for task $lastFocusedTaskEditTextId.")
                editTextToFocus.requestFocus()
                        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.showSoftInput(editTextToFocus, InputMethodManager.SHOW_IMPLICIT)
                } ?: run {
                 Timber.w(TAG_VARIANT_DETAIL_BS, "Could not find EditText for task ID: $lastFocusedTaskEditTextId to restore focus.")
                }
        }
    }

    private fun getThemeColor(@ColorRes colorRes: Int): Int {
        return ContextCompat.getColor(requireContext(), colorRes)
    }

    private fun updatePauseState() {
        if (isPaused) {
            countDownTimer?.cancel()
            binding.nsvVariantSolvingArea.visibility = View.GONE
            binding.btnFinishVariant.visibility = View.GONE
            binding.btnTimerPauseResume.setImageResource(android.R.drawable.ic_media_play)
            Timber.d(TAG_VARIANT_DETAIL_BS, "Таймер на паузе. Осталось: $timeRemainingInMillis")
        } else {
            if (instructionsAcknowledged) {
                 binding.nsvVariantSolvingArea.visibility = View.VISIBLE
                 binding.btnFinishVariant.visibility = View.VISIBLE
            }
            binding.btnTimerPauseResume.setImageResource(android.R.drawable.ic_media_pause)
            startTimer()
            Timber.d(TAG_VARIANT_DETAIL_BS, "Таймер возобновлен.")
        }
    }

    private fun saveVariantResults(variantId: Int) {
        val tasks = currentTasks ?: return
        val userAnswers = currentUserAnswers ?: return
        val variantEntity = currentVariantEntity ?: return

        if (tasks.isEmpty()) {
            Timber.d(TAG_VARIANT_DETAIL_BS, "Нет заданий для сохранения результатов варианта")
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

    override fun onAnswerChanged(taskId: Int, answer: String) {
        if (!currentIsChecked) {
            currentVariantId?.let {
                variantViewModel.saveUserAnswerAndCheck(
                    variantId = it,
                    taskId = taskId,
                    answer = answer
                )
        }
        }
    }

    override fun onEssayAnswerChanged(answer: String) {
        val holder = contentHolder ?: return
        
        if (!essayIsChecked) {
            val wordCount = answer.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            holder.essayCharCount?.text = "Слов: $wordCount"
            val isLongEnough = wordCount >= 150
            holder.essayCheckButton?.isEnabled = isLongEnough
            binding.btnFinishVariant.isEnabled = answer.isBlank()
        }

        if (!currentIsChecked) {
            val essayTask = currentTasks?.find { it.egeNumber == "27" }
            essayTask?.let { task ->
                currentVariantId?.let { variantId ->
                    variantViewModel.saveUserAnswerAndCheck(
                        variantId = variantId,
                        taskId = task.variantTaskId,
                        answer = answer
                    )
                }
            }
        }
    }
    
    override fun onCheckEssayClicked(taskId: Int, sharedTextId: Int?, essayText: String) {
        variantViewModel.checkVariantEssay(taskId, sharedTextId, essayText)
    }

    override fun onTaskFocusChanged(taskId: Int, hasFocus: Boolean) {
        if (hasFocus) {
            lastFocusedTaskEditTextId = taskId
            Timber.d(TAG_VARIANT_DETAIL_BS, "Focus changed to task $taskId")
    }
}
}