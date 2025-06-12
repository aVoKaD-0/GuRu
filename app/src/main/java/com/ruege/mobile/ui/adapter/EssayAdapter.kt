package com.ruege.mobile.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ruege.mobile.data.local.entity.ContentEntity
import com.ruege.mobile.databinding.ItemTheoryBinding

class EssayAdapter(private val listener: EssayItemClickListener) : RecyclerView.Adapter<EssayAdapter.EssayViewHolder>() {

    private val items = mutableListOf<ContentEntity>()

    fun updateItems(newItems: List<ContentEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EssayViewHolder {
        val binding = ItemTheoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EssayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EssayViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class EssayViewHolder(private val binding: ItemTheoryBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: ContentEntity) {
            binding.theoryTitle.text = item.title
            binding.theoryDescription.text = item.description ?: ""
            
            binding.root.setOnClickListener {
                listener.onEssayItemClick(item)
            }
        }
    }
    
    interface EssayItemClickListener {
        fun onEssayItemClick(item: ContentEntity)
    }
} 