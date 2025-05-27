package com.ruege.mobile.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PracticeAttemptItemUiModel(
    val attemptId: Long, 
    val taskId: Int,     
    val egeTaskNumberDisplay: String, 
    val attemptDateFormatted: String,
    val isCorrect: Boolean
) : Parcelable 