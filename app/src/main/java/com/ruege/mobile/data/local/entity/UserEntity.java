package com.ruege.mobile.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    @ColumnInfo(name = "user_id")
    private long userId;

    @ColumnInfo(name = "username")
    private String username;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "avatar_url")
    private String avatarUrl;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "last_login")
    private long lastLogin;

    @ColumnInfo(name = "is_2fa_enabled")
    private boolean is2faEnabled;

    
    public UserEntity() {
    }

    @Ignore
    public UserEntity(String username, String email, String avatarUrl, long createdAt, long lastLogin) {
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.is2faEnabled = false;
    }
    
    @Ignore
    public UserEntity(String username, String email, String avatarUrl, long createdAt, long lastLogin, boolean is2faEnabled) {
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.is2faEnabled = is2faEnabled;
    }

    @Ignore
    public UserEntity(long userId, String username, String email, String avatarUrl, long createdAt, long lastLogin, boolean is2faEnabled) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.is2faEnabled = is2faEnabled;
    }

    
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public boolean isIs2faEnabled() {
        return is2faEnabled;
    }

    public void setIs2faEnabled(boolean is2faEnabled) {
        this.is2faEnabled = is2faEnabled;
    }
} 