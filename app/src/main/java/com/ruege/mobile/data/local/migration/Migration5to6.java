package com.ruege.mobile.data.local.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Миграция для обновления схемы базы данных с версии 5 на версию 6.
 * Добавляет столбцы title и user_id в таблицу progress.
 */
public class Migration5to6 extends Migration {

    public Migration5to6() {
        super(5, 6);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        // Создаем временную таблицу с новой структурой
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS progress_temp (" +
            "content_id TEXT NOT NULL PRIMARY KEY, " +
            "percentage INTEGER NOT NULL DEFAULT 0, " +
            "last_accessed INTEGER NOT NULL, " +
            "completed INTEGER NOT NULL DEFAULT 0, " +
            "title TEXT, " +
            "user_id TEXT, " +
            "FOREIGN KEY (content_id) REFERENCES contents(content_id) ON DELETE CASCADE)"
        );

        // Копируем данные из старой таблицы в новую
        database.execSQL(
            "INSERT INTO progress_temp (content_id, percentage, last_accessed, completed, title, user_id) " +
            "SELECT content_id, percentage, last_accessed, completed, NULL, '1' FROM progress"
        );

        // Удаляем старую таблицу
        database.execSQL("DROP TABLE progress");

        // Переименовываем временную таблицу
        database.execSQL("ALTER TABLE progress_temp RENAME TO progress");

        // Создаем необходимый индекс
        database.execSQL("CREATE INDEX IF NOT EXISTS index_progress_content_id ON progress(content_id)");
    }
} 