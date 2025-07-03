package com.ruege.mobile.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ruege.mobile.databinding.ItemEssayBinding
import com.ruege.mobile.model.TaskItem

class EssayAdapter(private val listener: EssayItemClickListener) : RecyclerView.Adapter<EssayAdapter.EssayViewHolder>() {

    private val items = mutableListOf<TaskItem>()

    fun updateItems(newItems: List<TaskItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EssayViewHolder {
        val binding = ItemEssayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EssayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EssayViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class EssayViewHolder(private val binding: ItemEssayBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: TaskItem) {
            binding.essayTitle.text = item.title
            binding.essayDescription.text = "Нажмите, чтобы приступить к написанию"
            
            binding.root.setOnClickListener {
                listener.onEssayItemClick(item)
            }
        }
    }
    
    interface EssayItemClickListener {
        fun onEssayItemClick(item: TaskItem)
    }
} 