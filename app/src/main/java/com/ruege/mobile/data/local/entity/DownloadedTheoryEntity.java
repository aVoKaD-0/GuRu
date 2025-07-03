package com.ruege.mobile.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloaded_theory")
public class DownloadedTheoryEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "content_id")
    private String contentId;

    @NonNull
    @ColumnInfo(name = "title")
    private String title;

    @NonNull
    @ColumnInfo(name = "html_content")
    private String htmlContent;

    @ColumnInfo(name = "downloaded_at")
    private long downloadedAt;

    public DownloadedTheoryEntity(@NonNull String contentId, @NonNull String title, @NonNull String htmlContent, long downloadedAt) {
        this.contentId = contentId;
        this.title = title;
        this.htmlContent = htmlContent;
        this.downloadedAt = downloadedAt;
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

    @NonNull
    public String getHtmlContent() {
        return htmlContent;
    }

    public long getDownloadedAt() {
        return downloadedAt;
    }
} 