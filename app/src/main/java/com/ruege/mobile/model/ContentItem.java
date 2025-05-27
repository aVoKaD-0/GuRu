package com.ruege.mobile.model;

public class ContentItem {
    private String id;
    private String title;
    private String description;
    private String type; 
    private String parentId; 
    private boolean isDownloaded;
    private boolean isNew;

    public ContentItem(String id, String title, String description, String type, String parentId, boolean isDownloaded) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.parentId = parentId;
        this.isDownloaded = isDownloaded;
        this.isNew = false; 
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
    
    public String getContentId() {
        return id;
    }
} 