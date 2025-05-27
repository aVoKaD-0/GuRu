package com.ruege.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ruege.mobile.data.local.entity.VariantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VariantDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateVariant(variant: VariantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateVariants(variants: List<VariantEntity>)

    @Query("SELECT * FROM variants ORDER BY last_accessed_at DESC, created_at DESC")
    fun getAllVariants(): Flow<List<VariantEntity>>

    @Query("SELECT * FROM variants WHERE variant_id = :variantId")
    suspend fun getVariantById(variantId: Int): VariantEntity?

    @Query("SELECT * FROM variants WHERE variant_id = :variantId")
    fun getVariantByIdFlow(variantId: Int): Flow<VariantEntity?>

    @Update
    suspend fun updateVariant(variant: VariantEntity)

    @Query("DELETE FROM variants WHERE variant_id = :variantId")
    suspend fun deleteVariantById(variantId: Int)

    @Query("UPDATE variants SET last_accessed_at = :timestamp WHERE variant_id = :variantId")
    suspend fun updateLastAccessedTimestamp(variantId: Int, timestamp: String)
    
    @Query("UPDATE variants SET is_downloaded = :isDownloaded WHERE variant_id = :variantId")
    suspend fun updateDownloadStatus(variantId: Int, isDownloaded: Boolean)

    @Query("SELECT * FROM variants WHERE is_downloaded = 1")
    fun getDownloadedVariants(): Flow<List<VariantEntity>>

    @Query("DELETE FROM variants")
    suspend fun clearAllVariants()
} 