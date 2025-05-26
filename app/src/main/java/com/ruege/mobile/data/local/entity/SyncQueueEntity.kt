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
    val entityType: String, // Тип сущности (progress, settings, etc.)
    
    @ColumnInfo(name = "entity_id")
    val entityId: Long, // ID сущности для синхронизации
    
    @ColumnInfo(name = "operation_type")
    val operationType: String, // Тип операции (INSERT, UPDATE, DELETE)
    
    @ColumnInfo(name = "data")
    val data: String, // Сериализованные данные для синхронизации (JSON)
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(), // Время создания записи
    
    @ColumnInfo(name = "attempts")
    val attempts: Int = 0, // Количество попыток синхронизации
    
    @ColumnInfo(name = "last_attempt")
    val lastAttempt: Long? = null, // Время последней попытки синхронизации
    
    @ColumnInfo(name = "status")
    val status: String = "PENDING" // Статус (PENDING, IN_PROGRESS, FAILED, COMPLETED)
) 