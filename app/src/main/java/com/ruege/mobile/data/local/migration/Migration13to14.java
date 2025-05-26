package com.ruege.mobile.data.local.migration;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

public class Migration13to14 extends Migration {
    public Migration13to14() {
        super(13, 14);
    }
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        database.execSQL("CREATE TABLE `user_variant_task_answers_new` (`variant_task_id` INTEGER NOT NULL, `variant_id` INTEGER NOT NULL, `user_submitted_answer` TEXT, `is_submission_correct` INTEGER, `points_awarded` INTEGER, `answered_timestamp` TEXT NOT NULL, PRIMARY KEY(`variant_task_id`), FOREIGN KEY(`variant_task_id`) REFERENCES `variant_tasks`(`variant_task_id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`variant_id`) REFERENCES `variants`(`variant_id`) ON UPDATE NO ACTION ON DELETE NO ACTION )");
        database.execSQL("INSERT INTO `user_variant_task_answers_new` SELECT `variant_task_id`, `variant_id`, `user_submitted_answer`, `is_submission_correct`, `points_awarded`, `answered_timestamp` FROM `user_variant_task_answers`");
        database.execSQL("DROP TABLE `user_variant_task_answers`");
        database.execSQL("ALTER TABLE `user_variant_task_answers_new` RENAME TO `user_variant_task_answers`");
        // Добавляем обратно индексы, если они были и не создались автоматически с новой таблицей;
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_user_variant_task_answers_variant_id` ON `user_variant_task_answers` (`variant_id`)");
    }
}
