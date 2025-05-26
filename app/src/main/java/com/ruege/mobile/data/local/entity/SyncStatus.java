package com.ruege.mobile.data.local.entity;

/**
 * Перечисление статусов синхронизации для ProgressSyncQueueEntity.
 */
public enum SyncStatus {
    /**
     * Ожидает синхронизации с сервером
     */
    PENDING("PENDING"),
    
    /**
     * В процессе синхронизации
     */
    SYNCING("SYNCING"),
    
    /**
     * Успешно синхронизировано с сервером
     */
    SYNCED("SYNCED"),
    
    /**
     * Ошибка синхронизации
     */
    FAILED("FAILED"),
    
    /**
     * Конфликт данных при синхронизации
     */
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
     * @param value строковое представление статуса
     * @return соответствующий статус или PENDING, если не найден
     */
    public static SyncStatus fromString(String value) {
        for (SyncStatus status : SyncStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return PENDING; // Возвращаем PENDING как значение по умолчанию
    }
} 