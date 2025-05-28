package com.ruege.mobile.ui.exercises

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.ruege.mobile.R
import com.ruege.mobile.data.local.entity.TaskEntity
import com.ruege.mobile.model.AnswerType
import com.ruege.mobile.model.Solution
import com.ruege.mobile.model.TaskItem
import com.ruege.mobile.ui.viewmodel.ContentViewModel
import com.ruege.mobile.viewmodel.PracticeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * Фрагмент для отображения задания и ввода ответа пользователя.
 */
@AndroidEntryPoint
class TaskFragment : Fragment() {

    private val contentViewModel: ContentViewModel by viewModels()
    private val practiceViewModel: PracticeViewModel by viewModels()
    
    private lateinit var taskWebView: WebView
    private lateinit var answerCard: CardView
    private lateinit var radioGroup: RadioGroup
    private lateinit var checkboxContainer: RecyclerView
    private lateinit var textAnswerInput: EditText
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar
    
    // Карточка с результатом проверки
    private lateinit var resultCard: CardView
    private lateinit var resultIcon: TextView
    private lateinit var resultText: TextView
    private lateinit var explanationText: TextView
    private lateinit var nextButton: Button
    
    private var taskId: String? = null
    private var currentTask: TaskItem? = null
    
    // Адаптер для чекбоксов (для заданий с множественным выбором)
    private lateinit var checkboxAdapter: CheckboxAdapter
    
