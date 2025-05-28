package com.ruege.mobile.data.local.migration;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

public class Migration12to13 extends Migration {
    public Migration12to13() {
        super(12, 13);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        // Создание таблицы variants
        database.execSQL("CREATE TABLE IF NOT EXISTS `variants` (`variant_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `is_official` INTEGER NOT NULL, `task_count` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER, `last_accessed_at` INTEGER, `is_downloaded` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`variant_id`))");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_variants_variant_id` ON `variants` (`variant_id`)");

        // Создание таблицы variant_shared_texts
        database.execSQL("CREATE TABLE IF NOT EXISTS `variant_shared_texts` (`id` INTEGER NOT NULL, `variant_id` INTEGER NOT NULL, `text_content` TEXT NOT NULL, `text_type` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`variant_id`) REFERENCES `variants`(`variant_id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_variant_shared_texts_variant_id` ON `variant_shared_texts` (`variant_id`)");

        // Создание таблицы variant_tasks (восстановлено к версии пользователя)
        database.execSQL("CREATE TABLE IF NOT EXISTS `variant_tasks` (`variant_task_id` INTEGER NOT NULL, `variant_id` INTEGER NOT NULL, `original_task_id` INTEGER, `variant_shared_text_id` INTEGER, `ege_number` TEXT NOT NULL, `order_in_variant` INTEGER NOT NULL, `title` TEXT NOT NULL, `task_statement` TEXT NOT NULL, `difficulty` INTEGER NOT NULL, `max_points` INTEGER NOT NULL, `task_type` TEXT NOT NULL, `solution_text` TEXT, `explanation_text` TEXT, `time_limit` INTEGER NOT NULL, `created_at` TEXT NOT NULL, `updated_at` TEXT NOT NULL, PRIMARY KEY(`variant_task_id`), FOREIGN KEY(`variant_id`) REFERENCES `variants`(`variant_id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`variant_shared_text_id`) REFERENCES `variant_shared_texts`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_variant_tasks_variant_id` ON `variant_tasks` (`variant_id`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_variant_tasks_variant_shared_text_id` ON `variant_tasks` (`variant_shared_text_id`)");
        
        // Создание таблицы user_variant_task_answers (FK к variant_tasks(variant_task_id))
        database.execSQL("CREATE TABLE IF NOT EXISTS `user_variant_task_answers` (`variant_task_id` INTEGER NOT NULL, `variant_id` INTEGER NOT NULL, `user_submitted_answer` TEXT, `is_submission_correct` INTEGER, `points_awarded` INTEGER, `answered_timestamp` TEXT NOT NULL, PRIMARY KEY(`variant_task_id`), FOREIGN KEY(`variant_task_id`) REFERENCES `variant_tasks`(`variant_task_id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`variant_id`) REFERENCES `variants`(`variant_id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_user_variant_task_answers_variant_id` ON `user_variant_task_answers` (`variant_id`)");
    }
} 