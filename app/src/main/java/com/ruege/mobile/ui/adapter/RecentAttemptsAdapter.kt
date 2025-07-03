package com.ruege.mobile.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ruege.mobile.R
import com.ruege.mobile.model.PracticeAttemptItemUiModel
import com.ruege.mobile.databinding.ItemRecentAttemptBinding
import java.text.SimpleDateFormat
import java.util.Locale

class RecentAttemptsAdapter : ListAdapter<PracticeAttemptItemUiModel, RecentAttemptsAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentAttemptBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, dateFormat)
    }

    class ViewHolder(private val binding: ItemRecentAttemptBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PracticeAttemptItemUiModel, dateFormat: SimpleDateFormat) {
            binding.tvAttemptTaskTitle.text = item.egeTaskNumberDisplay
            binding.tvAttemptDate.text = item.attemptDateFormatted 

            if (item.isCorrect) {
                binding.tvAttemptResult.text = itemView.context.getString(R.string.correct)
                binding.tvAttemptResult.setTextColor(ContextCompat.getColor(itemView.context, R.color.md_theme_light_tertiary))
                binding.ivStatusIndicator.setImageResource(R.drawable.ic_circle_check_green)
            } else {
                binding.tvAttemptResult.text = itemView.context.getString(R.string.incorrect)
                binding.tvAttemptResult.setTextColor(ContextCompat.getColor(itemView.context, R.color.md_theme_light_error)) 
                binding.ivStatusIndicator.setImageResource(R.drawable.ic_circle_cross_red)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PracticeAttemptItemUiModel>() {
        override fun areItemsTheSame(oldItem: PracticeAttemptItemUiModel, newItem: PracticeAttemptItemUiModel): Boolean {
            return oldItem.attemptId == newItem.attemptId
        }

        override fun areContentsTheSame(oldItem: PracticeAttemptItemUiModel, newItem: PracticeAttemptItemUiModel): Boolean {
            return oldItem == newItem
        }
    }
} 