package com.ruege.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность для кэширования метаданных о шпаргалках в локальной базе данных.
 */
@Entity(tableName = "shpargalki_cache")
data class ShpargalkaEntity(
    @PrimaryKey
    val id: Int, // Соответствует pdf_id с сервера
    val title: String,
    val description: String?,
    var isDownloaded: Boolean = false
) 