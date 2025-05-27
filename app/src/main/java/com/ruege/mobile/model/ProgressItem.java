package com.ruege.mobile.model;

/**
 * Модель элемента прогресса для отображения в UI
 */
public class ProgressItem {
    private String id;
    
    private String title;

    private String description;
    

    private int percentage;
    

    private String type;


    public ProgressItem(String id, String title, String description, int percentage, String type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.percentage = percentage;
        this.type = type;
    }


    public ProgressItem(String title, int percentage) {
        this.id = "progress_" + System.currentTimeMillis();
        this.title = title;
        this.description = "";
        this.percentage = percentage;
        this.type = "PROGRESS";
    }



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