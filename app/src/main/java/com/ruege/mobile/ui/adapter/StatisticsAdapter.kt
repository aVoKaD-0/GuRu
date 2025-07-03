package com.ruege.mobile.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ruege.mobile.R
import com.ruege.mobile.data.local.entity.PracticeStatisticsEntity
import com.ruege.mobile.databinding.ItemPracticeStatisticsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsAdapter(
    private val onPracticeClick: (String) -> Unit,
    private val onStatisticsClick: (String) -> Unit
) : ListAdapter<PracticeStatisticsEntity, StatisticsAdapter.StatisticsViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatisticsViewHolder {
        val binding = ItemPracticeStatisticsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StatisticsViewHolder(binding, onPracticeClick, onStatisticsClick)
    }

    override fun onBindViewHolder(holder: StatisticsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StatisticsViewHolder(
        private val binding: ItemPracticeStatisticsBinding,
        private val onPracticeClick: (String) -> Unit,
        private val onStatisticsClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        fun bind(statistics: PracticeStatisticsEntity) {
            if (statistics.egeNumber.startsWith("essay:")) {
                binding.tvEgeNumber.text = statistics.egeNumber.removePrefix("essay:")
                binding.tvPractice.visibility = View.GONE
            } else if (statistics.egeNumber.contains("_")) {
                val variantName = statistics.egeNumber.substringBeforeLast('_')
                binding.tvEgeNumber.text = variantName
                binding.tvPractice.visibility = View.GONE
            } else {
                binding.tvEgeNumber.text = "Задание ${statistics.egeNumber}"
                binding.tvPractice.visibility = View.VISIBLE
                binding.tvPractice.setOnClickListener {
                    onPracticeClick(statistics.egeNumber)
                }
            }

            binding.tvAttemptsCount.text = "Попыток: ${statistics.totalAttempts}"
            
            val successRate = statistics.successRate
            if (statistics.egeNumber.startsWith("essay:")) {
                binding.tvSuccessRate.text = "Проверено"
                binding.progressSuccess.visibility = View.GONE
            } else {
                binding.tvSuccessRate.text = "Успешность: ${String.format("%.1f", successRate)}%"
                binding.progressSuccess.progress = successRate.toInt()
                binding.progressSuccess.visibility = View.VISIBLE
            }
            
            val colorResId = when {
                successRate >= 80 -> android.R.color.holo_green_dark
                successRate >= 50 -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }
            binding.progressSuccess.progressTintList = android.content.res.ColorStateList.valueOf(
                itemView.context.getColor(colorResId)
            )
            
            val lastAttemptDate = if (statistics.lastAttemptDate > 0) {
                "Последняя попытка: ${dateFormat.format(Date(statistics.lastAttemptDate))}"
            } else {
                "Нет попыток"
            }
            binding.tvLastAttempt.text = lastAttemptDate
            
            if (!statistics.egeNumber.contains("_")) {
                binding.tvPractice.setOnClickListener {
                    onPracticeClick(statistics.egeNumber)
                }
            }
            
            val variantData = statistics.variantData
            if (variantData != null && variantData.isNotEmpty()) {
                binding.ivVariantData.visibility = View.VISIBLE
                
                itemView.setOnClickListener {
                    onStatisticsClick(statistics.egeNumber)
                }
            } else {
                binding.ivVariantData.visibility = View.GONE
                if (!statistics.egeNumber.contains("_")) {
                     itemView.setOnClickListener {
                        onStatisticsClick(statistics.egeNumber)
                    }
                } else {
                    itemView.setOnClickListener(null)
                }
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
                       oldItem.lastAttemptDate == newItem.lastAttemptDate &&
                       oldItem.variantData == newItem.variantData
            }
        }
    }
} 