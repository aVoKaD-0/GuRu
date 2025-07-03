package com.ruege.mobile.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "news") 
public class NewsEntity {

    @PrimaryKey
    @ColumnInfo(name = "news_id") 
    private long newsId;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "publication_date") 
    private long publicationDate;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "image_url") 
    private String imageUrl;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;
    
    
    public NewsEntity() {
    }

    
    public long getNewsId() {
        return newsId;
    }

    public void setNewsId(long newsId) {
        this.newsId = newsId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(long publicationDate) {
        this.publicationDate = publicationDate;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Ignore
    public NewsEntity(long newsId, String title, long publicationDate, String description, String imageUrl, long createdAt, long updatedAt) {
        this.newsId = newsId;
        this.title = title;
        this.publicationDate = publicationDate;
        this.description = description;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
} 