package com.ruege.mobile.data.local.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Миграция для обновления схемы базы данных с версии 6 на версию 7.
 * Добавляет таблицу progress_sync_queue для отслеживания синхронизации прогресса.
 */
public class Migration6to7 extends Migration {

    public Migration6to7() {
        super(6, 7);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        // Создаем таблицу для очереди синхронизации прогресса
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS progress_sync_queue (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "content_id TEXT NOT NULL, " +
            "percentage INTEGER NOT NULL, " +
            "completed INTEGER NOT NULL, " +
            "timestamp INTEGER NOT NULL, " +
            "user_id TEXT, " +
            "sync_status TEXT NOT NULL, " +
            "retry_count INTEGER NOT NULL DEFAULT 0, " +
            "last_sync_attempt INTEGER NOT NULL DEFAULT 0, " +
            "error_message TEXT)"
        );
        
        // Создаем индекс для ускорения поиска по content_id
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_progress_sync_queue_content_id " +
            "ON progress_sync_queue (content_id)"
        );
        
        // Создаем индекс для ускорения поиска по user_id
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_progress_sync_queue_user_id " +
            "ON progress_sync_queue (user_id)"
        );
        
        // Создаем индекс для ускорения поиска по sync_status
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_progress_sync_queue_sync_status " +
            "ON progress_sync_queue (sync_status)"
        );
    }
} 