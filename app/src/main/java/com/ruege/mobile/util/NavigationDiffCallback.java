package com.ruege.mobile.util;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.ruege.mobile.model.NavigationItem; 

import java.util.List;
import java.util.Objects;

public class NavigationDiffCallback extends DiffUtil.Callback {

    private final List<NavigationItem> oldList;
    private final List<NavigationItem> newList;

    public NavigationDiffCallback(List<NavigationItem> oldList, List<NavigationItem> newList) {
        this.oldList = oldList == null ? List.of() : oldList;
        this.newList = newList == null ? List.of() : newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        
        
        NavigationItem oldItem = oldList.get(oldItemPosition);
        NavigationItem newItem = newList.get(newItemPosition);
        
        
        return Objects.equals(oldItem.getTitle(), newItem.getTitle()); 
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        
        NavigationItem oldItem = oldList.get(oldItemPosition);
        NavigationItem newItem = newList.get(newItemPosition);
        return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                && oldItem.getIconResId() == newItem.getIconResId();
        
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}