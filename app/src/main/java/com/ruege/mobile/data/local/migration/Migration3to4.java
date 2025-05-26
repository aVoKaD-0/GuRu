package com.ruege.mobile.data.local.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Миграция для добавления полей created_at и updated_at в таблицу новостей
 */
public class Migration3to4 extends Migration {
    
    public Migration3to4() {
        super(3, 4);
    }
    
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        try {
            // Пытаемся добавить колонки created_at и updated_at
            
            // Сначала проверяем, существуют ли уже эти поля
            boolean hasCreatedAt = false;
            boolean hasUpdatedAt = false;
            
            try {
                database.execSQL("SELECT `created_at` FROM `news` LIMIT 0");
                hasCreatedAt = true;
            } catch (Exception e) {
                // Поле отсутствует
            }
            
            try {
                database.execSQL("SELECT `updated_at` FROM `news` LIMIT 0");
                hasUpdatedAt = true;
            } catch (Exception e) {
                // Поле отсутствует
            }
            
            // Если полей нет - добавляем их
            if (!hasCreatedAt) {
                database.execSQL("ALTER TABLE `news` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT " + 
                        System.currentTimeMillis());
            }
            
            if (!hasUpdatedAt) {
                database.execSQL("ALTER TABLE `news` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT " + 
                        System.currentTimeMillis());
            }
            
            // Переименовываем старое поле date в publicationDate, если необходимо
            try {
                database.execSQL("SELECT `date` FROM `news` LIMIT 0");
                
                // Создаем новую таблицу с нужной структурой
                database.execSQL("CREATE TABLE `news_new` (" +
                        "`news_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT, " +
                        "`publication_date` INTEGER NOT NULL, " +
                        "`description` TEXT, " +
                        "`image_url` TEXT, " +
                        "`created_at` INTEGER NOT NULL, " +
                        "`updated_at` INTEGER NOT NULL)");
                
                // Копируем данные из старой таблицы в новую
                database.execSQL("INSERT INTO `news_new` " +
                        "(`news_id`, `title`, `publication_date`, `description`, `image_url`, `created_at`, `updated_at`) " +
                        "SELECT `news_id`, `title`, `date`, `description`, `image_url`, " + 
                        System.currentTimeMillis() + ", " + System.currentTimeMillis() + " FROM `news`");
                
                // Удаляем старую таблицу
                database.execSQL("DROP TABLE `news`");
                
                // Переименовываем новую таблицу
                database.execSQL("ALTER TABLE `news_new` RENAME TO `news`");
            } catch (Exception e) {
                // Поле date не существует или произошла другая ошибка
                // Ничего делать не нужно
            }
            
        } catch (Exception e) {
            // Если таблица не существует или произошла ошибка, воссоздаем ее полностью
            database.execSQL("DROP TABLE IF EXISTS `news`");
            
            database.execSQL("CREATE TABLE `news` (" +
                    "`news_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT, " +
                    "`publication_date` INTEGER NOT NULL, " +
                    "`description` TEXT, " +
                    "`image_url` TEXT, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "`updated_at` INTEGER NOT NULL)");
        }
    }
} 