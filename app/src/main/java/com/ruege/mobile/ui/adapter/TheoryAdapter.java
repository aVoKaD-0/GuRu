package com.ruege.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.ruege.mobile.R;
import com.ruege.mobile.databinding.ItemContentBinding;
import com.ruege.mobile.model.ContentItem;

public class TheoryAdapter extends ListAdapter<ContentItem, TheoryAdapter.TheoryViewHolder> {
    private final OnContentClickListener clickListener;
    private OnItemSelectionListener selectionListener;
    private OnItemDeleteListener deleteListener;

    public TheoryAdapter(OnContentClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    public void setOnItemSelectionListener(OnItemSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public TheoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemContentBinding binding = ItemContentBinding.inflate(inflater, parent, false);
        return new TheoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TheoryViewHolder holder, int position) {
        ContentItem item = getItem(position);
        if (item != null) {
            holder.bind(item, clickListener, selectionListener, deleteListener);
        }
    }

    public static class TheoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemContentBinding binding;

        public TheoryViewHolder(ItemContentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final ContentItem item, final OnContentClickListener clickListener, final OnItemSelectionListener selectionListener, final OnItemDeleteListener deleteListener) {
            binding.contentTitle.setText(item.getTitle());
            binding.contentDescription.setText(item.getDescription());

            binding.contentArrow.setVisibility(View.GONE);
            binding.contentDownloadContainer.setVisibility(View.VISIBLE);

            if (item.isDownloaded()) {
                binding.contentDownloadStatus.setVisibility(View.GONE);
                binding.contentDownloadedIcon.setVisibility(View.VISIBLE);
                binding.contentDownloadContainer.setClickable(true);
                binding.contentDownloadContainer.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onItemDelete(item);
                    }
                });
            } else {
                binding.contentDownloadStatus.setVisibility(View.VISIBLE);
                binding.contentDownloadedIcon.setVisibility(View.GONE);
                binding.contentDownloadContainer.setClickable(false);
                binding.contentDownloadContainer.setOnClickListener(null);
            }

            binding.contentDownloadStatus.setChecked(item.isSelected());
            binding.contentDownloadStatus.setOnCheckedChangeListener(null);
            binding.contentDownloadStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (selectionListener != null) {
                    selectionListener.onItemSelectionChanged(item, isChecked);
                }
            });
            
            itemView.setOnClickListener(v -> clickListener.onContentClick(item));

            binding.contentIcon.setImageResource(R.drawable.ic_theory);
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

    public interface OnItemSelectionListener {
        void onItemSelectionChanged(ContentItem item, boolean isSelected);
    }

    public interface OnItemDeleteListener {
        void onItemDelete(ContentItem item);
    }
} 