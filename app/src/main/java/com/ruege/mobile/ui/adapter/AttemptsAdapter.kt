package com.ruege.mobile.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ruege.mobile.R
import com.ruege.mobile.viewmodel.PracticeAttemptWithTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttemptsAdapter(
    private val onTryAgainClick: (PracticeAttemptWithTask) -> Unit
) : ListAdapter<PracticeAttemptWithTask, AttemptsAdapter.AttemptViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttemptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_practice_attempt, parent, false)
        return AttemptViewHolder(view, onTryAgainClick)
    }

    override fun onBindViewHolder(holder: AttemptViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AttemptViewHolder(
        itemView: View,
        private val onTryAgainClick: (PracticeAttemptWithTask) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvTaskTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        private val tvAttemptDate: TextView = itemView.findViewById(R.id.tvAttemptDate)
        private val ivResult: ImageView = itemView.findViewById(R.id.ivResult)
        private val tvTaskText: TextView = itemView.findViewById(R.id.tvTaskText)
        private val tvTryAgain: TextView = itemView.findViewById(R.id.tvTryAgain)

        private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())

        fun bind(attemptWithTask: PracticeAttemptWithTask) {
            tvTaskTitle.text = "Задание ${attemptWithTask.egeNumber}"
            
            val attemptDate = if (attemptWithTask.attemptDate > 0) {
                dateTimeFormat.format(Date(attemptWithTask.attemptDate))
            } else {
                "Неизвестно"
            }
            tvAttemptDate.text = attemptDate
            
            // Устанавливаем иконку результата
            val iconResId = if (attemptWithTask.isCorrect) {
                android.R.drawable.ic_menu_send
            } else {
                android.R.drawable.ic_menu_close_clear_cancel
            }
            ivResult.setImageResource(iconResId)
            
            // Устанавливаем цвет иконки в зависимости от результата
            val colorResId = if (attemptWithTask.isCorrect) {
                android.R.color.holo_green_dark
            } else {
                android.R.color.holo_red_dark
            }
            ivResult.setColorFilter(itemView.context.getColor(colorResId))
            
            // Отображаем краткий текст задания
            tvTaskText.text = attemptWithTask.taskEntity.taskText ?: "Нет текста задания"
            
            // Обработчик нажатия на кнопку "Попробовать снова"
            tvTryAgain.setOnClickListener {
                onTryAgainClick(attemptWithTask)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PracticeAttemptWithTask>() {
            override fun areItemsTheSame(oldItem: PracticeAttemptWithTask, newItem: PracticeAttemptWithTask): Boolean {
                return oldItem.attempt.attemptId == newItem.attempt.attemptId
            }

            override fun areContentsTheSame(oldItem: PracticeAttemptWithTask, newItem: PracticeAttemptWithTask): Boolean {
                return oldItem.isCorrect == newItem.isCorrect &&
                       oldItem.attemptDate == newItem.attemptDate &&
                       oldItem.taskEntity.id == newItem.taskEntity.id
            }
        }
    }
} 