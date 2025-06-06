package com.ruege.mobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "variants")
data class VariantEntity(
    @PrimaryKey
    @ColumnInfo(name = "variant_id")
    val variantId: Int,

    val name: String,
    val description: String?,

    @ColumnInfo(name = "is_official")
    val isOfficial: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: String, 

    @ColumnInfo(name = "updated_at")
    val updatedAt: String?, 

    @ColumnInfo(name = "task_count")
    val taskCount: Int,

    @ColumnInfo(name = "is_downloaded", defaultValue = "0") 
    val isDownloaded: Boolean = false,

    @ColumnInfo(name = "last_accessed_at")
    val lastAccessedAt: String? = null 
) 