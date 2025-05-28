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
import com.ruege.mobile.data.local.entity.VariantSharedTextEntity
import com.ruege.mobile.data.local.entity.VariantTaskEntity
import com.ruege.mobile.data.local.entity.UserVariantTaskAnswerEntity
import com.ruege.mobile.utils.Resource
import com.ruege.mobile.viewmodel.VariantViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import android.content.DialogInterface

// private const val TAG = "VariantDetailBS" // Удаляем старый TAG
private const val ARG_VARIANT_ID = "variant_id"
private const val ARG_VARIANT_TITLE = "variant_title" // Добавляем для заголовка
private const val TIMER_DURATION_MS = (3 * 60 * 60 * 1000) + (55 * 60 * 1000).toLong() // 3 часа 55 минут

@AndroidEntryPoint
class VariantDetailBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG_VARIANT_DETAIL_BS = "VariantDetailBottomSheet_TAG" // Новый публичный TAG

        @JvmStatic
        fun newInstance(variantId: String, title: String?): VariantDetailBottomSheetDialogFragment { // Изменена сигнатура, ID теперь String
            val fragment = VariantDetailBottomSheetDialogFragment()
            val args = Bundle()
            // Поскольку currentVariantId у нас Int, а MainActivity передает String (после String.valueOf)
            // нужно решить, какой тип будет основным. Если ID в БД - Int, то здесь лучше принимать Int.
            // Если ID от API - String, то пусть будет String. Пока что будем конвертировать String в Int.
            try {
                args.putInt(ARG_VARIANT_ID, variantId.toInt())
            } catch (e: NumberFormatException) {
                Log.e(TAG_VARIANT_DETAIL_BS, "Error parsing variantId '$variantId' to Int", e)
                // Здесь можно обработать ошибку, например, показать Toast или закрыть диалог
            }
            args.putString(ARG_VARIANT_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }

    private val variantViewModel: VariantViewModel by activityViewModels()
    private var currentVariantId: Int? = null
    private var currentVariantTitle: String? = null
    // private var userAnswersMap: Map<Int, UserVariantTaskAnswerEntity>? = null // Больше не храним здесь, получаем из LiveData в checkAndPopulate

    // Views для инструкций
    private lateinit var svInstructionsArea: NestedScrollView
    private lateinit var btnAcknowledgeInstructions: Button

    // Views для решения варианта
    private lateinit var nsvVariantSolvingArea: NestedScrollView
    private lateinit var tvVariantName: TextView
    private lateinit var tvVariantDescription: TextView
    private lateinit var llDynamicContentContainer: LinearLayout
    private lateinit var btnFinishVariant: Button
    private lateinit var tvTimer: TextView
    
    private lateinit var pbVariantDetailLoading: ProgressBar // Общий ProgressBar

    private var countDownTimer: CountDownTimer? = null
    private var instructionsAcknowledged = false
    private val displayedSharedTextIds = mutableSetOf<Int>() // Для отслеживания отображенных общих текстов

    // Переменные для хранения текущих данных из LiveData, чтобы избежать частого доступа к .value
    private var currentVariantEntity: VariantEntity? = null
    private var currentSharedTexts: List<VariantSharedTextEntity>? = null
    private var currentTasks: List<VariantTaskEntity>? = null
    private var currentUserAnswers: Map<Int, UserVariantTaskAnswerEntity>? = null
    private var currentIsChecked: Boolean = false
    private var lastFocusedTaskEditTextId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // currentVariantId = it.getInt(ARG_VARIANT_ID) // Было
            if (it.containsKey(ARG_VARIANT_ID)) {
                currentVariantId = it.getInt(ARG_VARIANT_ID)
            } else {
                Log.e(TAG_VARIANT_DETAIL_BS, "ARG_VARIANT_ID not found in arguments")
                // Обработка случая, когда ID не передан или ключ некорректен
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
        
        // Инициализация Views
        pbVariantDetailLoading = view.findViewById<ProgressBar>(R.id.pb_variant_detail_loading)
        
        svInstructionsArea = view.findViewById<NestedScrollView>(R.id.sv_instructions_area)
        btnAcknowledgeInstructions = view.findViewById<Button>(R.id.btn_acknowledge_instructions)
        
        nsvVariantSolvingArea = view.findViewById<NestedScrollView>(R.id.nsv_variant_solving_area)
        tvVariantName = view.findViewById<TextView>(R.id.tv_variant_name_bs)
        tvVariantDescription = view.findViewById<TextView>(R.id.tv_variant_description_bs)
        llDynamicContentContainer = view.findViewById<LinearLayout>(R.id.ll_dynamic_content_container)
        btnFinishVariant = view.findViewById<Button>(R.id.btn_finish_variant)
        tvTimer = view.findViewById<TextView>(R.id.tv_timer)
        
        // Начальное состояние UI
        svInstructionsArea.visibility = View.VISIBLE
        nsvVariantSolvingArea.visibility = View.GONE
        tvTimer.visibility = View.GONE
        pbVariantDetailLoading.visibility = View.VISIBLE // Показываем загрузку, пока основные данные не пришли

        // Добавляем обработчик касаний для скрытия клавиатуры
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
            false // Не перехватываем событие полностью, чтобы другие слушатели могли сработать
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
                behavior.peekHeight = windowHeight // Or a very large value
                behavior.isFitToContents = false
                behavior.skipCollapsed = true
                 behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_DRAGGING && behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                           // Optional: Prevent dragging down from expanded state if truly fixed fullscreen needed
                           // behavior.state = BottomSheetBehavior.STATE_EXPANDED
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
        observeViewModel() // Наблюдение за ViewModel начнется сразу

        currentVariantId = arguments?.getInt(ARG_VARIANT_ID)
        if (currentVariantId == null) {
            Log.e(TAG_VARIANT_DETAIL_BS, "Variant ID is null, cannot load details.")
            Toast.makeText(requireContext(), "Ошибка: ID варианта не найден", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        // Слушатель для кнопки подтверждения прочтения инструкций
        btnAcknowledgeInstructions.setOnClickListener {
            instructionsAcknowledged = true
            svInstructionsArea.visibility = View.GONE
            // pbVariantDetailLoading остается, если данные еще грузятся
            // nsvVariantSolvingArea и tvTimer станут видимы через checkAndPopulate, когда данные будут готовы
            Log.d(TAG_VARIANT_DETAIL_BS, "Инструкции подтверждены, вызываем checkAndPopulate")
            checkAndPopulate(forceRepopulate = true) // Перерисовываем с учетом нового состояния
        }
        
        btnFinishVariant.setOnClickListener {
            currentVariantId?.let {
                Log.d(TAG_VARIANT_DETAIL_BS, "Нажата кнопка 'Завершить вариант' для variantId: $it")
                variantViewModel.checkVariantAnswers(it) 
            } ?: Log.e(TAG_VARIANT_DETAIL_BS, "Невозможно завершить вариант, currentVariantId is null")
        }
    }

    private fun startTimer() {
        if (countDownTimer != null) return // Уже запущен
        if (!instructionsAcknowledged || variantViewModel.variantCheckedState.value) {
             Log.d(TAG_VARIANT_DETAIL_BS, "Таймер не запущен: инструкции не подтверждены (${instructionsAcknowledged}) или вариант уже проверен (${variantViewModel.variantCheckedState.value})")
            return // Не запускаем таймер, если инструкции не подтверждены или вариант проверен
        }
        tvTimer.visibility = View.VISIBLE
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(TIMER_DURATION_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            }

            override fun onFinish() {
                tvTimer.text = "00:00:00"
                // TODO: Добавить логику по завершению времени (например, авто-завершение варианта)
                Toast.makeText(requireContext(), "Время вышло!", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        currentVariantId?.let {
            Log.d(TAG_VARIANT_DETAIL_BS, "onDestroyView: Synchronizing answers for variantId: $it")
            variantViewModel.synchronizeAnswersWithServer(it)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        Log.d(TAG_VARIANT_DETAIL_BS, "onDismiss called, consuming variant details.")
        variantViewModel.consumeVariantDetails() // Сбрасываем состояние ViewModel
        // Также останавливаем таймер, если он был запущен
        countDownTimer?.cancel()
        countDownTimer = null
        Log.d(TAG_VARIANT_DETAIL_BS, "Timer cancelled in onDismiss.")
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
            // Вызываем checkAndPopulate всегда, когда обновляются ответы пользователя,
            // так как это последний из основных блоков данных, который может прийти.
            checkAndPopulate()
        }

        variantViewModel.variantCheckedState.asLiveData().observe(viewLifecycleOwner) { isChecked ->
            Log.d(TAG_VARIANT_DETAIL_BS, "Observed variantCheckedState: $isChecked")
            currentIsChecked = isChecked
            if (isChecked) {
                tvTimer.visibility = View.GONE
                countDownTimer?.cancel()
                btnFinishVariant.isEnabled = false
                btnAcknowledgeInstructions.visibility = View.GONE // Если вариант проверен, инструкции не нужны
                Log.d(TAG_VARIANT_DETAIL_BS, "Variant is checked, calling checkAndPopulate to show results.")
                checkAndPopulate(forceRepopulate = true) // Обновляем UI, чтобы показать результаты

                // СИНХРОНИЗАЦИЯ ПРИ ЗАВЕРШЕНИИ ВАРИАНТА
                currentVariantId?.let { variantId ->
                    Log.d(TAG_VARIANT_DETAIL_BS, "Variant $variantId is checked. Synchronizing answers with server.")
                    variantViewModel.synchronizeAnswersWithServer(variantId)
                }

            } else {
                // Если состояние сброшено на непроверенное (например, при загрузке нового варианта)
                // checkAndPopulate() сам обработает состояние кнопки и таймера.
                checkAndPopulate()
            }
        }
    }
    
    // Переименована и изменена логика checkAndPopulate
    private fun checkAndPopulate(forceRepopulate: Boolean = false) {
        Log.d(TAG_VARIANT_DETAIL_BS, "checkAndPopulate called. Force: $forceRepopulate, Ack: $instructionsAcknowledged, Checked: $currentIsChecked")

        val variant = currentVariantEntity
        val texts = currentSharedTexts
        val tasks = currentTasks
        val answersMap = currentUserAnswers

        val variantResource = variantViewModel.variantDetailsLiveData.value // для проверки состояния загрузки
        val textsResource = variantViewModel.sharedTextsLiveData.value
        val tasksResource = variantViewModel.tasksLiveData.value
        val answersResource = variantViewModel.userAnswersForCurrentVariantLiveData.value

        // Все основные данные ДОЛЖНЫ БЫТЬ ЗАГРУЖЕНЫ (не null) для отображения контента решения
        val essentialDataAvailable = variant != null && texts != null && tasks != null && answersMap != null
        
        // Лог перед основной логикой доступности кнопки - ПЕРЕМЕЩЕН ПОСЛЕ ОБЪЯВЛЕНИЯ essentialDataAvailable
        Log.d(TAG_VARIANT_DETAIL_BS, "checkAndPopulate - Conditions: currentIsChecked=$currentIsChecked, tasks.size=${tasks?.size}, essentialDataAvailable=$essentialDataAvailable, instructionsAcknowledged=$instructionsAcknowledged")

        if (instructionsAcknowledged) {
            if (essentialDataAvailable) {
                pbVariantDetailLoading.visibility = View.GONE
                nsvVariantSolvingArea.visibility = View.VISIBLE
                llDynamicContentContainer.visibility = View.VISIBLE
                
                populateDynamicContent(variant!!, texts!!, tasks!!, answersMap!!, currentIsChecked)

                if (currentIsChecked) {
                    btnFinishVariant.isEnabled = false
                    btnFinishVariant.text = "Вариант проверен"
                    countDownTimer?.cancel()
                    tvTimer.visibility = View.GONE
                    disableAnswerFields()
                } else {
                    btnFinishVariant.isEnabled = true // Может быть переопределено ниже, если вариант пуст
                    btnFinishVariant.text = "Завершить вариант"
                    if (tasks?.isNotEmpty() == true) { // Запускаем таймер только если есть задания
                        startTimer()
                    } else {
                        tvTimer.visibility = View.GONE
                        btnFinishVariant.isEnabled = false // Нельзя завершить пустой вариант
                    }
                }
            } else {
                // Данные еще не загружены или ошибка, но инструкции подтверждены
                pbVariantDetailLoading.visibility = View.VISIBLE
                nsvVariantSolvingArea.visibility = View.GONE
                tvTimer.visibility = View.GONE
                
                // Проверяем, есть ли явные ошибки в ресурсах
                if (variantResource is Resource.Error || textsResource is Resource.Error || tasksResource is Resource.Error || answersResource is Resource.Error) {
                    pbVariantDetailLoading.visibility = View.GONE // Ошибка важнее прогресса
                    val errorMsg = (variantResource as? Resource.Error)?.message ?:
                                   (textsResource as? Resource.Error)?.message ?:
                                   (tasksResource as? Resource.Error)?.message ?:
                                   (answersResource as? Resource.Error)?.message ?: "Неизвестная ошибка загрузки данных."
                    showErrorStateInSolvingArea(errorMsg)
                } else {
                     Log.d(TAG_VARIANT_DETAIL_BS, "Данные для решения варианта еще грузятся, инструкции подтверждены.")
                     // Можно показать заглушку "Загрузка..." в nsvVariantSolvingArea, если нужно
                     // llDynamicContentContainer.removeAllViews() // Очистить старое
                     // llDynamicContentContainer.addView(createStyledTextView("Загрузка данных варианта...", isCentered = true))
                }
            }
        } else { // Инструкции не подтверждены
            svInstructionsArea.visibility = View.VISIBLE
            nsvVariantSolvingArea.visibility = View.GONE
            tvTimer.visibility = View.GONE

            if (essentialDataAvailable) { // Данные загружены, можно начать
                pbVariantDetailLoading.visibility = View.GONE
                btnAcknowledgeInstructions.isEnabled = true
                btnAcknowledgeInstructions.text = "Я прочитал(а) и готов(а) начать"
            } else { // Данные еще грузятся или ошибка
                 if (variantResource is Resource.Error || textsResource is Resource.Error || tasksResource is Resource.Error || answersResource is Resource.Error) {
                    pbVariantDetailLoading.visibility = View.GONE
                     // Показываем ошибку прямо на экране инструкций или общую
                     Toast.makeText(context, "Не удалось загрузить данные варианта. Попробуйте позже.", Toast.LENGTH_LONG).show()
                     btnAcknowledgeInstructions.text = "Ошибка загрузки"
                     btnAcknowledgeInstructions.isEnabled = false
                 } else {
                    pbVariantDetailLoading.visibility = View.VISIBLE
                    btnAcknowledgeInstructions.isEnabled = false
                    btnAcknowledgeInstructions.text = "Загрузка данных..."
                 }
            }
        }
    }

    private fun showErrorStateInSolvingArea(message: String) {
        nsvVariantSolvingArea.visibility = View.VISIBLE // Показываем область решения, чтобы там отобразить ошибку
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
    }

    private fun disableAnswerFields() {
        for (i in 0 until llDynamicContentContainer.childCount) {
            val view = llDynamicContentContainer.getChildAt(i)
            if (view is ViewGroup && view.tag is Int) { // Ищем контейнер задания
                // Ищем EditText внутри контейнера задания
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
        // Если контейнер уже отрисован и мы не в режиме проверки результатов (т.е. isChecked == false),
        // то не перерисовываем всё полностью, чтобы не терять фокус EditText.
        // Предполагается, что EditText сам содержит актуальный текст, введенный пользователем.
        // Обновления userAnswers (например, с сервера при первоначальной загрузке)
        // будут учтены при первой полной отрисовке (когда childCount == 0).
        if (llDynamicContentContainer.childCount > 0 && !isChecked) {
            Log.d(TAG_VARIANT_DETAIL_BS, "populateDynamicContent: Skipping full redraw for ongoing input. Child count: ${llDynamicContentContainer.childCount}, isChecked: $isChecked")
            // ВАЖНО: Если бы нам нужно было ОБНОВИТЬ какие-то ДРУГИЕ части UI (не EditText)
            // на основе изменений в userAnswers, пока isChecked=false, это нужно было бы делать здесь
            // точечно, без removeAllViews(). Но сейчас такой потребности нет,
            // так как основная проблема - потеря фокуса EditText.
            return // Выходим, чтобы не перерисовывать
        }

        // Если мы здесь, значит, это либо первая отрисовка, либо isChecked=true (показываем результаты),
        // либо была запрошена принудительная перерисовка, требующая очистки.
        llDynamicContentContainer.removeAllViews()
        displayedSharedTextIds.clear() // Очищаем перед заполнением
        Log.d(TAG_VARIANT_DETAIL_BS, "populateDynamicContent: Performing full redraw. isChecked: $isChecked. SharedTexts: ${sharedTexts.size}, Tasks: ${tasks.size}, Answers: ${userAnswers.size}")
        
        // --- Инструкции внутри варианта --- 
        var addedPart1Instruction = false
        var addedTask1_3Instruction = false
        var addedPart2Instruction = false // Эта переменная относится к Части 2 (задание 27) и должна остаться
        
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

        // Сначала отобразим все уникальные общие тексты, которые используются в заданиях,
        // КРОМЕ текста для заданий 23-26, который будет вставлен после задания 22.
        val uniqueTextIdsInTasks = tasks.mapNotNull { it.variantSharedTextId }.distinct()
        val textsToDisplay = sharedTexts.filter { uniqueTextIdsInTasks.contains(it.variantSharedTextId) }
                                      .sortedBy { it.variantSharedTextId } 

        textsToDisplay.forEach { textEntity ->
            // Текст для заданий 23-26 (textIdForTasks22_26) будет отображен отдельно, после задания 22.
            if (textEntity.variantSharedTextId == textIdForTasks22_26) {
                return@forEach // Пропускаем этот текст здесь
            }

            if (displayedSharedTextIds.add(textEntity.variantSharedTextId)) { 
                // Инструкция перед текстом для заданий 1-3 (если текст используется для них)
                if (tasks.any { it.variantSharedTextId == textEntity.variantSharedTextId && it.orderInVariant <= 3 && it.orderInVariant >=1 } && !addedTask1_3Instruction) {
                    llDynamicContentContainer.addView(createStyledTextView("Прочитайте текст и выполните задания 1-3.", isBold = true, styleResId = android.R.style.TextAppearance_Material_Subhead, bottomMarginDp = 8))
                    addedTask1_3Instruction = true
                }
                
                // Отображаем сам общий текст
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
            // Инструкция к Части 2 перед 27 заданием
            if (taskEntity.orderInVariant == 27 && !addedPart2Instruction) {
                addSeparator(llDynamicContentContainer)
                llDynamicContentContainer.addView(createStyledTextView("Часть 2", styleResId = android.R.style.TextAppearance_Material_Title, isBold = true, isCentered = true, topMarginDp = 16, bottomMarginDp = 4))
                llDynamicContentContainer.addView(createStyledTextView("Для ответа на задание 27 используйте БЛАНК ОТВЕТОВ № 2.", styleResId = android.R.style.TextAppearance_Material_Body2, isCentered = true, bottomMarginDp = 16))
                addSeparator(llDynamicContentContainer)
                addedPart2Instruction = true // Корректное использование addedPart2Instruction
            }

            val taskContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 8, 0, 8) // Уменьшил отступы для контейнера задания
                tag = taskEntity.variantTaskId 
            }

            val taskFullTitle = SpannableString("Задание ${taskEntity.egeNumber ?: taskEntity.orderInVariant}. (${taskEntity.maxPoints} балл.) ${taskEntity.title ?: ""}")
            // Можно добавить стили для частей заголовка, если нужно
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
                id = View.generateViewId() // Генерируем ID для EditText
                // Восстанавливаем ранее введенный ответ
                val userAnswerEntity = userAnswers[taskEntity.variantTaskId]
                setText(userAnswerEntity?.userSubmittedAnswer ?: "")
                
                isEnabled = !isChecked // Блокируем, если вариант проверен
                isFocusable = !isChecked
                isClickable = !isChecked
                isFocusableInTouchMode = !isChecked

                // Отслеживаем фокус
                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        lastFocusedTaskEditTextId = taskEntity.variantTaskId
                        Log.d(TAG_VARIANT_DETAIL_BS, "EditText for task ${taskEntity.variantTaskId} (viewId: ${this.id}) gained focus. lastFocusedTaskEditTextId set to ${taskEntity.variantTaskId}.")
                    }
                }

                // Сохранение ответа пользователя при изменении текста
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (!isChecked) { // Сохраняем только если вариант не проверен
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
                    etAnswer.setBackgroundColor(getThemeColor(R.color.correct_answer_background_light)) // Use light version, dark will be picked by system
                    resultTextView.text = "Верно (+${points} балл(ов))"
                    resultTextView.setTextColor(getThemeColor(R.color.correct_answer_green))
                } else {
                    etAnswer.setBackgroundColor(getThemeColor(R.color.incorrect_answer_background_light)) // Use light version, dark will be picked by system
                    resultTextView.text = "Неверно (${points} балл(ов))"
                    resultTextView.setTextColor(getThemeColor(R.color.incorrect_answer_red))
                }
                taskContainer.addView(resultTextView)

                // Кнопка/текст для показа правильного ответа и объяснения
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
                    setBackgroundColor(getThemeColor(R.color.solution_explanation_background_light)) // Use light version, dark will be picked by system
                    visibility = View.GONE // По умолчанию скрыто
                    id = View.generateViewId()
                }
                
                if (!taskEntity.solutionText.isNullOrBlank()) {
                    val tvCorrectAnswer = TextView(requireContext()).apply {
                        text = "Правильный ответ: ${taskEntity.solutionText}"
                        // textAppearance = R.style.TextAppearance_MaterialComponents_Body1
                        setPadding(0,0,0,4)
                        id = View.generateViewId()
                    }
                    solutionExplanationContainer.addView(tvCorrectAnswer)
                }

                if (!taskEntity.explanationText.isNullOrBlank()) {
                    val tvExplanation = TextView(requireContext()).apply {
                        text = "Объяснение: ${taskEntity.explanationText}"
                        // textAppearance = R.style.TextAppearance_MaterialComponents_Body1
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
                     tvShowSolution.visibility = View.GONE // Скрываем кнопку, если нечего показывать
                }
            }

            llDynamicContentContainer.addView(taskContainer)

            // --- Вставка текста для заданий 23-26 ПОСЛЕ задания 22 ---
            if (taskEntity.egeNumber.startsWith("22") && textIdForTasks22_26 != null) {
                val sharedTextForTasksAfter22 = sharedTexts.find { it.variantSharedTextId == textIdForTasks22_26 }
                if (sharedTextForTasksAfter22 != null && displayedSharedTextIds.add(textIdForTasks22_26)) { // Проверяем, что еще не добавлен
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
            // --- Конец вставки ---

            // Добавляем разделитель после каждого задания (и вставленного блока текста), кроме последнего элемента в общем потоке
            if (taskEntity != sortedTasks.lastOrNull()) { 
                // Если текущий элемент - задание 22 И текст для 23-26 был вставлен,
                // И это задание 22 не является последним в общем списке заданий,
                // то разделитель после текста уже как бы "внутри" блока задания 22.
                // Следующий разделитель должен быть после следующего задания (23 и т.д.)
                // Эта логика может потребовать небольшой доработки, чтобы избежать двойных разделителей или их отсутствия.
                // Пока оставляем как есть, addSeparator(llDynamicContentContainer) сработает.
                 addSeparator(llDynamicContentContainer)
            }
            lastDisplayedOrder = taskEntity.orderInVariant
        }

        // Финальное предупреждение
        addSeparator(llDynamicContentContainer)
        llDynamicContentContainer.addView(createStyledTextView(
            "Не забудьте перенести все ответы в бланк ответов № 1 (и № 2 для сочинения) в соответствии с инструкцией по выполнению работы. Проверьте, чтобы каждый ответ был записан в строке с номером соответствующего задания.",
            styleResId = android.R.style.TextAppearance_Material_Body2, 
            isBold = true, 
            topMarginDp = 16, 
            bottomMarginDp = 24 // Увеличил нижний отступ
        ))
        
        Log.d(TAG_VARIANT_DETAIL_BS, "populateDynamicContent finished. Children in llDynamicContentContainer: ${llDynamicContentContainer.childCount}")

        // Восстановление фокуса после ПОЛНОЙ перерисовки UI (например, при первой загрузке или при isChecked=true)
        // Этот блок теперь будет иметь смысл только если populateDynamicContent действительно перерисовал все.
        // Если мы вышли раньше из-за (llDynamicContentContainer.childCount > 0 && !isChecked), фокус и не должен был теряться.
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
            // Очищать lastFocusedTaskEditTextId здесь не будем, так как если пользователь продолжит ввод
            // в том же поле, оно должно остаться. Он сбросится при фокусе на другом поле.
        }
    }

    // Вспомогательные функции для создания View
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
            id = View.generateViewId() // Генерируем ID, чтобы избежать проблем с некоторыми UI тестами или другими операциями
        }
    }
    
    private fun addSeparator(container: LinearLayout) {
        val separator = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.dpToPx() // высота разделителя 1dp
            ).apply {
                val margin = 8.dpToPx() // отступы по 8dp сверху и снизу
                setMargins(0, margin, 0, margin)
            }
            setBackgroundColor(getThemeColor(R.color.divider_light)) // Use light version, dark will be picked by system
            id = View.generateViewId()
        }
        container.addView(separator)
    }

    private fun formatFootnotes(text: String): SpannableString {
        val spannableString = SpannableString(text)
        // Regex to find numbers in parentheses, e.g., (1), (12), [1], [12]
        // Также обрабатываем случаи типа "текст(1)" или "текст[1]" без пробела
        val pattern = Pattern.compile("(\\s|\\[|\\()(\\d+)(\\]|\\))")
        val matcher = pattern.matcher(text)

        while (matcher.find()) {
            val startIndex = matcher.start(2) // Начало только цифр
            val endIndex = matcher.end(2)   // Конец только цифр

            // Уменьшаем размер текста для цифр
            spannableString.setSpan(
                TextAppearanceSpan(requireContext(), R.style.FootnoteText),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannableString
    }

    // Extension function для конвертации dp в px
    fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun getThemeColor(@ColorRes colorRes: Int): Int {
        return ContextCompat.getColor(requireContext(), colorRes)
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

            if (!switchedToColumn2 && line.matches(Regex("^[А-ЯЁ]\\s*\\).*"))) { // A) B) C) etc.
                column1Items.add(line)
            } else if (line.matches(Regex("^\\d+\\s*\\).*"))) { // 1) 2) 3) etc.
                switchedToColumn2 = true
                column2Items.add(line)
            } else if (switchedToColumn2) {
                if (column2Items.isNotEmpty()) {
                    column2Items[column2Items.size - 1] = column2Items.last() + "\n" + line
                } else {
                     column2Items.add(line) // Should only happen if first line of col2 is not numbered (unlikely for task 8/22)
                }
            } else {
                if (column1Items.isNotEmpty()) {
                    column1Items[column1Items.size - 1] = column1Items.last() + "\n" + line
                } else {
                    column1Items.add(line) // First line, doesn't match A) pattern
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
            // Fallback: if parsing failed, show original statement in one column
             val fallbackTextView = createStyledTextView(statement, styleResId = android.R.style.TextAppearance_Material_Body2, bottomMarginDp = 8)
             mainLayout.addView(fallbackTextView) // Add directly to mainLayout
        } else {
            if (column1Layout.childCount > 0) horizontalLayout.addView(column1Layout)
            if (column2Layout.childCount > 0) horizontalLayout.addView(column2Layout)
            if (horizontalLayout.childCount > 0) mainLayout.addView(horizontalLayout)
        }
        
        return mainLayout
    }

    private fun formatTextWithCurlyBraceHighlights(text: String): SpannableString {
        Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Input text: [$text]")
        // Текст для отображения (без фигурных скобок)
        val displayText = text.replace(Regex("\\{([^}]+)\\}"), "$1")
        Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Display text (after replace): [$displayText]")
        val spannableString = SpannableString(displayText)
    
        // Паттерн для поиска слов в фигурных скобках в оригинальном тексте
        val pattern = Pattern.compile("\\{([^}]+)\\}")
        val matcher = pattern.matcher(text) // Ищем в оригинальном тексте
    
        var cumulativeOffset = 0 // Накопленное смещение из-за удаления скобок
        var matchesFound = 0
    
        while (matcher.find()) {
            matchesFound++
            val wordInBraces = matcher.group(0)!! // Например, "{слово}"
            val wordOnly = matcher.group(1)!!     // Например, "слово"
            Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Match $matchesFound: wordInBraces='${wordInBraces}', wordOnly='${wordOnly}'")
    
            // Начальная позиция слова (без скобок) в displayText
            // matcher.start() - это начало "{слово}" в оригинальном тексте
            // cumulativeOffset - это количество символов ({ и }), удаленных до этого момента
            val startInDisplayText = matcher.start() - cumulativeOffset
            val endInDisplayText = startInDisplayText + wordOnly.length
            Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Calculated indices for displayText: start=$startInDisplayText, end=$endInDisplayText (cumulativeOffset=$cumulativeOffset)")
    
            if (startInDisplayText >= 0 && endInDisplayText <= spannableString.length) {
                spannableString.setSpan(StyleSpan(Typeface.BOLD), startInDisplayText, endInDisplayText, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Applied BOLD span to '${spannableString.substring(startInDisplayText, endInDisplayText)}'")
            } else {
                Log.w(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Invalid indices, skipping span for word '$wordOnly'")
            }
            cumulativeOffset += 2 // Каждая пара {} удаляет 2 символа
        }
        if (matchesFound == 0) {
            Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - No matches found for {} pattern.")
        }
        Log.d(TAG_VARIANT_DETAIL_BS, "formatTextWithCurlyBraceHighlights - Returning spannable: $spannableString")
        return spannableString
    }
}
// Helper extension function for Context to avoid nullable context
// fun Context.dpToPx(dp: Int): Int {
//     return (dp * resources.displayMetrics.density).toInt()
// }
// Note: createStyledTextView already handles dp to px conversion
 