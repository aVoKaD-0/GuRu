package com.ruege.mobile.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(
    tableName = "contents",
    foreignKeys = {
        @ForeignKey(
            entity = CategoryEntity.class,
            parentColumns = "category_id",
            childColumns = "parent_id",
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(name = "index_contents_parent_id", value = "parent_id")
    }
)
public class ContentEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "content_id")
    private String contentId;

    @NonNull
    @ColumnInfo(name = "title")
    private String title;

    @Nullable
    @ColumnInfo(name = "description")
    private String description;

    @NonNull
    @ColumnInfo(name = "type")
    private String type;
    
    @Nullable
    @ColumnInfo(name = "parent_id")
    private String parentId; 
    
    @ColumnInfo(name = "is_downloaded")
    private boolean isDownloaded;
    
    @ColumnInfo(name = "is_new")
    private boolean isNew;
    
    @ColumnInfo(name = "order_position")
    private int orderPosition;
    
    @Nullable
    @ColumnInfo(name = "content_url")
    private String contentUrl;

    
    public ContentEntity() {
        this.contentId = "default_id";
        this.title = "Без названия";
        this.type = "unknown";
    }

    @Ignore
    public ContentEntity(@NonNull String contentId, @NonNull String title, @Nullable String description, 
                       @NonNull String type, @Nullable String parentId, boolean isDownloaded, 
                       boolean isNew, int orderPosition, @Nullable String contentUrl) {
        this.contentId = contentId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.parentId = parentId;
        this.isDownloaded = isDownloaded;
        this.isNew = isNew;
        this.orderPosition = orderPosition;
        this.contentUrl = contentUrl;
    }

    @NonNull
    public String getContentId() {
        return contentId;
    }

    public void setContentId(@NonNull String contentId) {
        this.contentId = contentId;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    @Nullable
    public String getParentId() {
        return parentId;
    }

    public void setParentId(@Nullable String parentId) {
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

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public int getOrderPosition() {
        return orderPosition;
    }

    public void setOrderPosition(int orderPosition) {
        this.orderPosition = orderPosition;
    }

    @Nullable
    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(@Nullable String contentUrl) {
        this.contentUrl = contentUrl;
    }
} 