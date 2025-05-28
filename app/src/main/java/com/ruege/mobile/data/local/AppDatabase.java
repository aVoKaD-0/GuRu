package com.ruege.mobile.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
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
import com.ruege.mobile.data.local.migration.Migration1to2;
import com.ruege.mobile.data.local.migration.Migration3to4;
import com.ruege.mobile.data.local.migration.Migration4to5;
import com.ruege.mobile.data.local.migration.Migration5to6;
import com.ruege.mobile.data.local.migration.Migration6to7;
import com.ruege.mobile.data.local.migration.Migration7to8;
import com.ruege.mobile.data.local.migration.Migration8to9;
import com.ruege.mobile.data.local.migration.Migration9to10;
import com.ruege.mobile.data.local.migration.Migration10to11;
import com.ruege.mobile.data.local.migration.Migration11to12;
import com.ruege.mobile.data.local.migration.Migration12to13;
import com.ruege.mobile.data.local.migration.Migration13to14;


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
        VariantTaskOptionEntity.class
    },
    version = 15,
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

    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "mobile_database.db";

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Удаляем временное решение с удалением базы данных
                    
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                        )
                        .fallbackToDestructiveMigration()
                        .addMigrations(new Migration1to2()) 
                        .addMigrations(new Migration3to4())
                        .addMigrations(new Migration4to5())
                        .addMigrations(new Migration5to6())
                        .addMigrations(new Migration6to7())
                        .addMigrations(new Migration7to8())
                        .addMigrations(new Migration8to9())
                        .addMigrations(new Migration9to10())
                        .addMigrations(new Migration10to11())
                        .addMigrations(new Migration11to12())
                        .addMigrations(new Migration12to13())
                        .addMigrations(new Migration13to14())
                        .addCallback(new Callback() {
                            @Override
                            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                super.onOpen(db);
                                // Здесь можно выполнить дополнительные действия после открытия базы
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