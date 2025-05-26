package com.ruege.mobile.model;

public class NavigationItem {
    private final String id;
    private final String title;
    private final int iconResId;
    
    public NavigationItem(String id, String title, int iconResId) {
        this.id = id;
        this.title = title;
        this.iconResId = iconResId;
    }
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public int getIconResId() {
        return iconResId;
    }
} 