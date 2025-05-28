package com.ruege.mobile.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PracticeStatisticItem(
    val id: String, 
    val egeDisplayNumber: String, 
    val totalAttempts: Int,
    val correctAttempts: Int,
    val successRate: Int,
    val lastAttemptDateFormatted: String 
) : Parcelable 