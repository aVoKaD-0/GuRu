package com.ruege.mobile.data.local.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.util.Log;

/**
 * Миграция базы данных с версии 7 на версию 8
 * Обновляет схему таблицы progress_sync_queue, чтобы она соответствовала ожидаемой структуре
 */
public class Migration7to8 extends Migration {
    
    private static final String TAG = "Migration7to8";
    
    public Migration7to8() {
        super(7, 8);
    }
    
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "Migrating database from version 7 to 8");
        
        try {
            // 1. Создаем временную таблицу для сохранения данных
            database.execSQL("CREATE TABLE IF NOT EXISTS `progress_sync_queue_temp` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`content_id` TEXT NOT NULL, " +
                    "`percentage` INTEGER NOT NULL, " +
                    "`completed` INTEGER NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`user_id` TEXT, " +
                    "`sync_status` TEXT NOT NULL, " +
                    "`retry_count` INTEGER NOT NULL DEFAULT 0, " +
                    "`last_sync_attempt` INTEGER NOT NULL DEFAULT 0, " +
                    "`error_message` TEXT)");
            
            // 2. Копируем данные из старой таблицы во временную
            database.execSQL("INSERT OR IGNORE INTO `progress_sync_queue_temp` " +
                    "(`id`, `content_id`, `percentage`, `completed`, `timestamp`, `user_id`, `sync_status`, `retry_count`, `last_sync_attempt`, `error_message`) " +
                    "SELECT `id`, `content_id`, `percentage`, `completed`, `timestamp`, `user_id`, `sync_status`, `retry_count`, `last_sync_attempt`, `error_message` " +
                    "FROM `progress_sync_queue`");
            
            // 3. Удаляем старую таблицу
            database.execSQL("DROP TABLE IF EXISTS `progress_sync_queue`");
            
            // 4. Переименовываем временную таблицу
            database.execSQL("ALTER TABLE `progress_sync_queue_temp` RENAME TO `progress_sync_queue`");
            
            // 5. Создаем необходимые индексы
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_progress_sync_queue_sync_status` ON `progress_sync_queue` (`sync_status`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_progress_sync_queue_user_id` ON `progress_sync_queue` (`user_id`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_progress_sync_queue_content_id` ON `progress_sync_queue` (`content_id`)");
            
            Log.d(TAG, "Migration completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during migration", e);
            throw e;
        }
    }
} 