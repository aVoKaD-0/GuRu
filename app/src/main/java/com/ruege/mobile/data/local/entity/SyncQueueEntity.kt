package com.ruege.mobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность для хранения данных очереди синхронизации
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "entity_type")
    val entityType: String, 
    
    @ColumnInfo(name = "entity_id")
    val entityId: Long, 
    
    @ColumnInfo(name = "operation_type")
    val operationType: String,
    
    @ColumnInfo(name = "data")
    val data: String, 
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "attempts")
    val attempts: Int = 0, 
    
    @ColumnInfo(name = "last_attempt")
    val lastAttempt: Long? = null,
    
    @ColumnInfo(name = "status")
    val status: String = "PENDING" 
) 