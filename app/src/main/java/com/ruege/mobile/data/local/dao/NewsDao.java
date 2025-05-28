package com.ruege.mobile.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.ruege.mobile.data.local.entity.NewsEntity;
import java.util.List;


@Dao 
public interface NewsDao {

    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<NewsEntity> newsList); 

    
    @Query("SELECT * FROM news ORDER BY publication_date DESC")
    kotlinx.coroutines.flow.Flow<java.util.List<NewsEntity>> getAllNews(); 

    
    @Query("DELETE FROM news")
    void deleteAll(); 

    
    
} 