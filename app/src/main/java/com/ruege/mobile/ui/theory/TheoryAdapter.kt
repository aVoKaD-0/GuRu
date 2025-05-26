package com.ruege.mobile.ui.theory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ruege.mobile.data.local.entity.ContentEntity
import com.ruege.mobile.databinding.ItemTheoryBinding

class TheoryAdapter(private val listener: TheoryItemClickListener) : RecyclerView.Adapter<TheoryAdapter.TheoryViewHolder>() {

    private val items = mutableListOf<ContentEntity>()

    fun updateItems(newItems: List<ContentEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TheoryViewHolder {
        val binding = ItemTheoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TheoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TheoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TheoryViewHolder(private val binding: ItemTheoryBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: ContentEntity) {
            binding.theoryTitle.text = item.title
            binding.theoryDescription.text = item.description
            
            binding.root.setOnClickListener {
                listener.onTheoryItemClick(item)
            }
        }
    }
    
    interface TheoryItemClickListener {
        fun onTheoryItemClick(item: ContentEntity)
    }
} 