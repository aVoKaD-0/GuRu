package com.ruege.mobile.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ruege.mobile.R
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsAdapter(
    private val onPracticeClick: (String) -> Unit
) : ListAdapter<PracticeStatisticsEntity, StatisticsAdapter.StatisticsViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_practice_statistics, parent, false)
        return StatisticsViewHolder(view, onPracticeClick)
    }

    override fun onBindViewHolder(holder: StatisticsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StatisticsViewHolder(
        itemView: View,
        private val onPracticeClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvEgeNumber: TextView = itemView.findViewById(R.id.tvEgeNumber)
        private val tvAttemptsCount: TextView = itemView.findViewById(R.id.tvAttemptsCount)
        private val tvSuccessRate: TextView = itemView.findViewById(R.id.tvSuccessRate)
        private val progressSuccess: ProgressBar = itemView.findViewById(R.id.progressSuccess)
        private val tvLastAttempt: TextView = itemView.findViewById(R.id.tvLastAttempt)
        private val tvPractice: TextView = itemView.findViewById(R.id.tvPractice)

        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        fun bind(statistics: PracticeStatisticsEntity) {
            tvEgeNumber.text = "Задание ${statistics.egeNumber}"
            tvAttemptsCount.text = "Попыток: ${statistics.totalAttempts}"
            
            val successRate = statistics.successRate
            tvSuccessRate.text = "Успешность: ${String.format("%.1f", successRate)}%"
            progressSuccess.progress = successRate.toInt()
            
            // Изменение цвета прогресс-бара в зависимости от успешности
            val colorResId = when {
                successRate >= 80 -> android.R.color.holo_green_dark
                successRate >= 50 -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }
            progressSuccess.progressTintList = android.content.res.ColorStateList.valueOf(
                itemView.context.getColor(colorResId)
            )
            
            val lastAttemptDate = if (statistics.lastAttemptDate > 0) {
                "Последняя попытка: ${dateFormat.format(Date(statistics.lastAttemptDate))}"
            } else {
                "Нет попыток"
            }
            tvLastAttempt.text = lastAttemptDate
            
            tvPractice.setOnClickListener {
                onPracticeClick(statistics.egeNumber)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PracticeStatisticsEntity>() {
            override fun areItemsTheSame(oldItem: PracticeStatisticsEntity, newItem: PracticeStatisticsEntity): Boolean {
                return oldItem.egeNumber == newItem.egeNumber
            }

            override fun areContentsTheSame(oldItem: PracticeStatisticsEntity, newItem: PracticeStatisticsEntity): Boolean {
                return oldItem.totalAttempts == newItem.totalAttempts &&
                       oldItem.correctAttempts == newItem.correctAttempts &&
                       oldItem.lastAttemptDate == newItem.lastAttemptDate
            }
        }
    }
} 