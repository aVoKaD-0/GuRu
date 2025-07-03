package com.ruege.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.ruege.mobile.R;
import com.ruege.mobile.databinding.ItemShpargalkiBinding;
import com.ruege.mobile.model.ContentItem;

public class ShpargalkaAdapter extends ListAdapter<ContentItem, ShpargalkaAdapter.ShpargalkaViewHolder> {
    private final OnContentClickListener clickListener;

    public ShpargalkaAdapter(OnContentClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ShpargalkaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemShpargalkiBinding binding = ItemShpargalkiBinding.inflate(inflater, parent, false);
        return new ShpargalkaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ShpargalkaViewHolder holder, int position) {
        ContentItem item = getItem(position);
        if (item != null) {
            holder.bind(item, clickListener);
        }
    }

    public static class ShpargalkaViewHolder extends RecyclerView.ViewHolder {
        private final ItemShpargalkiBinding binding;

        public ShpargalkaViewHolder(ItemShpargalkiBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final ContentItem item, final OnContentClickListener clickListener) {
            binding.contentTitle.setText(item.getTitle());
            binding.contentDescription.setText(item.getDescription());
            
            itemView.setOnClickListener(v -> clickListener.onContentClick(item));

            binding.contentIcon.setImageResource(R.drawable.ic_cheatsheets);
        }
    }

    private static final DiffUtil.ItemCallback<ContentItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<ContentItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ContentItem oldItem, @NonNull ContentItem newItem) {
            return oldItem.getContentId().equals(newItem.getContentId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ContentItem oldItem, @NonNull ContentItem newItem) {
            return oldItem.equals(newItem);
        }
    };

    public interface OnContentClickListener {
        void onContentClick(ContentItem item);
    }
} 