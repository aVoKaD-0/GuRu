package com.ruege.mobile.model

import androidx.annotation.Keep

@Keep
data class ContentItem(
    val contentId: String,
    val title: String,
    val description: String?,
    val type: String,
    val parentId: String?,
    var isDownloaded: Boolean,
    var isSelected: Boolean,
    val isNew: Boolean = false
) 