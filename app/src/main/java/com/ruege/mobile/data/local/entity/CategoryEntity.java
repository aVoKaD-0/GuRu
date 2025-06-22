package com.ruege.mobile.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "categories")
public class CategoryEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "category_id")
    private String categoryId; 

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "icon_url")
    private String iconUrl;

    @ColumnInfo(name = "order_position")
    private int orderPosition;

    @ColumnInfo(name = "is_visible")
    private boolean isVisible;

    
    public CategoryEntity() {
    }

    @Ignore
    public CategoryEntity(String categoryId, String title, String description, String iconUrl, int orderPosition, boolean isVisible) {
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.iconUrl = iconUrl;
        this.orderPosition = orderPosition;
        this.isVisible = isVisible;
    }

    
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
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

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public int getOrderPosition() {
        return orderPosition;
    }

    public void setOrderPosition(int orderPosition) {
        this.orderPosition = orderPosition;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }
} 