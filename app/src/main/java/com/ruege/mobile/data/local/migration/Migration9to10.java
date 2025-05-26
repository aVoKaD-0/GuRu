package com.ruege.mobile.data.local.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.util.Log;

/**
 * Миграция с версии 9 на 10 - добавление таблицы sync_queue.
 */
public class Migration9to10 extends Migration {
    private static final String TAG = "Migration9to10";

    public Migration9to10() {
        super(9, 10);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "Начало миграции с версии 9 на 10");

        try {
            // Создаем таблицу sync_queue
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `sync_queue` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`entity_type` TEXT NOT NULL, " +
                    "`entity_id` INTEGER NOT NULL, " +
                    "`operation_type` TEXT NOT NULL, " +
                    "`data` TEXT NOT NULL, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "`attempts` INTEGER NOT NULL, " +
                    "`last_attempt` INTEGER, " +
                    "`status` TEXT NOT NULL" +
                ")"
            );
            
            Log.d(TAG, "Миграция с версии 9 на 10 успешно завершена");
        } catch (Exception e) {
            // Логируем ошибку, но не прерываем миграцию
            Log.e(TAG, "Ошибка при миграции с версии 9 на 10", e);
            // Пробрасываем исключение, чтобы Room знал, что миграция не удалась
            throw e;
        }
    }
} 