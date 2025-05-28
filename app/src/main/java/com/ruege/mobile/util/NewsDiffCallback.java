package com.ruege.mobile.util;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.ruege.mobile.model.NewsItem; 

import java.util.List;
import java.util.Objects;

public class NewsDiffCallback extends DiffUtil.Callback {

    private final List<NewsItem> oldList;
    private final List<NewsItem> newList;

    public NewsDiffCallback(List<NewsItem> oldList, List<NewsItem> newList) {
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
        
        
        
        NewsItem oldItem = oldList.get(oldItemPosition);
        NewsItem newItem = newList.get(newItemPosition);
        
        
        
        return Objects.equals(oldItem.getTitle(), newItem.getTitle()); 
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        
        NewsItem oldItem = oldList.get(oldItemPosition);
        NewsItem newItem = newList.get(newItemPosition);
        
        return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                && Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl());
                
                
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}