    companion object {
        private const val ARG_TASK_ID = "task_id"
        
        fun newInstance(taskId: String): TaskFragment {
            val fragment = TaskFragment()
            val args = Bundle()
            args.putString(ARG_TASK_ID, taskId)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getString(ARG_TASK_ID)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_task, container, false)
        
        // Инициализация UI-элементов
        taskWebView = view.findViewById(R.id.task_webview)
        answerCard = view.findViewById(R.id.answer_card)
        radioGroup = view.findViewById(R.id.radio_group)
        checkboxContainer = view.findViewById(R.id.checkbox_container)
        textAnswerInput = view.findViewById(R.id.text_answer_input)
        submitButton = view.findViewById(R.id.submit_button)
        progressBar = view.findViewById(R.id.progress_bar)
        
        resultCard = view.findViewById(R.id.result_card)
        resultIcon = view.findViewById(R.id.result_icon)
        resultText = view.findViewById(R.id.result_text)
        explanationText = view.findViewById(R.id.explanation_text)
        nextButton = view.findViewById(R.id.next_button)
        
        // Настройка RecyclerView для чекбоксов
        checkboxAdapter = CheckboxAdapter()
        checkboxContainer.layoutManager = LinearLayoutManager(requireContext())
        checkboxContainer.adapter = checkboxAdapter
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Подробный лог о загрузке фрагмента
        Timber.d("TaskFragment создан. ID задания: ${arguments?.getString("task_id")}")
        
        taskId?.let { id ->
            Timber.d("Загружаем задание с ID: $id")
            contentViewModel.loadTaskDetail(id)
        } ?: run {
            Timber.e("ID задания отсутствует!")
            Toast.makeText(requireContext(), "Ошибка: ID задания не указан", Toast.LENGTH_SHORT).show()
        }
        
        // Подробный лог о загрузке заданий
        Timber.d("TaskFragment: начинаем загрузку задания")
        
        // Наблюдение за состоянием загрузки
        contentViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })
        
        // Наблюдение за текущим заданием
        contentViewModel.taskContent.observe(viewLifecycleOwner, Observer { task ->
            setupTaskUI(task)
        })
        
        // Наблюдение за результатом проверки ответа
        contentViewModel.answerCheckResultLiveData.observe(viewLifecycleOwner, Observer { result ->
            result?.let {
                // Сохраняем результат попытки в PracticeViewModel
                currentTask?.let { task ->
                    // Создаем TaskEntity на основе данных из TaskItem
                    val taskEntity = TaskEntity(
                        task.taskId.toIntOrNull() ?: 0,        // id
                        extractEgeNumber(task.title),         // fipiId (используем egeNumber)
                        extractEgeNumber(task.title),         // egeNumber
                        task.content,                         // taskText
                        null,                                 // solution
                        null,                                 // explanation
                        "practice",                           // source
                        null,                                 // textId
                        task.answerType.name                  // taskType
                    )
                    
                    // Сохраняем попытку
                    practiceViewModel.saveAttempt(
                        taskEntity, 
                        it.isCorrect,
                        "practice", // source - источник попытки (практика)
                        task.answerType.name, // taskType - тип задания 
                        task.taskId // textId - идентификатор задания
                    )
                    
                    // Показываем результат пользователю
                    showResultCard(it.isCorrect, it.explanation ?: "", it.correctAnswer ?: "", it.userAnswer)
                }
            }
        })
        
        // Наблюдение за ошибками
        contentViewModel.errorMessage.observe(viewLifecycleOwner, Observer { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        })
        
        // Обработка нажатия на кнопку отправки ответа
        submitButton.setOnClickListener {
            submitAnswer()
        }
        
        // Обработка нажатия на кнопку "Следующее задание"
        nextButton.setOnClickListener {
            // Сбрасываем результат и переходим к списку заданий
            contentViewModel.clearAnswerResult()
            parentFragmentManager.popBackStack()
        }
    }
    
    /**
     * Настраивает UI в зависимости от типа задания.
     */
    private fun setupTaskUI(task: TaskItem?) {
        // Если задание null, выходим из метода
        if (task == null) {
            Timber.e("Получено null задание в setupTaskUI")
            return
        }
        
        // Сохраняем текущее задание
        currentTask = task
        
        // Загрузка HTML-контента в WebView
        val htmlContent = getFormattedHtml(task.content)
        taskWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        
        // Настройка формы ответа в зависимости от типа задания
        when (task.answerType) {
            AnswerType.SINGLE_CHOICE -> {
                setupSingleChoiceUI(task.solutions ?: emptyList())
            }
            AnswerType.MULTIPLE_CHOICE -> {
                setupMultipleChoiceUI(task.solutions ?: emptyList())
            }
            AnswerType.TEXT, AnswerType.NUMBER -> {
                setupTextInputUI(task.answerType == AnswerType.NUMBER)
            }
        }
    }
    
    /**
     * Настраивает UI для задания с одним вариантом ответа.
     */
    private fun setupSingleChoiceUI(options: List<Solution>) {
        radioGroup.visibility = View.VISIBLE
        checkboxContainer.visibility = View.GONE
        textAnswerInput.visibility = View.GONE
        
        // Очищаем группу радиокнопок
        radioGroup.removeAllViews()
        
        // Добавляем радиокнопки для каждого варианта
        options.forEach { option ->
            val radioButton = RadioButton(requireContext())
            radioButton.id = View.generateViewId()
            radioButton.text = option.text
            radioButton.tag = option.id
            radioGroup.addView(radioButton)
        }
    }
    
    /**
     * Настраивает UI для задания с несколькими вариантами ответа.
     */
    private fun setupMultipleChoiceUI(options: List<Solution>) {
        radioGroup.visibility = View.GONE
        checkboxContainer.visibility = View.VISIBLE
        textAnswerInput.visibility = View.GONE
        
        // Передаем опции в адаптер
        checkboxAdapter.setOptions(options)
    }
    
    /**
     * Настраивает UI для задания с текстовым или числовым ответом.
     */
    private fun setupTextInputUI(isNumberInput: Boolean) {
        radioGroup.visibility = View.GONE
        checkboxContainer.visibility = View.GONE
        textAnswerInput.visibility = View.VISIBLE
        
        // Настраиваем поле ввода в зависимости от типа
        if (isNumberInput) {
            textAnswerInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            textAnswerInput.hint = getString(R.string.enter_number_answer)
        } else {
            textAnswerInput.inputType = android.text.InputType.TYPE_CLASS_TEXT
            textAnswerInput.hint = getString(R.string.enter_text_answer)
        }
        
        // Очищаем поле ввода
        textAnswerInput.text.clear()
    }
    
    /**
     * Извлекает номер задания ЕГЭ из заголовка задания.
     * Например, из "Задание 1. Фонетика" извлекает "1".
     */
    private fun extractEgeNumber(title: String): String {
        // Ищем строку, которая начинается со слова "Задание" (или "задание"),
        // за которым следует пробел и цифра
        val regex = Regex("(?i)задание\\s+(\\d+)")
        val matchResult = regex.find(title)
        
        // Если найдено совпадение, возвращаем цифру, иначе возвращаем "0"
        return matchResult?.groups?.get(1)?.value ?: "0"
    }
    
    /**
     * Отправляет ответ пользователя на проверку.
     */
    private fun submitAnswer() {
        Timber.d("submitAnswer CALLED. taskId from arguments: $taskId")
        taskId?.let { idString ->
            val answer = when {
                radioGroup.visibility == View.VISIBLE -> {
                    // Для задания с одним вариантом ответа
                    val selectedRadioButtonId = radioGroup.checkedRadioButtonId
                    if (selectedRadioButtonId == -1) {
                        Toast.makeText(requireContext(), getString(R.string.select_answer), Toast.LENGTH_SHORT).show()
                        return
                    }
                    // Получаем ID выбранного ответа (хранится в tag)
                    view?.findViewById<RadioButton>(selectedRadioButtonId)?.tag?.toString() ?: ""
                }
                checkboxContainer.visibility == View.VISIBLE -> {
                    // Для задания с несколькими вариантами ответа
                    checkboxAdapter.getSelectedOptionIds().joinToString(",")
                }
                textAnswerInput.visibility == View.VISIBLE -> {
                    // Для задания с текстовым/числовым ответом
                    textAnswerInput.text.toString().trim()
                }
                else -> {
                    Timber.w("Не удалось определить тип ввода ответа")
                    ""
                }
            }
            
            // Проверяем, что ответ не пустой
            if (answer.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.enter_answer), Toast.LENGTH_SHORT).show()
                return
            }

            try {
                // Проверяем ответ через ContentViewModel
                val taskIdInt = idString.toInt()
                contentViewModel.checkAnswer(taskIdInt.toString(), answer)
            } catch (e: NumberFormatException) {
                Timber.e("Ошибка при парсинге ID задания: ${e.message}")
                Toast.makeText(requireContext(), "Ошибка при проверке ответа: неверный формат ID задания", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Timber.e("Невозможно отправить ответ: taskId is null")
            Toast.makeText(requireContext(), "Ошибка: ID задания неизвестен", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Показывает карточку с результатом проверки.
     */
    private fun showResultCard(
        isCorrect: Boolean,
        explanation: String,
        correctAnswer: String,
        userAnswer: String
    ) {
        Timber.d("showResultCard: isCorrect=$isCorrect, explanation='${explanation}', correctAnswer='$correctAnswer', userAnswer='$userAnswer'")
        // Показываем карточку с результатом
        resultCard.visibility = View.VISIBLE
        answerCard.visibility = View.GONE // Скрываем карточку с вводом ответа

        if (isCorrect) {
            resultIcon.text = "✓" // Галочка
            resultIcon.setTextColor(ContextCompat.getColor(requireContext(), R.color.correct_answer))
            resultText.text = getString(R.string.correct_answer)
        } else {
            resultIcon.text = "✗" // Крестик
            resultIcon.setTextColor(ContextCompat.getColor(requireContext(), R.color.incorrect_answer))
            resultText.text = "${getString(R.string.incorrect_answer)}\n${getString(R.string.your_answer)}: $userAnswer\n${getString(R.string.correct_answer)}: $correctAnswer"
        }

        // Показываем объяснение и кнопку "Объяснение" всегда, если объяснение есть
        if (explanation.isNotBlank()) {
            Timber.d("Explanation IS NOT BLANK, showing: '${explanation}'")
            explanationText.text = explanation // Само объяснение
            explanationText.visibility = View.VISIBLE
        } else {
            Timber.d("Explanation IS BLANK or NULL, hiding explanation text. Raw explanation value was: '${explanation}'")
            explanationText.visibility = View.GONE
        }
    }
    
    /**
     * Форматирует HTML-содержимое задания.
     */
    private fun getFormattedHtml(content: String): String {
        // Добавляем стили для контента
        val style = """
            <style>
                body {
                    font-family: 'Roboto', sans-serif;
                    margin: 0;
                    padding: 8px;
                    font-size: 16px;
                    line-height: 1.5;
                    color: #000000;
                    background-color: #FFFFFF;
                }
                p {
                    margin-bottom: 8px;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 16px 0;
                }
                table, th, td {
                    border: 1px solid #CCCCCC;
                }
                th, td {
                    padding: 8px;
                    text-align: left;
                }
                img {
                    max-width: 100%;
                    height: auto;
                }
                
                /* Стили для темной темы */
                @media (prefers-color-scheme: dark) {
                    body {
                        color: #FFFFFF;
                        background-color: #121212;
                    }
                    table, th, td {
                        border-color: #333333;
                    }
                }
            </style>
        """.trimIndent()
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                $style
            </head>
            <body>
                $content
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * Адаптер для отображения чекбоксов (для заданий с множественным выбором).
     */
    private inner class CheckboxAdapter : RecyclerView.Adapter<CheckboxAdapter.ViewHolder>() {
        private var options: List<Solution> = emptyList()
        private val selectedOptionsMap = mutableMapOf<String, Boolean>()
        
        fun setOptions(options: List<Solution>) {
            this.options = options
            selectedOptionsMap.clear()
            options.forEach { option ->
                selectedOptionsMap[option.id] = false
            }
            notifyDataSetChanged()
        }
        
        fun getSelectedOptionIds(): List<String> {
            return selectedOptionsMap.filter { it.value }.keys.toList()
        }
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val checkbox: MaterialCheckBox = itemView.findViewById(R.id.checkbox)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_checkbox, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val option = options[position]
            holder.checkbox.text = option.text
            holder.checkbox.isChecked = selectedOptionsMap[option.id] ?: false
            
            holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                selectedOptionsMap[option.id] = isChecked
            }
        }
        
        override fun getItemCount(): Int = options.size
    }
} 