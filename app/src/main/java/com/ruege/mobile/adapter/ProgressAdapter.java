package com.ruege.mobile.adapter;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.TypedValue;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.ruege.mobile.R;
import com.ruege.mobile.model.ProgressItem;
import com.ruege.mobile.util.ProgressDiffCallback;

import java.util.ArrayList;
import java.util.List;

public class ProgressAdapter extends RecyclerView.Adapter<ProgressAdapter.ProgressViewHolder> {

    private List<ProgressItem> progressItems;
    private final OnProgressClickListener onProgressClickListener;
    
    public interface OnProgressClickListener {
        void onProgressClick(ProgressItem progressItem);
    }

    public ProgressAdapter(List<ProgressItem> progressItems, OnProgressClickListener listener) {
        this.progressItems = new ArrayList<>(progressItems == null ? List.of() : progressItems);
        this.onProgressClickListener = listener;
    }

    @NonNull
    @Override
    public ProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_progress, parent, false);
        return new ProgressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressViewHolder holder, int position) {
        ProgressItem item = progressItems.get(position);
        holder.titleTextView.setText(item.getTitle());
        
        // Логирование для отладки
        android.util.Log.d("ProgressAdapter", "Позиция " + position + 
                          ": id=" + item.getId() + 
                          ", title=" + item.getTitle() + 
                          ", type=" + item.getType() + 
                          ", percentage=" + item.getPercentage());
        
        // Устанавливаем описание, если оно есть
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            holder.descriptionTextView.setText(item.getDescription());
            holder.descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionTextView.setVisibility(View.GONE);
        }
        
        // Устанавливаем прогресс
        holder.progressBar.setProgress(item.getPercentage());
        holder.percentageTextView.setText(item.getPercentage() + "%");
        
        // Отображаем индикатор статуса выполнения (если найден)
        if (holder.statusIndicator != null) {
            if (item.getPercentage() >= 100) {
                // Задание выполнено
                holder.statusIndicator.setVisibility(View.VISIBLE);
                holder.statusIndicator.setImageResource(R.drawable.ic_done);
                holder.statusIndicator.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.success));
            } else if (item.getPercentage() > 0) {
                // Задание в процессе
                holder.statusIndicator.setVisibility(View.VISIBLE);
                holder.statusIndicator.setImageResource(R.drawable.ic_in_progress);
                holder.statusIndicator.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.warning));
            } else {
                // Задание не начато
                holder.statusIndicator.setVisibility(View.GONE);
            }
        }
        
        // Более надежная проверка типа элемента
        String itemType = item.getType();
        boolean isProgressType = "PROGRESS".equals(itemType);
        boolean isTaskType = "TASK".equals(itemType);
        
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        
        // Проверка темного режима
        boolean isDarkMode = isDarkMode(holder.itemView.getContext());
        android.util.Log.d("ProgressAdapter", "isDarkMode: " + isDarkMode);
        
        // Разная визуализация в зависимости от типа
        if (isProgressType) {
            // Общий прогресс - делаем карточку шире
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            holder.itemView.setLayoutParams(params);
            
            // Применяем разный фон в зависимости от темы
            if (isDarkMode) {
                holder.itemView.setBackgroundResource(R.drawable.progress_card_background_primary_dark);
                holder.titleTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary_dark));
                holder.descriptionTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary_dark));
            } else {
                holder.itemView.setBackgroundResource(R.drawable.progress_card_background_primary);
            }
            
            holder.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            holder.percentageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            
            // Для общего прогресса скрываем индикатор статуса, но оставляем видимым progressBar
            if (holder.statusIndicator != null) {
                holder.statusIndicator.setVisibility(View.GONE);
            }
            
            // Увеличиваем длину progress_bar для общего прогресса
            ViewGroup.LayoutParams progressParams = holder.progressBar.getLayoutParams();
            progressParams.width = dpToPx(holder.itemView.getContext(), 150);
            holder.progressBar.setLayoutParams(progressParams);
            
            // Всегда показываем линию прогресса для общего прогресса, даже если процент равен 0
            holder.progressBar.setVisibility(View.VISIBLE);
            
            android.util.Log.d("ProgressAdapter", "Элемент " + item.getId() + " отображается как PROGRESS карточка");
        } else if (isTaskType) {
            // Прогресс задания - стандартная карточка
            // Ширина задается в XML разметке item_progress.xml ()
            params.width = dpToPx(holder.itemView.getContext(), 92);
            holder.itemView.setLayoutParams(params);
            
            // Применяем разный фон в зависимости от темы
            if (isDarkMode) {
                holder.itemView.setBackgroundResource(R.drawable.progress_card_background_dark);
                holder.titleTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary_dark));
                holder.descriptionTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary_dark));
            } else {
                holder.itemView.setBackgroundResource(R.drawable.progress_card_background);
            }
            
            // Восстанавливаем стандартный размер progress_bar
            ViewGroup.LayoutParams progressParams = holder.progressBar.getLayoutParams();
            progressParams.width = dpToPx(holder.itemView.getContext(), 75);
            holder.progressBar.setLayoutParams(progressParams);
            
            holder.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            holder.percentageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            
            android.util.Log.d("ProgressAdapter", "Элемент " + item.getId() + " отображается как TASK карточка");
        } else {
            // Неизвестный тип - обрабатываем как задание по умолчанию
            android.util.Log.w("ProgressAdapter", "Предупреждение: неизвестный тип элемента: " + itemType);
            
            params.width = dpToPx(holder.itemView.getContext(), 90);
            holder.itemView.setLayoutParams(params);
            
            // Применяем разный фон в зависимости от темы
            if (isDarkMode) {
                holder.itemView.setBackgroundResource(R.drawable.progress_card_background_dark);
                holder.titleTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary_dark));
                holder.descriptionTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary_dark));
            } else {
                holder.itemView.setBackgroundResource(R.drawable.progress_card_background);
            }
            
            holder.titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            holder.percentageTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }

        holder.itemView.setOnClickListener(v -> onProgressClickListener.onProgressClick(item));
    }

    /**
     * Проверяет, включен ли темный режим
     */
    private boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & 
                            Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public int getItemCount() {
        return progressItems.size();
    }
    
    public void submitList(List<ProgressItem> newItems) {
        List<ProgressItem> newProgressList = new ArrayList<>(newItems == null ? List.of() : newItems);
        ProgressDiffCallback diffCallback = new ProgressDiffCallback(this.progressItems, newProgressList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        
        this.progressItems.clear();
        this.progressItems.addAll(newProgressList);
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Обновляет список элементов прогресса
     * @param newItems новый список элементов
     */
    public void updateItems(List<ProgressItem> newItems) {
        // Защита от null
        List<ProgressItem> safeNewItems = newItems != null ? newItems : new ArrayList<>();
        
        // Логгирование для отладки
        android.util.Log.d("ProgressAdapter", "Обновление элементов, новых элементов: " + safeNewItems.size());
        
        // Выводим информацию о новых элементах
        for (int i = 0; i < safeNewItems.size(); i++) {
            ProgressItem item = safeNewItems.get(i);
            android.util.Log.d("ProgressAdapter", "Новый элемент " + i + 
                              ": id=" + item.getId() + 
                              ", title=" + item.getTitle() + 
                              ", type=" + item.getType() + 
                              ", percentage=" + item.getPercentage());
        }
        
        // Используем DiffUtil для эффективного обновления
        ProgressDiffCallback diffCallback = new ProgressDiffCallback(this.progressItems, safeNewItems);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        
        this.progressItems.clear();
        this.progressItems.addAll(safeNewItems);
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Конвертирует dp в пиксели
     */
    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    static class ProgressViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        ProgressBar progressBar;
        TextView percentageTextView;
        ImageView statusIndicator;

        public ProgressViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.progress_title);
            descriptionTextView = itemView.findViewById(R.id.progress_description);
            progressBar = itemView.findViewById(R.id.progress_bar);
            percentageTextView = itemView.findViewById(R.id.progress_percent);
            statusIndicator = itemView.findViewById(R.id.progress_status);
        }
    }
} 