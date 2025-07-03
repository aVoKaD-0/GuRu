package com.ruege.mobile.data.local.converter;

import androidx.room.TypeConverter;
import com.ruege.mobile.data.local.entity.SyncStatus;

/**
 * Конвертер для преобразования SyncStatus в String и обратно.
 * Используется Room для хранения перечислений в базе данных.
 */
public class SyncStatusConverter {
    @TypeConverter
    public static String fromSyncStatus(SyncStatus syncStatus) {
        return syncStatus == null ? null : syncStatus.getValue();
    }

    @TypeConverter
    public static SyncStatus toSyncStatus(String value) {
        return value == null ? null : SyncStatus.fromString(value);
    }
} 