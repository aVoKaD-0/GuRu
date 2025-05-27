package com.ruege.mobile.model;

public class NewsItem {
    private String title;
    private String date;
    private String description;
    private String imageUrl;
    private String fullContentUrl;

    public NewsItem(String title, String date, String description, String imageUrl, String fullContentUrl) {
        this.title = title;
        this.date = date;
        this.description = description;
        this.imageUrl = imageUrl;
        this.fullContentUrl = fullContentUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getFullContentUrl() {
        return fullContentUrl;
    }

    public void setFullContentUrl(String fullContentUrl) {
        this.fullContentUrl = fullContentUrl;
    }
    
    public String getContentId() {
        return fullContentUrl; 
    }
} 