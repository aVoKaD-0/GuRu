package com.ruege.mobile.ui.exercises;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ruege.mobile.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import timber.log.Timber;

public class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.ExerciseViewHolder> {

    private List<ExerciseItem> exerciseItems;
    private FragmentManager fragmentManager;

    public ExercisesAdapter(List<ExerciseItem> exerciseItems, FragmentManager fragmentManager) {
        this.exerciseItems = exerciseItems;
        this.fragmentManager = fragmentManager;
    }
    
    /**
     * Обновляет список заданий и перерисовывает адаптер
     */
    public void updateExerciseItems(List<ExerciseItem> newItems) {
        this.exerciseItems = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        ExerciseItem item = exerciseItems.get(position);
        Timber.d("Привязка элемента: %s, %s", item.getTitle(), item.getDescription());
        holder.titleTextView.setText(item.getTitle());
        holder.descriptionTextView.setText(item.getDescription());
        holder.difficultyTextView.setText("Сложность: " + item.getDifficulty());

        holder.buttonStartExercise.setOnClickListener(v -> {
            String taskId = item.getTaskId();
            
            TaskFragment taskFragment = new TaskFragment();
            Bundle args = new Bundle();
            
            if (taskId != null && !taskId.isEmpty()) {
                Timber.d("Открываем задание с ID: %s", taskId);
                args.putString("task_id", taskId);
            } else {
                String groupId = item.getContentId();
                Timber.d("ID задания не найден, используем ID группы: %s", groupId);
                Toast.makeText(v.getContext(), "Загружаем группу заданий: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                args.putString("task_id", groupId);
            }
            
            taskFragment.setArguments(args);
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, taskFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        Timber.d("Количество элементов в адаптере: %d", exerciseItems.size());
        return exerciseItems.size();
    }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView difficultyTextView;
        MaterialButton buttonStartExercise;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.exerciseTitle);
            descriptionTextView = itemView.findViewById(R.id.exerciseDescription);
            difficultyTextView = itemView.findViewById(R.id.exerciseDifficulty);
            buttonStartExercise = itemView.findViewById(R.id.btnStartExercise);
        }
    }
} 