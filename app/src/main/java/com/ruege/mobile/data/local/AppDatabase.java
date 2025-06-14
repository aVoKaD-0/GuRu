package com.ruege.mobile.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.migration.Migration;
import com.ruege.mobile.data.local.dao.CategoryDao;
import com.ruege.mobile.data.local.dao.ContentDao;
import com.ruege.mobile.data.local.dao.NewsDao;
import com.ruege.mobile.data.local.dao.PracticeAttemptDao;
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao;
import com.ruege.mobile.data.local.dao.ProgressDao;
import com.ruege.mobile.data.local.dao.ProgressSyncQueueDao;
import com.ruege.mobile.data.local.dao.TaskDao;
import com.ruege.mobile.data.local.dao.UserDao;
import com.ruege.mobile.data.local.dao.UserTaskAttemptDao;
import com.ruege.mobile.data.local.dao.TaskOptionDao;
import com.ruege.mobile.data.local.dao.SyncQueueDao;
import com.ruege.mobile.data.local.dao.VariantDao;
import com.ruege.mobile.data.local.dao.VariantSharedTextDao;
import com.ruege.mobile.data.local.dao.VariantTaskDao;
import com.ruege.mobile.data.local.dao.UserVariantTaskAnswerDao;
import com.ruege.mobile.data.local.dao.VariantTaskOptionDao;
import com.ruege.mobile.data.local.dao.DownloadedTheoryDao;
import com.ruege.mobile.data.local.dao.TaskTextDao;

import com.ruege.mobile.data.local.entity.CategoryEntity;
import com.ruege.mobile.data.local.entity.ContentEntity;
import com.ruege.mobile.data.local.entity.NewsEntity;
import com.ruege.mobile.data.local.entity.PracticeAttemptEntity;
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity;
import com.ruege.mobile.data.local.entity.ProgressEntity;
import com.ruege.mobile.data.local.entity.ProgressSyncQueueEntity;
import com.ruege.mobile.data.local.entity.TaskEntity;
import com.ruege.mobile.data.local.entity.TaskOptionEntity;
import com.ruege.mobile.data.local.entity.UserEntity;
import com.ruege.mobile.data.local.entity.UserTaskAttemptEntity;
import com.ruege.mobile.data.local.entity.SyncQueueEntity;
import com.ruege.mobile.data.local.entity.VariantEntity;
import com.ruege.mobile.data.local.entity.VariantSharedTextEntity;
import com.ruege.mobile.data.local.entity.VariantTaskEntity;
import com.ruege.mobile.data.local.entity.UserVariantTaskAnswerEntity;
import com.ruege.mobile.data.local.entity.VariantTaskOptionEntity;
import com.ruege.mobile.data.local.entity.DownloadedTheoryEntity;
import com.ruege.mobile.data.local.entity.TaskTextEntity;


@Database(
    entities = {
        NewsEntity.class,
        UserEntity.class,
        CategoryEntity.class,
        ContentEntity.class,
        ProgressEntity.class,
        TaskEntity.class,
        TaskOptionEntity.class,
        UserTaskAttemptEntity.class,
        PracticeAttemptEntity.class,
        PracticeStatisticsEntity.class,
        ProgressSyncQueueEntity.class,
        SyncQueueEntity.class,
        VariantEntity.class,
        VariantSharedTextEntity.class,
        VariantTaskEntity.class,
        UserVariantTaskAnswerEntity.class,
        VariantTaskOptionEntity.class,
        DownloadedTheoryEntity.class,
        TaskTextEntity.class
    },
    version = 19,
    exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract NewsDao newsDao();
    public abstract UserDao userDao();
    public abstract CategoryDao categoryDao();
    public abstract ContentDao contentDao();
    public abstract ProgressDao progressDao();
    public abstract ProgressSyncQueueDao progressSyncQueueDao();
    public abstract TaskDao taskDao();
    public abstract UserTaskAttemptDao userTaskAttemptDao();
    @NonNull
    public abstract TaskOptionDao taskOptionDao();
    public abstract PracticeAttemptDao practiceAttemptDao();
    public abstract PracticeStatisticsDao practiceStatisticsDao();

    public  abstract  SyncQueueDao syncQueueDao();

    public abstract VariantDao variantDao();
    public abstract VariantSharedTextDao variantSharedTextDao();
    public abstract VariantTaskDao variantTaskDao();
    public abstract UserVariantTaskAnswerDao userVariantTaskAnswerDao();
    public abstract VariantTaskOptionDao variantTaskOptionDao();
    public abstract DownloadedTheoryDao downloadedTheoryDao();
    public abstract TaskTextDao taskTextDao();

    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "mobile_database.db";
    
    // Миграция с версии 18 на 19: добавление поля variant_data в таблицу practice_statistics
    static final Migration MIGRATION_18_19 = new Migration(18, 19) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            try {
                // Создаем временную таблицу с новой структурой
                database.execSQL(
                    "CREATE TABLE practice_statistics_temp (" +
                    "ege_number TEXT NOT NULL PRIMARY KEY, " +
                    "total_attempts INTEGER NOT NULL, " +
                    "correct_attempts INTEGER NOT NULL, " +
                    "last_attempt_date INTEGER NOT NULL, " +
                    "variant_data TEXT)"
                );

                // Копируем данные из старой таблицы в новую
                database.execSQL(
                    "INSERT INTO practice_statistics_temp (ege_number, total_attempts, correct_attempts, last_attempt_date) " +
                    "SELECT ege_number, total_attempts, correct_attempts, last_attempt_date FROM practice_statistics"
                );

                // Удаляем старую таблицу
                database.execSQL("DROP TABLE practice_statistics");

                // Переименовываем временную таблицу
                database.execSQL("ALTER TABLE practice_statistics_temp RENAME TO practice_statistics");
            } catch (Exception e) {
                // В случае ошибки, создаем таблицу заново
                database.execSQL("DROP TABLE IF EXISTS practice_statistics");
                database.execSQL(
                    "CREATE TABLE practice_statistics (" +
                    "ege_number TEXT NOT NULL PRIMARY KEY, " +
                    "total_attempts INTEGER NOT NULL, " +
                    "correct_attempts INTEGER NOT NULL, " +
                    "last_attempt_date INTEGER NOT NULL, " +
                    "variant_data TEXT)"
                );
            }
        }
    };

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                        )
                        .addMigrations(MIGRATION_18_19)
                        .fallbackToDestructiveMigration()
                        .addCallback(new Callback() {
                            @Override
                            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                super.onOpen(db);
                                DBResetHelper.checkAndFixProgressTable(context);
                            }
                        })
                        .build();
                }
            }
        }
        return INSTANCE;
    }
    
    
    public static void clearAndRebuildDatabase(final Context context) {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
        
        
        context.deleteDatabase(DATABASE_NAME);
        
        
        getInstance(context);
    }
    
}
