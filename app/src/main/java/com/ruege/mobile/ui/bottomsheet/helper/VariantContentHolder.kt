package com.ruege.mobile.ui.bottomsheet.helper

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class VariantContentHolder {
    val answerEditTexts = mutableMapOf<Int, EditText>()
    val solutionContainers = mutableMapOf<Int, View>()
    val showSolutionButtons = mutableMapOf<Int, TextView>()
    val resultTextViews = mutableMapOf<Int, TextView>()
    val taskContainers = mutableMapOf<Int, View>()

    var essayInput: EditText? = null
    var essayCharCount: TextView? = null
    var essayCheckButton: Button? = null
    var essayResultText: TextView? = null
    var essayProgressBar: ProgressBar? = null
}
