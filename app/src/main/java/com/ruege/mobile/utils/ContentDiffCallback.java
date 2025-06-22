package com.ruege.mobile.utils;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.ruege.mobile.model.ContentItem; 

import java.util.List;
import java.util.Objects;

public class ContentDiffCallback extends DiffUtil.Callback {

    private final List<ContentItem> oldList;
    private final List<ContentItem> newList;

    public ContentDiffCallback(List<ContentItem> oldList, List<ContentItem> newList) {
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
        
        
        ContentItem oldItem = oldList.get(oldItemPosition);
        ContentItem newItem = newList.get(newItemPosition);
        
        return Objects.equals(oldItem.getContentId(), newItem.getContentId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        
        ContentItem oldItem = oldList.get(oldItemPosition);
        ContentItem newItem = newList.get(newItemPosition);
        
        return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                && Objects.equals(oldItem.getDescription(), newItem.getDescription()) 
                && Objects.equals(oldItem.getType(), newItem.getType()) 
                && oldItem.isNew() == newItem.isNew(); 
                
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}