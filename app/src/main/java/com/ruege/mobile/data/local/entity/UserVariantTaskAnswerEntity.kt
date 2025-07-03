package com.ruege.mobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_variant_task_answers",
    foreignKeys = [
        ForeignKey(
            entity = VariantTaskEntity::class,
            parentColumns = ["variant_task_id"],
            childColumns = ["variant_task_id"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = VariantEntity::class,
            parentColumns = ["variant_id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["variant_id"])
    ]
)
data class UserVariantTaskAnswerEntity(
    @PrimaryKey
    @ColumnInfo(name = "variant_task_id")
    val variantTaskId: Int,

    @ColumnInfo(name = "variant_id")
    val variantId: Int,

    @ColumnInfo(name = "user_submitted_answer")
    val userSubmittedAnswer: String?,

    @ColumnInfo(name = "is_submission_correct")
    val isSubmissionCorrect: Boolean?,

    @ColumnInfo(name = "points_awarded")
    val pointsAwarded: Int?,

    @ColumnInfo(name = "check_result")
    val checkResult: String? = null,

    @ColumnInfo(name = "answered_timestamp")
    val answeredTimestamp: String
) 