package com.ruege.mobile.ui.sync

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.ruege.mobile.R
import com.ruege.mobile.data.local.entity.ProgressSyncQueueEntity
import com.ruege.mobile.data.local.entity.SyncStatus
import com.ruege.mobile.ui.viewmodel.ProgressViewModel

/**
 * Менеджер для отображения и управления статусом синхронизации в UI
 */
class SyncStatusManager(
    private val context: Context,
    private val rootView: View,
    private val progressViewModel: ProgressViewModel
) : DefaultLifecycleObserver {
    private val syncStatusIcon: ImageView = rootView.findViewById(R.id.sync_status_icon)
    private val syncStatusText: TextView = rootView.findViewById(R.id.sync_status_text)
    private val pendingCountText: TextView = rootView.findViewById(R.id.pending_count)
    private val failedCountText: TextView = rootView.findViewById(R.id.failed_count)
    private val totalCountText: TextView = rootView.findViewById(R.id.total_count)
    private val syncNowButton: Button = rootView.findViewById(R.id.btn_sync_now)
    private val clearFailedButton: Button = rootView.findViewById(R.id.btn_clear_failed)
    
    private var pendingCount = 0
    private var failedCount = 0
    private var totalCount = 0
    
    init {
        syncNowButton.setOnClickListener {
            progressViewModel.syncNow()
            startSyncAnimation()
        }
        
        clearFailedButton.setOnClickListener {
            progressViewModel.clearSyncQueueByStatus(SyncStatus.FAILED)
        }
    }
    
    /**
     * Инициализирует наблюдателей за данными синхронизации
     */
    fun initialize(lifecycleOwner: LifecycleOwner) {
        progressViewModel.syncQueueLiveData.observe(lifecycleOwner) { syncItems: List<ProgressSyncQueueEntity>? ->
            syncItems?.let { updateSyncStatus(it) }
        }
        
        progressViewModel.pendingItemsCount.observe(lifecycleOwner) { count ->
            pendingCount = count?.toInt() ?: 0
            pendingCountText.text = pendingCount.toString()
            updateTotalCount()
            updateSyncStatusUI()
        }
        
        progressViewModel.failedItemsCount.observe(lifecycleOwner) { count ->
            failedCount = count?.toInt() ?: 0
            failedCountText.text = failedCount.toString()
            updateTotalCount()
            updateSyncStatusUI()
            
            clearFailedButton.isEnabled = failedCount > 0
        }
    }
    
    /**
     * Обновляет общий счетчик элементов в очереди
     */
    private fun updateTotalCount() {
        totalCount = pendingCount + failedCount
        totalCountText.text = totalCount.toString()
    }
    
    /**
     * Обновляет UI статуса синхронизации на основе текущих данных
     */
    private fun updateSyncStatusUI() {
        when {
            failedCount > 0 -> {
                syncStatusIcon.setImageResource(R.drawable.ic_sync_problem)
                syncStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_light))
                syncStatusText.text = "Есть ошибки синхронизации"
                syncStatusText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
            }
            pendingCount > 0 -> {
                syncStatusIcon.setImageResource(R.drawable.ic_sync_pending)
                syncStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_orange_light))
                syncStatusText.text = "Ожидает синхронизации"
                syncStatusText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_light))
            }
            else -> {
                syncStatusIcon.setImageResource(R.drawable.ic_sync_done)
                syncStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_light))
                syncStatusText.text = "Синхронизировано"
                syncStatusText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
            }
        }
    }
    
    /**
     * Обновляет статус синхронизации на основе списка элементов очереди
     */
    private fun updateSyncStatus(syncItems: List<ProgressSyncQueueEntity>) {
        totalCount = syncItems.size
        totalCountText.text = totalCount.toString()
        
        if (syncItems.isEmpty()) {
            syncStatusIcon.setImageResource(R.drawable.ic_sync_done)
            syncStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_light))
            syncStatusText.text = "Синхронизировано"
            syncStatusText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
            
            clearFailedButton.isEnabled = false
            
            pendingCount = 0
            failedCount = 0
            pendingCountText.text = "0"
            failedCountText.text = "0"
            return
        }
        
        var hasFailed = false
        var isSyncing = false
        
        pendingCount = syncItems.count { it.syncStatus == SyncStatus.PENDING }
        failedCount = syncItems.count { it.syncStatus == SyncStatus.FAILED }
        
        pendingCountText.text = pendingCount.toString()
        failedCountText.text = failedCount.toString()
        
        isSyncing = syncItems.any { it.syncStatus == SyncStatus.SYNCING }
        hasFailed = failedCount > 0
        
        when {
            isSyncing -> {
                startSyncAnimation()
                syncStatusText.text = "Синхронизация..."
                syncStatusText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_light))
            }
            hasFailed -> {
                syncStatusIcon.setImageResource(R.drawable.ic_sync_problem)
                syncStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_light))
                syncStatusText.text = "Есть ошибки синхронизации"
                syncStatusText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
            }
            pendingCount > 0 -> {
                syncStatusIcon.setImageResource(R.drawable.ic_sync_pending)
                syncStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_orange_light))
                syncStatusText.text = "Ожидает синхронизации"
                syncStatusText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_light))
            }
            else -> {
                syncStatusIcon.setImageResource(R.drawable.ic_sync_done)
                syncStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_light))
                syncStatusText.text = "Синхронизировано"
                syncStatusText.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
            }
        }
        clearFailedButton.isEnabled = hasFailed
    }
    
    /**
     * Запускает анимацию синхронизации
     */
    private fun startSyncAnimation() {
        val drawable = AppCompatResources.getDrawable(context, R.drawable.anim_sync)
        syncStatusIcon.setImageDrawable(drawable)
        syncStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_blue_light))
        if (drawable is AnimatedVectorDrawable) {
            drawable.start()
        }
    }
    
    /**
     * Отображает общее состояние синхронизации одной строкой
     */
    fun getSyncStatusSummary(): String {
        return when {
            failedCount > 0 -> "Ошибки синхронизации: $failedCount"
            pendingCount > 0 -> "Ожидают синхронизации: $pendingCount"
            else -> "Синхронизировано"
        }
    }
    
    /**
     * Метод жизненного цикла при паузе активности
     */
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
    }
    
    /**
     * Метод жизненного цикла при уничтожении активности
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
    }
} 