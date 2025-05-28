package com.ruege.mobile.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Transaction;

import com.ruege.mobile.data.local.entity.CategoryEntity;
import com.ruege.mobile.data.local.relation.CategoryWithContents;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CategoryEntity category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CategoryEntity> categories);

    @Update
    void update(CategoryEntity category);

    @Delete
    void delete(CategoryEntity category);

    @Query("SELECT * FROM categories WHERE category_id = :categoryId")
    LiveData<CategoryEntity> getCategoryById(String categoryId);

    @Query("SELECT * FROM categories ORDER BY order_position ASC")
    LiveData<List<CategoryEntity>> getAllCategories();

    @Query("SELECT * FROM categories WHERE is_visible = 1 ORDER BY order_position ASC")
    LiveData<List<CategoryEntity>> getVisibleCategories();

    @Transaction
    @Query("SELECT * FROM categories WHERE category_id = :categoryId")
    LiveData<CategoryWithContents> getCategoryWithContents(String categoryId);

    @Transaction
    @Query("SELECT * FROM categories ORDER BY order_position ASC")
    LiveData<List<CategoryWithContents>> getAllCategoriesWithContents();
} 