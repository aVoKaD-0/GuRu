package com.ruege.mobile.data.local.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migration4to5 extends Migration {

    public Migration4to5() {
        super(4, 5);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        // 1. Обновить таблицу tasks, добавив поле task_type
        database.execSQL("ALTER TABLE tasks ADD COLUMN task_type TEXT DEFAULT 'TEXT' NOT NULL");

        // 2. Обновить таблицу progress, удалив поле user_id и сделав content_id первичным ключом
        database.execSQL("CREATE TABLE IF NOT EXISTS progress_new (" +
                "content_id TEXT PRIMARY KEY NOT NULL, " +
                "percentage INTEGER DEFAULT 0 NOT NULL, " +
                "last_accessed INTEGER, " +
                "completed INTEGER DEFAULT 0 NOT NULL, " +
                "FOREIGN KEY (content_id) REFERENCES contents(content_id) ON DELETE CASCADE)");
        
        database.execSQL("INSERT OR IGNORE INTO progress_new (content_id, percentage, last_accessed, completed) " +
                "SELECT content_id, percentage, last_accessed, completed FROM progress " +
                "GROUP BY content_id");

        database.execSQL("DROP TABLE progress");
        database.execSQL("ALTER TABLE progress_new RENAME TO progress");
        database.execSQL("CREATE INDEX IF NOT EXISTS index_progress_content_id ON progress(content_id)");

        // 3. Создать таблицу practice_attempts
        database.execSQL("CREATE TABLE IF NOT EXISTS practice_attempts (" +
                "attempt_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "task_id INTEGER NOT NULL, " +
                "is_correct INTEGER DEFAULT 0 NOT NULL, " +
                "attempt_date INTEGER DEFAULT 0 NOT NULL, " +
                "FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE)");
        
        database.execSQL("CREATE INDEX IF NOT EXISTS index_practice_attempts_task_id ON practice_attempts(task_id)");

        // 4. Создать таблицу practice_statistics
        database.execSQL("CREATE TABLE IF NOT EXISTS practice_statistics (" +
                "ege_number TEXT PRIMARY KEY NOT NULL, " +
                "total_attempts INTEGER DEFAULT 0 NOT NULL, " +
                "correct_attempts INTEGER DEFAULT 0 NOT NULL, " +
                "last_attempt_date INTEGER DEFAULT 0 NOT NULL)");
    }
} 