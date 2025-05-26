package com.ruege.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruege.mobile.data.local.entity.VariantSharedTextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VariantSharedTextDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSharedText(sharedText: VariantSharedTextEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSharedTexts(sharedTexts: List<VariantSharedTextEntity>)

    @Query("SELECT * FROM variant_shared_texts WHERE variant_id = :variantId")
    fun getSharedTextsForVariant(variantId: Int): Flow<List<VariantSharedTextEntity>>
    
    @Query("SELECT * FROM variant_shared_texts WHERE variant_shared_text_id = :sharedTextId")
    suspend fun getSharedTextById(sharedTextId: Int): VariantSharedTextEntity?

    @Query("DELETE FROM variant_shared_texts WHERE variant_id = :variantId")
    suspend fun deleteSharedTextsForVariant(variantId: Int)

    @Query("DELETE FROM variant_shared_texts WHERE variant_shared_text_id = :sharedTextId")
    suspend fun deleteSharedTextById(sharedTextId: Int)

    @Query("DELETE FROM variant_shared_texts")
    suspend fun clearAllSharedTexts() // Для служебных целей

    @Query("SELECT * FROM variant_shared_texts WHERE variant_id = :variantId ORDER BY variant_shared_text_id ASC") // или другая логика сортировки
    fun getSharedTextsByVariantIdFlow(variantId: Int): Flow<List<VariantSharedTextEntity>>
} 