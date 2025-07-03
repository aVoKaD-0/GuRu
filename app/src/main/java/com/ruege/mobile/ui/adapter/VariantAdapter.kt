package com.ruege.mobile.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ruege.mobile.R
import com.ruege.mobile.data.local.entity.VariantEntity
import com.ruege.mobile.databinding.ItemVariantBinding

class VariantAdapter(
    private val onVariantClickListener: OnVariantClickListener,
    private val onItemSelectionListener: OnItemSelectionListener,
    private val onItemDeleteListener: OnItemDeleteListener
) : ListAdapter<VariantEntity, VariantAdapter.VariantViewHolder>(DIFF_CALLBACK) {

    interface OnVariantClickListener {
        fun onVariantClick(variant: VariantEntity)
    }

    interface OnItemSelectionListener {
        fun onSelectionChanged(variant: VariantEntity, isSelected: Boolean)
    }

    interface OnItemDeleteListener {
        fun onDeleteClick(variant: VariantEntity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VariantViewHolder {
        val binding = ItemVariantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VariantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VariantViewHolder, position: Int) {
        val variant = getItem(position)
        holder.bind(variant, onVariantClickListener, onItemSelectionListener, onItemDeleteListener)
    }

    class VariantViewHolder(private val binding: ItemVariantBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            variant: VariantEntity,
            clickListener: OnVariantClickListener,
            selectionListener: OnItemSelectionListener,
            deleteListener: OnItemDeleteListener
        ) {
            binding.tvVariantName.text = variant.name
            
            val taskCountText = when (val count = variant.taskCount) {
                0 -> "Нет заданий"
                1 -> "1 задание"
                in 2..4 -> "$count задания"
                else -> "$count заданий"
            }
            binding.tvVariantTaskCount.text = taskCountText
            
            if (variant.isDownloaded) {
                binding.variantDownloadedIcon.visibility = View.VISIBLE
                binding.variantCheckbox.visibility = View.GONE
                binding.variantDownloadedIcon.isClickable = true
                binding.variantDownloadedIcon.setOnClickListener {
                    deleteListener.onDeleteClick(variant)
                }
            } else {
                binding.variantDownloadedIcon.visibility = View.GONE
                binding.variantCheckbox.visibility = View.VISIBLE
                binding.variantCheckbox.isChecked = variant.isSelected
                binding.variantDownloadedIcon.isClickable = false
                binding.variantDownloadedIcon.setOnClickListener(null)
            }
            
            if (variant.isOfficial) {
                binding.tvVariantStatus.visibility = View.VISIBLE
                binding.tvVariantStatus.text = "Официальный"
            } else {
                binding.tvVariantStatus.visibility = View.GONE
            }

            binding.variantCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (binding.variantCheckbox.isPressed) {
                    selectionListener.onSelectionChanged(variant, isChecked)
                }
            }
            
            itemView.setOnClickListener {
                if(variant.isDownloaded) {
                    clickListener.onVariantClick(variant)
                } else {
                    selectionListener.onSelectionChanged(variant, !variant.isSelected)
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<VariantEntity>() {
            override fun areItemsTheSame(oldItem: VariantEntity, newItem: VariantEntity): Boolean {
                return oldItem.variantId == newItem.variantId
            }

            override fun areContentsTheSame(oldItem: VariantEntity, newItem: VariantEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
} 