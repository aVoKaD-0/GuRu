package com.ruege.mobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(tableName = "variants")
class VariantEntity(
    @PrimaryKey
    @ColumnInfo(name = "variant_id")
    var variantId: Int = 0,

    var name: String = "",
    var description: String? = null,

    @ColumnInfo(name = "is_official")
    var isOfficial: Boolean = false,

    @ColumnInfo(name = "created_at")
    var createdAt: String = "",

    @ColumnInfo(name = "updated_at")
    var updatedAt: String? = null,

    @ColumnInfo(name = "task_count")
    var taskCount: Int = 0,

    @ColumnInfo(name = "is_downloaded", defaultValue = "0")
    var isDownloaded: Boolean = false,

    @ColumnInfo(name = "last_accessed_at")
    var lastAccessedAt: String? = null,

    @ColumnInfo(name = "remaining_time_millis")
    var remainingTimeMillis: Long? = null
) {
    @Ignore
    var isSelected: Boolean = false

    fun copy(
        variantId: Int = this.variantId,
        name: String = this.name,
        description: String? = this.description,
        isOfficial: Boolean = this.isOfficial,
        createdAt: String = this.createdAt,
        updatedAt: String? = this.updatedAt,
        taskCount: Int = this.taskCount,
        isDownloaded: Boolean = this.isDownloaded,
        lastAccessedAt: String? = this.lastAccessedAt,
        remainingTimeMillis: Long? = this.remainingTimeMillis,
        isSelected: Boolean = this.isSelected
    ): VariantEntity {
        return VariantEntity(
            variantId,
            name,
            description,
            isOfficial,
            createdAt,
            updatedAt,
            taskCount,
            isDownloaded,
            lastAccessedAt,
            remainingTimeMillis
        ).apply {
            this.isSelected = isSelected
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VariantEntity

        if (variantId != other.variantId) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (isOfficial != other.isOfficial) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (taskCount != other.taskCount) return false
        if (isDownloaded != other.isDownloaded) return false
        if (lastAccessedAt != other.lastAccessedAt) return false
        if (remainingTimeMillis != other.remainingTimeMillis) return false
        if (isSelected != other.isSelected) return false

        return true
    }

    override fun hashCode(): Int {
        var result = variantId
        result = 31 * result + name.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + isOfficial.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (updatedAt?.hashCode() ?: 0)
        result = 31 * result + taskCount
        result = 31 * result + isDownloaded.hashCode()
        result = 31 * result + (lastAccessedAt?.hashCode() ?: 0)
        result = 31 * result + (remainingTimeMillis?.hashCode() ?: 0)
        result = 31 * result + isSelected.hashCode()
        return result
    }
} 