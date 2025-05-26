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
import com.ruege.mobile.model.NewsItem;
import com.ruege.mobile.util.NewsDiffCallback;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<NewsItem> newsItems;
    private final OnNewsClickListener onNewsClickListener;
    
    public interface OnNewsClickListener {
        void onNewsClick(NewsItem newsItem);
    }

    public NewsAdapter(List<NewsItem> newsItems, OnNewsClickListener listener) {
        this.newsItems = new ArrayList<>(newsItems == null ? List.of() : newsItems);
        this.onNewsClickListener = listener;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem item = newsItems.get(position);
        holder.titleView.setText(item.getTitle());
        
        Glide.with(holder.imageView.getContext())
             .load(item.getImageUrl())
             .placeholder(R.drawable.ic_launcher_background) 
             .error(R.drawable.ic_launcher_foreground) 
             .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> onNewsClickListener.onNewsClick(item));
    }

    @Override
    public int getItemCount() {
        return newsItems == null ? 0 : newsItems.size();
    }

    public void submitList(List<NewsItem> newItems) {
        List<NewsItem> newNewsList = new ArrayList<>(newItems == null ? List.of() : newItems);
        NewsDiffCallback diffCallback = new NewsDiffCallback(this.newsItems, newNewsList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        
        this.newsItems.clear();
        this.newsItems.addAll(newNewsList);
        diffResult.dispatchUpdatesTo(this);
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleView;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.news_image);
            titleView = itemView.findViewById(R.id.news_title);
        }
    }
} 