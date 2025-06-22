package com.ruege.mobile.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Вспомогательный класс для сброса базы данных при обновлении схемы,
 * когда миграция не может быть выполнена.
 */
public class DBResetHelper {
    
    private static final String TAG = "DBResetHelper";
    private static final String PREF_NAME = "db_version_prefs";
    private static final String LAST_VERSION_KEY = "last_db_version";
    private static final String PROGRESS_FIXED_KEY = "progress_table_fixed";
    
    private final Context context;
    private final SharedPreferences prefs;
    
    /**
     * Создает новый экземпляр помощника по сбросу базы данных
     * @param context Контекст приложения
     */
    public DBResetHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Получает текущую версию базы данных из настроек
     * @return Сохраненная версия базы данных или 0, если версия не была сохранена
     */
    public int getCurrentDbVersion() {
        return prefs.getInt(LAST_VERSION_KEY, 0);
    }
    
    /**
     * Сбрасывает базу данных и обновляет сохраненную версию
     * @param newVersion Новая версия базы данных для сохранения
     * @return true, если база была сброшена
     */
    public boolean resetDatabase(int newVersion) {
        try {
            Log.w(TAG, "Сброс базы данных и установка новой версии: " + newVersion);
            
            AppDatabase.clearAndRebuildDatabase(context);
            
            prefs.edit().putInt(LAST_VERSION_KEY, newVersion).apply();
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сбросе базы данных", e);
            return false;
        }
    }

    /**
     * Сбрасывает базу данных полностью, используя существующий метод clearAndRebuildDatabase
     * @param context Контекст приложения
     */
    public static void resetDatabase(Context context) {
        try {
            Log.w(TAG, "Полный сброс базы данных");
            AppDatabase.clearAndRebuildDatabase(context);
            Log.d(TAG, "База данных успешно сброшена и пересоздана");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сбросе базы данных", e);
        }
    }

    /**
     * Проверяет необходимость сброса базы данных.
     * Сравнивает текущую версию с сохраненной и сбрасывает базу, если версии не совпадают.
     * 
     * @param context Контекст приложения
     * @param currentVersion Текущая версия базы данных
     * @return true, если база была сброшена
     */
    public static boolean resetDatabaseIfNeeded(Context context, int currentVersion) {
        DBResetHelper helper = new DBResetHelper(context);
        int savedVersion = helper.getCurrentDbVersion();
        
        Log.d(TAG, "Current DB version: " + currentVersion + ", Saved version: " + savedVersion);
        
        if (savedVersion < currentVersion) {
            return helper.resetDatabase(currentVersion);
        }
        
        return false;
    }
    
    /**
     * Проверяет и исправляет таблицу прогресса, если необходимо.
     * Выполняется один раз после обновления приложения.
     * @param context Контекст приложения
     */
    public static void checkAndFixProgressTable(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isFixed = prefs.getBoolean(PROGRESS_FIXED_KEY, false);
        
        if (!isFixed) {
            Log.d(TAG, "Проверка и исправление таблицы прогресса...");
            try {
                new Thread(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(context);
                        
                        int deleted = db.getOpenHelper().getWritableDatabase()
                                .delete("progress", "content_id IS NULL", null);
                        
                        if (deleted > 0) {
                            Log.d(TAG, "Удалено записей с NULL contentId: " + deleted);
                        }
                        
                        android.database.Cursor cursor = db.getOpenHelper().getReadableDatabase()
                                .query("SELECT COUNT(*) FROM progress WHERE content_id LIKE 'task_group_%'");
                        
                        cursor.moveToFirst();
                        int taskGroupCount = cursor.getInt(0);
                        cursor.close();
                        
                        Log.d(TAG, "Найдено " + taskGroupCount + " записей task_group_");

                        prefs.edit().putBoolean(PROGRESS_FIXED_KEY, true).apply();
                        
                        Log.d(TAG, "Проверка и исправление таблицы прогресса завершены");
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при проверке и исправлении таблицы прогресса", e);
                    }
                }).start();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при запуске проверки таблицы прогресса", e);
            }
        } else {
            Log.d(TAG, "Таблица прогресса уже была проверена и исправлена ранее");
        }
    }
} 