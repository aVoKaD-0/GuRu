package com.ruege.mobile.data.local.entity;

/**
 * Перечисление статусов синхронизации для ProgressSyncQueueEntity.
 */
public enum SyncStatus {

    PENDING("PENDING"),
    

    SYNCING("SYNCING"),
    

    SYNCED("SYNCED"),

    FAILED("FAILED"),
    
    CONFLICT("CONFLICT");

    private final String value;

    SyncStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Получает статус по его строковому представлению
     */
    public static SyncStatus fromString(String value) {
        for (SyncStatus status : SyncStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return PENDING; 
    }
} 