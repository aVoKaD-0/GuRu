package com.ruege.mobile.util;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.ruege.mobile.model.ProgressItem; 

import java.util.List;
import java.util.Objects; 

/**
 * Класс для сравнения списков прогресса для оптимального обновления UI
 */
public class ProgressDiffCallback extends DiffUtil.Callback {

    private final List<ProgressItem> oldItems;
    private final List<ProgressItem> newItems;

    /**
     * Конструктор для сравнения списков прогресса
     * @param oldItems старый список
     * @param newItems новый список
     */
    public ProgressDiffCallback(List<ProgressItem> oldItems, List<ProgressItem> newItems) {
        this.oldItems = oldItems;
        this.newItems = newItems;
    }

    @Override
    public int getOldListSize() {
        return oldItems.size();
    }

    @Override
    public int getNewListSize() {
        return newItems.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        ProgressItem oldItem = oldItems.get(oldItemPosition);
        ProgressItem newItem = newItems.get(newItemPosition);
        
        // Сравниваем элементы по ID
        return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        ProgressItem oldItem = oldItems.get(oldItemPosition);
        ProgressItem newItem = newItems.get(newItemPosition);
        
        // Сравниваем данные элементов
        boolean sameTitles = oldItem.getTitle().equals(newItem.getTitle());
        boolean sameDescriptions = (oldItem.getDescription() == null && newItem.getDescription() == null) ||
                (oldItem.getDescription() != null && oldItem.getDescription().equals(newItem.getDescription()));
        boolean samePercentages = oldItem.getPercentage() == newItem.getPercentage();
        boolean sameTypes = (oldItem.getType() == null && newItem.getType() == null) ||
                (oldItem.getType() != null && oldItem.getType().equals(newItem.getType()));
        
        return sameTitles && sameDescriptions && samePercentages && sameTypes;
    }

    
    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        
        
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}