package com.ruege.mobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ruege.mobile.R;
import com.ruege.mobile.model.ContentItem;
import com.ruege.mobile.util.ContentDiffCallback;

import java.util.ArrayList;
import java.util.List;

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ContentViewHolder> {

    private List<ContentItem> contentItems;
    private final OnContentClickListener onContentClickListener;
    
    public interface OnContentClickListener {
        void onContentClick(ContentItem contentItem);
    }

    public ContentAdapter(List<ContentItem> contentItems, OnContentClickListener listener) {
        this.contentItems = new ArrayList<>(contentItems == null ? List.of() : contentItems);
        this.onContentClickListener = listener;
    }

    @NonNull
    @Override
    public ContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_content, parent, false);
        return new ContentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContentViewHolder holder, int position) {
        ContentItem item = contentItems.get(position);
        holder.titleView.setText(item.getTitle());
        if (holder.descriptionView != null) {
            holder.descriptionView.setText(item.getDescription());
        }

        holder.itemView.setOnClickListener(v -> onContentClickListener.onContentClick(item));
    }

    @Override
    public int getItemCount() {
        return contentItems == null ? 0 : contentItems.size();
    }

    public void submitList(List<ContentItem> newItems) {
        List<ContentItem> newContentList = new ArrayList<>(newItems == null ? List.of() : newItems);
        ContentDiffCallback diffCallback = new ContentDiffCallback(this.contentItems, newContentList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        
        this.contentItems.clear();
        this.contentItems.addAll(newContentList);
        diffResult.dispatchUpdatesTo(this);
    }

    static class ContentViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView descriptionView;

        public ContentViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.content_title);
            descriptionView = itemView.findViewById(R.id.content_description);
        }
    }
} 