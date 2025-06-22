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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_variant, parent, false)
        return VariantViewHolder(view)
    }

    override fun onBindViewHolder(holder: VariantViewHolder, position: Int) {
        val variant = getItem(position)
        holder.bind(variant, onVariantClickListener, onItemSelectionListener, onItemDeleteListener)
    }

    class VariantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvVariantName: TextView = itemView.findViewById(R.id.tv_variant_name)
        private val tvVariantTaskCount: TextView = itemView.findViewById(R.id.tv_variant_task_count)
        private val tvVariantStatus: TextView = itemView.findViewById(R.id.tv_variant_status)
        private val checkBox: CheckBox = itemView.findViewById(R.id.variant_checkbox)
        private val downloadedIcon: ImageView = itemView.findViewById(R.id.variant_downloaded_icon)

        fun bind(
            variant: VariantEntity,
            clickListener: OnVariantClickListener,
            selectionListener: OnItemSelectionListener,
            deleteListener: OnItemDeleteListener
        ) {
            tvVariantName.text = variant.name
            
            val taskCountText = when (val count = variant.taskCount) {
                0 -> "Нет заданий"
                1 -> "1 задание"
                in 2..4 -> "$count задания"
                else -> "$count заданий"
            }
            tvVariantTaskCount.text = taskCountText
            
            if (variant.isDownloaded) {
                downloadedIcon.visibility = View.VISIBLE
                checkBox.visibility = View.GONE
                downloadedIcon.isClickable = true
                downloadedIcon.setOnClickListener {
                    deleteListener.onDeleteClick(variant)
                }
            } else {
                downloadedIcon.visibility = View.GONE
                checkBox.visibility = View.VISIBLE
                checkBox.isChecked = variant.isSelected
                downloadedIcon.isClickable = false
                downloadedIcon.setOnClickListener(null)
            }
            
            if (variant.isOfficial) {
                tvVariantStatus.visibility = View.VISIBLE
                tvVariantStatus.text = "Официальный"
            } else {
                tvVariantStatus.visibility = View.GONE
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (checkBox.isPressed) {
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