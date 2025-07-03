package com.ruege.mobile.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import kotlinx.coroutines.flow.Flow;

import com.ruege.mobile.data.local.entity.UserEntity;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Delete
    void delete(UserEntity user);

    @Query("UPDATE users SET last_login = :timestamp WHERE user_id = :userId")
    void updateLastLogin(long userId, long timestamp);
    
    @Query("SELECT COUNT(*) FROM users")
    int getUserCount();

    @Query("SELECT * FROM users LIMIT 1")
    UserEntity getFirstUser();
    
    @Query("SELECT * FROM users LIMIT 1")
    Flow<UserEntity> getFirstUserFlow();

    @Query("DELETE FROM users")
    void deleteAll();
} 