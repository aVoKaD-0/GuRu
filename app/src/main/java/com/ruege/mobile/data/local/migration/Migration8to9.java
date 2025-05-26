package com.ruege.mobile.data.local.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.util.Log;
import android.database.Cursor;

/**
 * Миграция базы данных с версии 8 на версию 9
 * Добавляет поле google_id в таблицу users
 */
public class Migration8to9 extends Migration {
    
    private static final String TAG = "Migration8to9";
    
    public Migration8to9() {
        super(8, 9);
    }
    
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
        Log.d(TAG, "Migrating database from version 8 to 9");
        
        try {
            // Проверяем существование колонки google_id в таблице users
            boolean columnExists = false;
            Cursor cursor = database.query("PRAGMA table_info(users)");
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (nameIndex != -1) {
                        String columnName = cursor.getString(nameIndex);
                        if ("google_id".equals(columnName)) {
                            columnExists = true;
                            break;
                        }
                    }
                }
                cursor.close();
            }
            
            // Добавляем колонку google_id только если она еще не существует
            if (!columnExists) {
                database.execSQL("ALTER TABLE users ADD COLUMN google_id TEXT");
                Log.d(TAG, "Колонка google_id успешно добавлена в таблицу users");
            } else {
                Log.d(TAG, "Колонка google_id уже существует в таблице users, пропускаем");
            }
            
            Log.d(TAG, "Migration completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during migration", e);
            throw e;
        }
    }
} 