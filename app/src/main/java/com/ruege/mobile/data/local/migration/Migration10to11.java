package com.ruege.mobile.data.local.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.util.Log;

/**
 * Миграция с версии 10 на 11 - удаление внешнего ключа в таблице progress и добавление поля solved_task_ids.
 */
public class Migration10to11 extends Migration {
    private static final String TAG = "Migration10to11";

    public Migration10to11() {
        super(10, 11);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "Начало миграции с версии 10 на 11");

        try {
            // Проверяем, есть ли колонка solved_task_ids в текущей таблице progress
            boolean hasSolvedTaskIdsColumn = false;
            try {
                database.query("SELECT solved_task_ids FROM progress LIMIT 1").close();
                hasSolvedTaskIdsColumn = true;
                Log.d(TAG, "Колонка solved_task_ids уже существует в таблице progress");
            } catch (Exception e) {
                Log.d(TAG, "Колонка solved_task_ids не найдена в таблице progress, она будет добавлена");
            }

            // Создаем временную таблицу с колонкой solved_task_ids
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `progress_temp` (" +
                    "`content_id` TEXT NOT NULL, " +
                    "`percentage` INTEGER NOT NULL, " +
                    "`last_accessed` INTEGER NOT NULL, " +
                    "`completed` INTEGER NOT NULL, " +
                    "`title` TEXT, " +
                    "`description` TEXT, " +
                    "`user_id` INTEGER NOT NULL, " +
                    "`solved_task_ids` TEXT, " +
                    "PRIMARY KEY(`content_id`)" +
                ")"
            );
            
            // Копируем данные с учетом наличия/отсутствия колонки solved_task_ids
            if (hasSolvedTaskIdsColumn) {
                database.execSQL(
                    "INSERT OR IGNORE INTO progress_temp " +
                    "SELECT content_id, percentage, last_accessed, completed, title, description, user_id, solved_task_ids FROM progress"
                );
            } else {
                database.execSQL(
                    "INSERT OR IGNORE INTO progress_temp " +
                    "SELECT content_id, percentage, last_accessed, completed, title, description, user_id, NULL FROM progress"
                );
            }
            
            // Удаляем старую таблицу
            database.execSQL("DROP TABLE progress");
            
            // Переименовываем временную таблицу
            database.execSQL("ALTER TABLE progress_temp RENAME TO progress");
            
            // Создаем индекс
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_progress_content_id` ON `progress` (`content_id`)"
            );
            
            Log.d(TAG, "Миграция с версии 10 на 11 успешно завершена");
        } catch (Exception e) {
            // Логируем ошибку, но не прерываем миграцию
            Log.e(TAG, "Ошибка при миграции с версии 10 на 11", e);
            // Пробрасываем исключение, чтобы Room знал, что миграция не удалась
            throw e;
        }
    }
} 