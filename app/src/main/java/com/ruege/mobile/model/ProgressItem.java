package com.ruege.mobile.model;

/**
 * Модель элемента прогресса для отображения в UI
 */
public class ProgressItem {
    /**
     * Уникальный идентификатор элемента прогресса (например, contentId из ProgressEntity)
     */
    private String id;
    
    /**
     * Заголовок элемента прогресса, отображается в UI (например, "Задание 5")
     */
    private String title;
    
    /**
     * Описание элемента прогресса (например, "Выполнено 3 из 10 заданий")
     */
    private String description;
    
    /**
     * Процент выполнения (от 0 до 100)
     */
    private int percentage;
    
    /**
     * Тип элемента прогресса:
     * - "PROGRESS" - общий прогресс (используется другой стиль отображения)
     * - "TASK" - задание
     */
    private String type;

    /**
     * Полный конструктор с указанием всех полей
     */
    public ProgressItem(String id, String title, String description, int percentage, String type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.percentage = percentage;
        this.type = type;
    }

    /**
     * Конструктор для обратной совместимости
     */
    public ProgressItem(String title, int percentage) {
        this.id = "progress_" + System.currentTimeMillis();
        this.title = title;
        this.description = "";
        this.percentage = percentage;
        this.type = "PROGRESS";
    }

    // Геттеры и сеттеры

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
} 