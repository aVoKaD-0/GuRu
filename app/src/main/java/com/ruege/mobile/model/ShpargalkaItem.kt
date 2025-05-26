package com.ruege.mobile.model

/**
 * Модель шпаргалки для отображения в приложении
 */
data class ShpargalkaItem(
    val id: Int, // ID шпаргалки
    val title: String, // Название шпаргалки
    val description: String?, // Описание шпаргалки
    val groupId: String, // ID группы, к которой относится шпаргалка
    val groupTitle: String, // Название группы
    val fileName: String?, // Имя файла шпаргалки
    val publishTime: String? = null, // Время публикации
    var isDownloaded: Boolean = false // Флаг, скачана ли шпаргалка
)

/**
 * Модель группы шпаргалок
 */
data class ShpargalkaGroup(
    val id: String, // ID группы
    val title: String, // Название группы
    val items: List<ShpargalkaItem> // Список шпаргалок в группе
) 