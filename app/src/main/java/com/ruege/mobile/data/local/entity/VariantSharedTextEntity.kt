package com.ruege.mobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "variant_shared_texts",
    foreignKeys = [
        ForeignKey(
            entity = VariantEntity::class,
            parentColumns = ["variant_id"],
            childColumns = ["variant_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["variant_id"])]
)
data class VariantSharedTextEntity(
    @PrimaryKey
    @ColumnInfo(name = "variant_shared_text_id")
    val variantSharedTextId: Int,

    @ColumnInfo(name = "variant_id")
    val variantId: Int,

    @ColumnInfo(name = "text_content")
    val textContent: String,

    @ColumnInfo(name = "source_description")
    val sourceDescription: String,

    @ColumnInfo(name = "created_at")
    val createdAt: String, 

    @ColumnInfo(name = "updated_at")
    val updatedAt: String
) 