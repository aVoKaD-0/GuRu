package com.ruege.mobile.data.local.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.util.Log;

/**
 * Миграция с версии 11 на 12 - добавление поля solved_task_ids в таблицу progress_sync_queue.
 */
public class Migration11to12 extends Migration {
    private static final String TAG = "Migration11to12";

    public Migration11to12() {
        super(11, 12);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "Начало миграции с версии 11 на 12");

        try {
            // Добавляем новую колонку solved_task_ids в таблицу progress_sync_queue
            database.execSQL(
                "ALTER TABLE progress_sync_queue ADD COLUMN solved_task_ids TEXT"
            );
            
            Log.d(TAG, "Успешно добавлена колонка solved_task_ids в таблицу progress_sync_queue");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при миграции с версии 11 на 12", e);
            throw e;
        }
    }
} 