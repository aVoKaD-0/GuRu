package com.ruege.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.ruege.mobile.R;
import com.ruege.mobile.model.ContentItem;
import java.util.List;
import java.util.Objects;

public class ContentAdapter extends ListAdapter<ContentItem, ContentAdapter.ViewHolder> {
    private final OnContentClickListener clickListener;
    private OnItemSelectionListener selectionListener;
    private OnItemDeleteListener deleteListener;

    private static final int VIEW_TYPE_DEFAULT = 0;
    private static final int VIEW_TYPE_SHPARGALKA = 1;

    public ContentAdapter(OnContentClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    public void setOnItemSelectionListener(OnItemSelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.deleteListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        ContentItem item = getItem(position);
        if (item != null && "shpargalka".equals(item.getType())) {
            return VIEW_TYPE_SHPARGALKA;
        }
        return VIEW_TYPE_DEFAULT;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SHPARGALKA) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shpargalki, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_content, parent, false);
        }
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContentItem item = getItem(position);
        if (item != null) {
            holder.bind(item, clickListener, selectionListener, deleteListener);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView description;
        private final ImageView icon;
        private final ImageView arrow;
        private final CheckBox downloadStatus;
        private final ImageView downloadedIcon;
        private final FrameLayout downloadContainer;
        private final int viewType;

        public ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            title = itemView.findViewById(R.id.content_title);
            description = itemView.findViewById(R.id.content_description);
            icon = itemView.findViewById(R.id.content_icon);
            arrow = itemView.findViewById(R.id.content_arrow);

            if (viewType == VIEW_TYPE_DEFAULT) {
                downloadStatus = itemView.findViewById(R.id.content_download_status);
                downloadedIcon = itemView.findViewById(R.id.content_downloaded_icon);
                downloadContainer = itemView.findViewById(R.id.content_download_container);
            } else { 
                downloadStatus = null;
                downloadedIcon = null;
                downloadContainer = null;
            }
        }

        public void bind(final ContentItem item, final OnContentClickListener clickListener, final OnItemSelectionListener selectionListener, final OnItemDeleteListener deleteListener) {
            title.setText(item.getTitle());
            description.setText(item.getDescription());

            if (viewType == VIEW_TYPE_SHPARGALKA) {
                itemView.setOnClickListener(v -> clickListener.onContentClick(item));
            } else { 
                if (arrow != null) arrow.setVisibility(View.GONE);
                if (downloadContainer != null) downloadContainer.setVisibility(View.VISIBLE);

                if (item.isDownloaded()) {
                    if (downloadStatus != null) downloadStatus.setVisibility(View.GONE);
                    if (downloadedIcon != null) downloadedIcon.setVisibility(View.VISIBLE);
                    if (downloadContainer != null) {
                        downloadContainer.setClickable(true);
                        downloadContainer.setOnClickListener(v -> {
                            if (deleteListener != null) {
                                deleteListener.onItemDelete(item);
                            }
                        });
                    }
                } else {
                    if (downloadStatus != null) downloadStatus.setVisibility(View.VISIBLE);
                    if (downloadedIcon != null) downloadedIcon.setVisibility(View.GONE);
                    if (downloadContainer != null) {
                        downloadContainer.setClickable(false);
                        downloadContainer.setOnClickListener(null);
                    }
                }

                if (downloadStatus != null) {
                    downloadStatus.setChecked(item.isSelected());
                    downloadStatus.setOnCheckedChangeListener(null);
                    downloadStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (selectionListener != null) {
                            selectionListener.onItemSelectionChanged(item, isChecked);
                        }
                    });
                }
                
                itemView.setOnClickListener(v -> clickListener.onContentClick(item));
            }

            int iconRes = R.drawable.ic_exercises; 
            if (item.getType() != null) {
                switch (item.getType()) {
                    case "theory":
                        iconRes = R.drawable.ic_theory;
                        break;
                    case "task_group":
                        iconRes = R.drawable.ic_tasks;
                        break;
                    case "shpargalka":
                        iconRes = R.drawable.ic_cheatsheets;
                        break;
                }
            }
            icon.setImageResource(iconRes);
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