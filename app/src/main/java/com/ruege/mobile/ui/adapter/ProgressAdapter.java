package com.ruege.mobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.TypedValue;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.ruege.mobile.R;
import com.ruege.mobile.databinding.ItemProgressBinding;
import com.ruege.mobile.model.ProgressItem;
import com.ruege.mobile.utils.ProgressDiffCallback;
import com.ruege.mobile.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

public class ProgressAdapter extends RecyclerView.Adapter<ProgressAdapter.ProgressViewHolder> {

    private List<ProgressItem> progressItems;
    private final OnProgressClickListener onProgressClickListener;
    
    public interface OnProgressClickListener {
        void onProgressClick(ProgressItem progressItem);
    }

    public ProgressAdapter(List<ProgressItem> progressItems, OnProgressClickListener listener) {
        this.progressItems = new ArrayList<>(progressItems == null ? List.of() : progressItems);
        this.onProgressClickListener = listener;
    }

    @NonNull
    @Override
    public ProgressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProgressBinding binding = ItemProgressBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ProgressViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressViewHolder holder, int position) {
        ProgressItem item = progressItems.get(position);
        holder.bind(item, onProgressClickListener);
    }

    @Override
    public int getItemCount() {
        return progressItems.size();
    }
    
    public void submitList(List<ProgressItem> newItems) {
        List<ProgressItem> newProgressList = new ArrayList<>(newItems == null ? List.of() : newItems);
        ProgressDiffCallback diffCallback = new ProgressDiffCallback(this.progressItems, newProgressList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        
        this.progressItems.clear();
        this.progressItems.addAll(newProgressList);
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * Обновляет список элементов прогресса
     * @param newItems новый список элементов
     */
    public void updateItems(List<ProgressItem> newItems) {
        List<ProgressItem> safeNewItems = newItems != null ? newItems : new ArrayList<>();
        
        Timber.tag("ProgressAdapter").d("Обновление элементов, новых элементов: " + safeNewItems.size());
        
        for (int i = 0; i < safeNewItems.size(); i++) {
            ProgressItem item = safeNewItems.get(i);
            Timber.tag("ProgressAdapter").d("Новый элемент " + i + 
                              ": id=" + item.getId() + 
                              ", title=" + item.getTitle() + 
                              ", type=" + item.getType() + 
                              ", percentage=" + item.getPercentage());
        }
        
        ProgressDiffCallback diffCallback = new ProgressDiffCallback(this.progressItems, safeNewItems);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        
        this.progressItems.clear();
        this.progressItems.addAll(safeNewItems);
        diffResult.dispatchUpdatesTo(this);
    }

    static class ProgressViewHolder extends RecyclerView.ViewHolder {
        private final ItemProgressBinding binding;

        public ProgressViewHolder(ItemProgressBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ProgressItem item, OnProgressClickListener listener) {
            Context context = itemView.getContext();
            binding.progressTitle.setText(item.getTitle());

            Timber.tag("ProgressAdapter").d("Позиция " + getAdapterPosition() +
                    ": id=" + item.getId() +
                    ", title=" + item.getTitle() +
                    ", type=" + item.getType() +
                    ", percentage=" + item.getPercentage());

            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                binding.progressDescription.setText(item.getDescription());
                binding.progressDescription.setVisibility(View.VISIBLE);
            } else {
                binding.progressDescription.setVisibility(View.GONE);
            }

            binding.progressBar.setProgress(item.getPercentage());
            binding.progressPercent.setText(item.getPercentage() + "%");

            if (binding.progressStatus != null) {
                if (item.getPercentage() >= 100) {
                    binding.progressStatus.setVisibility(View.VISIBLE);
                    binding.progressStatus.setImageResource(R.drawable.ic_done);
                    binding.progressStatus.setColorFilter(
                            ContextCompat.getColor(context, R.color.success));
                } else if (item.getPercentage() > 0) {
                    binding.progressStatus.setVisibility(View.VISIBLE);
                    binding.progressStatus.setImageResource(R.drawable.ic_in_progress);
                    binding.progressStatus.setColorFilter(
                            ContextCompat.getColor(context, R.color.warning));
                } else {
                    binding.progressStatus.setVisibility(View.GONE);
                }
            }

            String itemType = item.getType();
            ViewGroup.LayoutParams params = itemView.getLayoutParams();
            boolean isDarkMode = UiUtils.isDarkMode(context);

            if ("PROGRESS".equals(itemType)) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                itemView.setLayoutParams(params);

                if (isDarkMode) {
                    itemView.setBackgroundResource(R.drawable.progress_card_background_primary_dark);
                    binding.progressTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary_dark));
                    binding.progressDescription.setTextColor(ContextCompat.getColor(context, R.color.text_secondary_dark));
                } else {
                    itemView.setBackgroundResource(R.drawable.progress_card_background_primary);
                }

                binding.progressTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                binding.progressPercent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

                if (binding.progressStatus != null) {
                    binding.progressStatus.setVisibility(View.GONE);
                }

                ViewGroup.LayoutParams progressParams = binding.progressBar.getLayoutParams();
                progressParams.width = UiUtils.dpToPx(context, 150);
                binding.progressBar.setLayoutParams(progressParams);
                binding.progressBar.setVisibility(View.VISIBLE);

                Timber.tag("ProgressAdapter").d("Элемент " + item.getId() + " отображается как PROGRESS карточка");
            } else {
                params.width = UiUtils.dpToPx(context, "TASK".equals(itemType) ? 92 : 90);
                itemView.setLayoutParams(params);

                if (isDarkMode) {
                    itemView.setBackgroundResource(R.drawable.progress_card_background_dark);
                    binding.progressTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary_dark));
                    binding.progressDescription.setTextColor(ContextCompat.getColor(context, R.color.text_secondary_dark));
                } else {
                    itemView.setBackgroundResource(R.drawable.progress_card_background);
                }

                ViewGroup.LayoutParams progressParams = binding.progressBar.getLayoutParams();
                progressParams.width = UiUtils.dpToPx(context, 75);
                binding.progressBar.setLayoutParams(progressParams);

                binding.progressTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                binding.progressPercent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                
                if ("TASK".equals(itemType)) {
                    Timber.tag("ProgressAdapter").d("Элемент " + item.getId() + " отображается как TASK карточка");
                } else {
                    Timber.tag("ProgressAdapter").w("Предупреждение: неизвестный тип элемента: " + itemType);
                }
            }

            itemView.setOnClickListener(v -> listener.onProgressClick(item));
        }
    }
} 