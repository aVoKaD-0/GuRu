package com.ruege.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_texts")
data class TaskTextEntity(
    @PrimaryKey
    val textId: String,
    val content: String
)

