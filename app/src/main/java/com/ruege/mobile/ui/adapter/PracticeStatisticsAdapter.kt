package com.ruege.mobile.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ruege.mobile.databinding.ItemPracticeStatisticsBinding
import com.ruege.mobile.model.PracticeStatisticItem

class PracticeStatisticsAdapter(

) : ListAdapter<PracticeStatisticItem, PracticeStatisticsAdapter.ViewHolder>(PracticeStatisticDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPracticeStatisticsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPracticeStatisticsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PracticeStatisticItem) {
            binding.tvEgeNumber.text = item.egeDisplayNumber
            binding.tvAttemptsCount.text = "Попыток: ${item.totalAttempts}"
            binding.tvSuccessRate.text = "Успешность: ${item.successRate}%"
            binding.progressSuccess.progress = item.successRate
            binding.tvLastAttempt.text = "Последняя: ${item.lastAttemptDateFormatted}"
            
            binding.tvPractice.visibility = View.GONE 
        }
    }
}

class PracticeStatisticDiffCallback : DiffUtil.ItemCallback<PracticeStatisticItem>() {
    override fun areItemsTheSame(oldItem: PracticeStatisticItem, newItem: PracticeStatisticItem): Boolean {
        return oldItem.id == newItem.id 
    }

    override fun areContentsTheSame(oldItem: PracticeStatisticItem, newItem: PracticeStatisticItem): Boolean {
        return oldItem == newItem 
    }
} 