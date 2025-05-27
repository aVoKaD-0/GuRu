package com.ruege.mobile.model

/**
 * Модель шпаргалки для отображения в приложении
 */
data class ShpargalkaItem(
    val id: Int, 
    val title: String, 
    val description: String?,
    val groupId: String, 
    val groupTitle: String,
    val fileName: String?, 
    val publishTime: String? = null, 
    var isDownloaded: Boolean = false
)

/**
 * Модель группы шпаргалок
 */
data class ShpargalkaGroup(
    val id: String, 
    val title: String,
    val items: List<ShpargalkaItem>
) 