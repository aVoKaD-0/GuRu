package com.ruege.mobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.ruege.mobile.R;
import com.ruege.mobile.model.NavigationItem;
import com.ruege.mobile.util.NavigationDiffCallback;

import java.util.ArrayList;
import java.util.List;

public class NavigationAdapter extends RecyclerView.Adapter<NavigationAdapter.NavigationViewHolder> {
    
    private List<NavigationItem> navigationItems;
    private final OnNavigationItemClickListener listener;
    private int selectedPosition = 0;
    
    public interface OnNavigationItemClickListener {
        void onNavigationItemClick(NavigationItem navigationItem, int position);
    }
    
    public NavigationAdapter(List<NavigationItem> navigationItems, OnNavigationItemClickListener listener) {
        this.navigationItems = new ArrayList<>(navigationItems == null ? List.of() : navigationItems);
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public NavigationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_navigation, parent, false);
        return new NavigationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull NavigationViewHolder holder, int position) {
        NavigationItem item = navigationItems.get(position);
        holder.navTitle.setText(item.getTitle());
        holder.navIcon.setImageResource(item.getIconResId());

        holder.itemView.setSelected(position == selectedPosition);
        
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && previousSelected != currentPosition) {
                selectedPosition = currentPosition;
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition); 
                listener.onNavigationItemClick(item, selectedPosition);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return navigationItems == null ? 0 : navigationItems.size();
    }
    
    public void submitList(List<NavigationItem> newItems) {
        List<NavigationItem> newNavList = new ArrayList<>(newItems == null ? List.of() : newItems);
        NavigationDiffCallback diffCallback = new NavigationDiffCallback(this.navigationItems, newNavList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        
        this.navigationItems.clear();
        this.navigationItems.addAll(newNavList);
        diffResult.dispatchUpdatesTo(this); 
    }

    public void setSelectedPosition(int position) {
        if (position >= 0 && position < getItemCount() && position != selectedPosition) {
            int previousSelected = selectedPosition;
            selectedPosition = position;
            notifyItemChanged(previousSelected);
            notifyItemChanged(position);
        }
    }
    
    public static class NavigationViewHolder extends RecyclerView.ViewHolder {
        final ImageView navIcon;
        final TextView navTitle;
        
        public NavigationViewHolder(@NonNull View itemView) {
            super(itemView);
            navIcon = itemView.findViewById(R.id.nav_icon);
            navTitle = itemView.findViewById(R.id.nav_title);
        }
    }
} 