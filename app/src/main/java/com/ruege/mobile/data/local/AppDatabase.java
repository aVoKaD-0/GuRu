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
import com.ruege.mobile.data.local.dao.ShpargalkaDao;

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
import com.ruege.mobile.data.local.entity.ShpargalkaEntity;


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
        TaskTextEntity.class,
        ShpargalkaEntity.class
    },
    version = 21,
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
    public abstract ShpargalkaDao shpargalkaDao();

    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "mobile_database.db";
    
    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                        )
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
