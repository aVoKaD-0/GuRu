package com.ruege.mobile.ui.bottomsheet.helper

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.ruege.mobile.R
import com.ruege.mobile.data.local.entity.UserVariantTaskAnswerEntity
import com.ruege.mobile.data.local.entity.VariantSharedTextEntity
import com.ruege.mobile.data.local.entity.VariantTaskEntity
import io.noties.markwon.Markwon
import java.util.regex.Pattern

class VariantContentBuilder(
    private val context: Context,
    private val markwon: Markwon,
    private val listeners: Listeners
) {

    interface Listeners {
        fun onAnswerChanged(taskId: Int, answer: String)
        fun onEssayAnswerChanged(answer: String)
        fun onCheckEssayClicked(taskId: Int, sharedTextId: Int?, essayText: String)
        fun onTaskFocusChanged(taskId: Int, hasFocus: Boolean)
    }

    fun buildContent(
        userAnswers: Map<Int, UserVariantTaskAnswerEntity>,
        tasks: List<VariantTaskEntity>,
        sharedTexts: List<VariantSharedTextEntity>,
        isChecked: Boolean,
        essayIsBeingChecked: Boolean,
        essayHasResult: Boolean
    ): Pair<LinearLayout, VariantContentHolder> {

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val holder = VariantContentHolder()
        val displayedSharedTextIds = mutableSetOf<Int>()

        var addedPart1Instruction = false
        var addedTask1_3Instruction = false
        var addedPart2Instruction = false

        val textIdForTasks22_26 = tasks.find {
            (it.egeNumber.startsWith("22") || it.egeNumber.startsWith("23") || it.egeNumber.startsWith("24") || it.egeNumber.startsWith("25") || it.egeNumber.startsWith("26")) && it.variantSharedTextId != null
        }?.variantSharedTextId

        if (!addedPart1Instruction) {
            val part1Title = createStyledTextView("Часть 1", styleResId = android.R.style.TextAppearance_Material_Title, isBold = true, isCentered = true, bottomMarginDp = 4)
            container.addView(part1Title)
            val part1Desc = createStyledTextView(
                "Ответами к заданиям 1-26 являются цифра (число), или слово (несколько слов), или последовательность цифр (чисел). Ответ запишите в поле ответа в тексте работы, а затем перенесите в БЛАНК ОТВЕТОВ № 1 справа от номера задания, начиная с первой клеточки, без пробелов, запятых и других дополнительных символов. Каждую букву или цифру пишите в отдельной клеточке в соответствии с приведёнными в бланке образцами.",
                styleResId = android.R.style.TextAppearance_Material_Body2,
                bottomMarginDp = 16
            )
            container.addView(part1Desc)
            addSeparator(container)
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
                if (tasks.any { it.variantSharedTextId == textEntity.variantSharedTextId && it.orderInVariant <= 3 && it.orderInVariant >= 1 } && !addedTask1_3Instruction) {
                    container.addView(createStyledTextView("Прочитайте текст и выполните задания 1-3.", isBold = true, styleResId = android.R.style.TextAppearance_Material_Subhead, bottomMarginDp = 8))
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
                container.addView(tvSharedText)
            }
        }

        val sortedTasks = tasks.sortedBy { it.orderInVariant }
        sortedTasks.forEach { taskEntity ->
            if (taskEntity.orderInVariant == 27 && !addedPart2Instruction) {
                addSeparator(container)
                container.addView(createStyledTextView("Часть 2", styleResId = android.R.style.TextAppearance_Material_Title, isBold = true, isCentered = true, topMarginDp = 16, bottomMarginDp = 4))
                container.addView(createStyledTextView("Для ответа на задание 27 используйте БЛАНК ОТВЕТОВ № 2.", styleResId = android.R.style.TextAppearance_Material_Body2, isCentered = true, bottomMarginDp = 16))
                addSeparator(container)
                addedPart2Instruction = true
            }

            val taskContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 8, 0, 8)
            }
            holder.taskContainers[taskEntity.variantTaskId] = taskContainer

            val taskFullTitle = SpannableString("Задание ${taskEntity.egeNumber ?: taskEntity.orderInVariant}. (${taskEntity.maxPoints} балл.) ${taskEntity.title ?: ""}")
            val tvTaskTitle = createStyledTextView(taskFullTitle, styleResId = android.R.style.TextAppearance_Material_Subhead, isBold = true, bottomMarginDp = 8)
            taskContainer.addView(tvTaskTitle)

            if (!taskEntity.taskStatement.isNullOrBlank()) {
                val taskStatementView: View = if (taskEntity.egeNumber == "8" || taskEntity.egeNumber == "22") {
                    createTaskTableLayout(context, taskEntity.taskStatement!!)
                } else {
                    createStyledTextView(taskEntity.taskStatement!!, styleResId = android.R.style.TextAppearance_Material_Body2, bottomMarginDp = 8)
                }
                taskContainer.addView(taskStatementView)
            }

            if (taskEntity.egeNumber == "27") {
                val essayContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val userAnswerEntity = userAnswers[taskEntity.variantTaskId]

                holder.essayInput = EditText(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        300.dpToPx()
                    ).apply {
                        setMargins(0, 8.dpToPx(), 0, 4.dpToPx())
                    }
                    gravity = android.view.Gravity.TOP
                    hint = "Введите ваше сочинение (не менее 150 слов)"
                    setText(userAnswerEntity?.userSubmittedAnswer ?: "")

                    isEnabled = !isChecked
                    isFocusable = !isChecked
                    isClickable = !isChecked
                    isFocusableInTouchMode = !isChecked

                    setOnFocusChangeListener { _, hasFocus ->
                        listeners.onTaskFocusChanged(taskEntity.variantTaskId, hasFocus)
                    }

                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            val text = s.toString()
                            listeners.onEssayAnswerChanged(text)
                        }
                    })
                }
                essayContainer.addView(holder.essayInput)

                holder.essayCharCount = createStyledTextView("", styleResId = android.R.style.TextAppearance_Material_Caption, bottomMarginDp = 8)
                essayContainer.addView(holder.essayCharCount)

                holder.essayCheckButton = Button(context).apply { text = "Проверить сочинение" }
                essayContainer.addView(holder.essayCheckButton)

                holder.essayProgressBar = ProgressBar(context).apply { visibility = View.GONE }
                essayContainer.addView(holder.essayProgressBar)

                holder.essayResultText = createStyledTextView("", bottomMarginDp = 8).apply { visibility = View.GONE }
                essayContainer.addView(holder.essayResultText)

                taskContainer.addView(essayContainer)

                holder.essayInput?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val text = s.toString()
                        listeners.onEssayAnswerChanged(text)
                    }
                })

                holder.essayCheckButton?.setOnClickListener {
                    val essayText = holder.essayInput?.text.toString()
                    listeners.onCheckEssayClicked(taskEntity.variantTaskId, taskEntity.variantSharedTextId, essayText)
                }
                
                if (essayHasResult) {
                     holder.essayInput?.isEnabled = false
                     holder.essayCheckButton?.isEnabled = false
                     holder.essayCheckButton?.text = "Сочинение проверено"
                } else if (essayIsBeingChecked) {
                    holder.essayInput?.isEnabled = false
                    holder.essayCheckButton?.isEnabled = false
                    holder.essayProgressBar?.visibility = View.VISIBLE
                    holder.essayResultText?.visibility = View.VISIBLE
                    holder.essayResultText?.text = "Идет проверка..."
                } else {
                    holder.essayInput?.isEnabled = !isChecked
                }
                if (isChecked) {
                    holder.essayInput?.isEnabled = false
                    holder.essayCheckButton?.visibility = View.GONE
                    holder.essayCharCount?.visibility = View.GONE
                }

            } else {
                val etAnswer = EditText(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    hint = "Введите ваш ответ"
                    val userAnswerEntity = userAnswers[taskEntity.variantTaskId]
                    setText(userAnswerEntity?.userSubmittedAnswer ?: "")

                    isEnabled = !isChecked
                    isFocusable = !isChecked
                    isClickable = !isChecked
                    isFocusableInTouchMode = !isChecked

                    setOnFocusChangeListener { _, hasFocus ->
                        listeners.onTaskFocusChanged(taskEntity.variantTaskId, hasFocus)
                    }

                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            if (!isChecked) {
                                listeners.onAnswerChanged(taskEntity.variantTaskId, s.toString())
                            }
                        }
                    })
                }
                taskContainer.addView(etAnswer)
                holder.answerEditTexts[taskEntity.variantTaskId] = etAnswer
            }

            if (isChecked) {
                if (taskEntity.egeNumber != "27") {
                    val resultTextView = TextView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        setPadding(8, 4, 8, 4)
                        textSize = 14f
                    }
                    taskContainer.addView(resultTextView)
                    holder.resultTextViews[taskEntity.variantTaskId] = resultTextView

                    val tvShowSolution = TextView(context).apply {
                        text = "Показать ответ и объяснение"
                        setTextColor(getThemeColor(R.color.show_answer_link_color))
                        setTypeface(null, Typeface.BOLD)
                        setPadding(8, 8, 8, 8)
                        isClickable = true
                        isFocusable = true
                    }
                    taskContainer.addView(tvShowSolution)
                    holder.showSolutionButtons[taskEntity.variantTaskId] = tvShowSolution

                    val solutionExplanationContainer = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        setPadding(8, 4, 8, 8)
                        setBackgroundColor(getThemeColor(R.color.solution_explanation_background_light))
                        visibility = View.GONE
                    }

                    if (!taskEntity.solutionText.isNullOrBlank()) {
                        val tvCorrectAnswer = TextView(context).apply {
                            text = "Правильный ответ: ${taskEntity.solutionText}"
                            setPadding(0, 0, 0, 4)
                        }
                        solutionExplanationContainer.addView(tvCorrectAnswer)
                    }

                    if (!taskEntity.explanationText.isNullOrBlank()) {
                        val tvExplanation = TextView(context).apply {
                            text = "Объяснение: ${taskEntity.explanationText}"
                        }
                        solutionExplanationContainer.addView(tvExplanation)
                    }

                    if (solutionExplanationContainer.childCount > 0) {
                        taskContainer.addView(solutionExplanationContainer)
                        tvShowSolution.setOnClickListener {
                            solutionExplanationContainer.visibility = if (solutionExplanationContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        }
                    } else {
                        tvShowSolution.visibility = View.GONE
                    }
                    holder.solutionContainers[taskEntity.variantTaskId] = solutionExplanationContainer
                }
            }

            container.addView(taskContainer)

            if (taskEntity.egeNumber.startsWith("22") && textIdForTasks22_26 != null) {
                val sharedTextForTasksAfter22 = sharedTexts.find { it.variantSharedTextId == textIdForTasks22_26 }
                if (sharedTextForTasksAfter22 != null && displayedSharedTextIds.add(textIdForTasks22_26)) {
                    addSeparator(container)

                    container.addView(
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
                    container.addView(tvSharedTextInjected)
                }
            }

            if (taskEntity != sortedTasks.lastOrNull()) {
                addSeparator(container)
            }
        }

        addSeparator(container)
        container.addView(createStyledTextView(
            "Не забудьте перенести все ответы в бланк ответов № 1 (и № 2 для сочинения) в соответствии с инструкцией по выполнению работы. Проверьте, чтобы каждый ответ был записан в строке с номером соответствующего задания.",
            styleResId = android.R.style.TextAppearance_Material_Body2,
            isBold = true,
            topMarginDp = 16,
            bottomMarginDp = 24
        ))

        return Pair(container, holder)
    }
    
    internal fun createStyledTextView(text: CharSequence, styleResId: Int = android.R.style.TextAppearance_Material_Body1, isBold: Boolean = false, isCentered: Boolean = false, topMarginDp: Int = 0, bottomMarginDp: Int = 8): TextView {
        return TextView(context).apply {
            this.text = text
            TextViewCompat.setTextAppearance(this, styleResId)
            if (isBold) this.typeface = Typeface.DEFAULT_BOLD
            if (isCentered) this.gravity = android.view.Gravity.CENTER_HORIZONTAL

            val scale = context.resources.displayMetrics.density
            val topMarginPx = (topMarginDp * scale + 0.5f).toInt()
            val bottomMarginPx = (bottomMarginDp * scale + 0.5f).toInt()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, topMarginPx, 0, bottomMarginPx)
            }
        }
    }

    internal fun addSeparator(container: LinearLayout) {
        val separator = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.dpToPx()
            ).apply {
                val margin = 8.dpToPx()
                setMargins(0, margin, 0, margin)
            }
            setBackgroundColor(getThemeColor(R.color.divider_light))
        }
        container.addView(separator)
    }

    internal fun formatFootnotes(text: String): SpannableString {
        val spannableString = SpannableString(text)
        val pattern = Pattern.compile("(\\s|\\[|\\()(\\d+)(\\]|\\))")
        val matcher = pattern.matcher(text)

        while (matcher.find()) {
            val startIndex = matcher.start(2)
            val endIndex = matcher.end(2)

            spannableString.setSpan(
                TextAppearanceSpan(context, R.style.FootnoteText),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannableString
    }

    internal fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    internal fun getThemeColor(@ColorRes colorRes: Int): Int {
        return ContextCompat.getColor(context, colorRes)
    }

    internal fun createTaskTableLayout(context: Context, statement: String): View {
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

    internal fun formatTextWithCurlyBraceHighlights(text: String): SpannableString {
        val displayText = text.replace(Regex("\\{([^}]+)\\}"), "$1")
        val spannableString = SpannableString(displayText)

        val pattern = Pattern.compile("\\{([^}]+)\\}")
        val matcher = pattern.matcher(text)

        var cumulativeOffset = 0

        while (matcher.find()) {
            val wordOnly = matcher.group(1)!!
            val startInDisplayText = matcher.start() - cumulativeOffset
            val endInDisplayText = startInDisplayText + wordOnly.length

            if (startInDisplayText >= 0 && endInDisplayText <= spannableString.length) {
                spannableString.setSpan(StyleSpan(Typeface.BOLD), startInDisplayText, endInDisplayText, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            cumulativeOffset += 2
        }
        return spannableString
    }
}
