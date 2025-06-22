package com.ruege.mobile.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ruege.mobile.databinding.FragmentExercisesBinding;
import com.ruege.mobile.model.ContentItem;
import com.ruege.mobile.ui.exercises.ExerciseItem;
import com.ruege.mobile.ui.adapter.ExercisesAdapter;
import com.ruege.mobile.ui.viewmodel.TasksViewModel;
import com.ruege.mobile.utilss.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class ExercisesFragment extends Fragment {

    private FragmentExercisesBinding binding;
    private ExercisesAdapter adapter;
    private TasksViewModel tasksViewModel;
    private String egeNumberFilter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExercisesBinding.inflate(inflater, container, false);
        
        if (getArguments() != null) {
            egeNumberFilter = getArguments().getString("ege_number");
            Timber.d("Получен параметр фильтрации по номеру ЕГЭ: %s", egeNumberFilter);
        }
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (egeNumberFilter != null && !egeNumberFilter.isEmpty() && binding != null && binding.titleExercises != null) {
            binding.titleExercises.setText("Задания №" + egeNumberFilter);
        }
        
        tasksViewModel = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);
        
        RecyclerView recyclerView = binding.recyclerViewExercises;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        adapter = new ExercisesAdapter(new ArrayList<>(), getParentFragmentManager());
        recyclerView.setAdapter(adapter);
        
        setupObservers();
    }
    
    private void setupObservers() {
        View progressBar = binding.progressBar;
        RecyclerView recyclerView = binding.recyclerViewExercises;

        tasksViewModel.getTaskItemsState().observe(getViewLifecycleOwner(), resource -> {
            if (resource instanceof Resource.Loading) {
                progressBar.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                Timber.d("Загрузка списка заданий...");
            } else if (resource instanceof Resource.Success) {
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                List<ContentItem> taskGroups = resource.data;
                if (taskGroups != null && !taskGroups.isEmpty()) {
                    Timber.d("Получено %d групп заданий", taskGroups.size());
                    List<ExerciseItem> exerciseItems = convertTaskGroupsToExerciseItems(taskGroups);

                    if (egeNumberFilter != null && !egeNumberFilter.isEmpty()) {
                        exerciseItems = exerciseItems.stream()
                            .filter(item -> egeNumberFilter.equals(item.getTaskId()))
                            .collect(Collectors.toList());
                        Timber.d("Применен фильтр по номеру ЕГЭ %s: осталось %d элементов",
                            egeNumberFilter, exerciseItems.size());
                    }
                    adapter.updateExerciseItems(exerciseItems);
                } else {
                    Timber.d("Список групп заданий пуст");
                    adapter.updateExerciseItems(new ArrayList<>());
                }
            } else if (resource instanceof Resource.Error) {
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE); 
                String error = resource.message;
                if (error != null && !error.isEmpty()) {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                    Timber.e("Ошибка загрузки заданий: %s", error);
                }
            }
        });
    }

    private List<ExerciseItem> convertTaskGroupsToExerciseItems(List<ContentItem> taskGroups) {
        List<ExerciseItem> items = new ArrayList<>();
        
        for (ContentItem group : taskGroups) {
            String egeNumber = group.getContentId().replace("task_group_", "");
            
            ExerciseItem item = new ExerciseItem(
                group.getTitle(),        
                group.getDescription() != null ? group.getDescription() : "",
                "Средняя",               
                10,                      
                egeNumber,
                group.getContentId()
            );
            items.add(item);
        }
        
        Timber.d("Сконвертировано %d элементов ExerciseItem", items.size());
        return items;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 