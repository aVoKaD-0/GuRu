package com.ruege.mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private val onVariantClickListener: OnVariantClickListener
) : ListAdapter<VariantEntity, VariantAdapter.VariantViewHolder>(DIFF_CALLBACK) {

    interface OnVariantClickListener {
        fun onVariantClick(variant: VariantEntity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VariantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_variant, parent, false)
        return VariantViewHolder(view)
    }

    override fun onBindViewHolder(holder: VariantViewHolder, position: Int) {
        val variant = getItem(position)
        holder.bind(variant, onVariantClickListener)
    }

    class VariantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvVariantName: TextView = itemView.findViewById(R.id.tv_variant_name)
        private val tvVariantTaskCount: TextView = itemView.findViewById(R.id.tv_variant_task_count)
        private val tvVariantStatus: TextView = itemView.findViewById(R.id.tv_variant_status)

        fun bind(variant: VariantEntity, listener: OnVariantClickListener) {
            tvVariantName.text = variant.name
            
            // Установка количества заданий
            val taskCountText = when (variant.taskCount) {
                0 -> "Нет заданий"
                1 -> "1 задание"
                in 2..4 -> "${variant.taskCount} задания"
                else -> "${variant.taskCount} заданий"
            }
            tvVariantTaskCount.text = taskCountText
            
            // Показываем статус (официальный/скачан), если применимо
            if (variant.isOfficial) {
                tvVariantStatus.visibility = View.VISIBLE
                tvVariantStatus.text = "Официальный"
            } else if (variant.isDownloaded) {
                tvVariantStatus.visibility = View.VISIBLE
                tvVariantStatus.text = "Скачан"
            } else {
                tvVariantStatus.visibility = View.GONE
            }
            
            // Установка обработчика клика на весь элемент
            itemView.setOnClickListener {
                listener.onVariantClick(variant)
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