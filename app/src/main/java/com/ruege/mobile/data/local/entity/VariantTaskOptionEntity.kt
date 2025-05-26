package com.ruege.mobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "variant_task_options",
    foreignKeys = [
        ForeignKey(
            entity = VariantTaskEntity::class,
            parentColumns = ["variant_task_id"], // Связь с VariantTaskEntity по ее PK 'variant_task_id'
            childColumns = ["variant_task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["variant_task_id"])]
)
data class VariantTaskOptionEntity(
    @PrimaryKey // Если ID опции уникален глобально и приходит с сервера
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "variant_task_id")
    val variantTaskId: Int,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean,

    @ColumnInfo(name = "feedback")
    val feedback: String?,

    @ColumnInfo(name = "image_url")
    val imageUrl: String?
) 