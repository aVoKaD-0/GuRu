package com.ruege.mobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_essays")
data class UserEssayEntity(
    @PrimaryKey
    @ColumnInfo(name = "task_id")
    val taskId: String,

    @ColumnInfo(name = "essay_content")
    var essayContent: String?,

    @ColumnInfo(name = "result")
    var result: String?,

    @ColumnInfo(name = "check_id")
    var checkId: String? = null
) 