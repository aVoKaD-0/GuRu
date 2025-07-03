package com.ruege.mobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "variant_tasks",
    foreignKeys = [
        ForeignKey(
            entity = VariantEntity::class,
            parentColumns = ["variant_id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VariantSharedTextEntity::class,
            parentColumns = ["variant_shared_text_id"],
            childColumns = ["variant_shared_text_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["variant_id"]),
        Index(value = ["variant_shared_text_id"])
    ]
)
data class VariantTaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "variant_task_id")
    val variantTaskId: Int,

    @ColumnInfo(name = "variant_id")
    val variantId: Int,

    @ColumnInfo(name = "original_task_id")
    val originalTaskId: Int?,

    @ColumnInfo(name = "variant_shared_text_id")
    val variantSharedTextId: Int?,

    @ColumnInfo(name = "ege_number")
    val egeNumber: String,

    @ColumnInfo(name = "order_in_variant")
    val orderInVariant: Int,

    val title: String,

    @ColumnInfo(name = "task_statement")
    val taskStatement: String,

    val difficulty: Int,

    @ColumnInfo(name = "max_points")
    val maxPoints: Int,

    @ColumnInfo(name = "task_type")
    val taskType: String,

    @ColumnInfo(name = "solution_text")
    val solutionText: String?,

    @ColumnInfo(name = "explanation_text")
    val explanationText: String?,

    @ColumnInfo(name = "time_limit")
    val timeLimit: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: String, 

    @ColumnInfo(name = "updated_at")
    val updatedAt: String,

    @ColumnInfo(name = "check_id")
    var checkId: String? = null
) 