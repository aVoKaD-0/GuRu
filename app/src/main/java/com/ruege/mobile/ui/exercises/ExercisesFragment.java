package com.ruege.mobile.ui.exercises;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ruege.mobile.R;
import com.ruege.mobile.data.local.entity.ContentEntity;
import com.ruege.mobile.databinding.FragmentExercisesBinding;
import com.ruege.mobile.model.TaskItem;
import com.ruege.mobile.ui.viewmodel.ContentViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class ExercisesFragment extends Fragment {

    private FragmentExercisesBinding binding;
    private ExercisesAdapter adapter;
    private ContentViewModel contentViewModel;
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
        
        contentViewModel = new ViewModelProvider(requireActivity()).get(ContentViewModel.class);
        
        RecyclerView recyclerView = binding.recyclerViewExercises;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        adapter = new ExercisesAdapter(new ArrayList<>(), getParentFragmentManager());
        recyclerView.setAdapter(adapter);
        
        View progressBar = binding.progressBar;
        
        List<ContentEntity> existingData = contentViewModel.getTasksTopicsLiveData().getValue();
        if (existingData != null && !existingData.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            
            List<ExerciseItem> exerciseItems = convertTaskGroupsToExerciseItems(existingData);
            
            if (egeNumberFilter != null && !egeNumberFilter.isEmpty()) {
                exerciseItems = exerciseItems.stream()
                    .filter(item -> egeNumberFilter.equals(item.getTaskId()))
                    .collect(Collectors.toList());
                    
                Timber.d("Применен фильтр по номеру ЕГЭ %s: осталось %d элементов", 
                    egeNumberFilter, exerciseItems.size());
            }
            
            adapter.updateExerciseItems(exerciseItems);
            Timber.d("Загруженные данные отображены из текущего состояния ViewModel: %d элементов", exerciseItems.size());
        } else {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            Timber.d("Данные ещё не загружены, отображаем индикатор загрузки");
        }
        
        contentViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Timber.d("Состояние загрузки: %s", isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });
        
        contentViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                Timber.e("Ошибка загрузки заданий: %s", error);
            }
        });
        
        contentViewModel.getTasksTopicsLiveData().observe(getViewLifecycleOwner(), taskGroups -> {
            if (taskGroups != null && !taskGroups.isEmpty()) {
                Timber.d("Получено %d групп заданий", taskGroups.size());
                
                for (ContentEntity group : taskGroups) {
                    Timber.d("Группа заданий: id=%s, title=%s, type=%s", 
                        group.getContentId(), group.getTitle(), group.getType());
                }
                
                List<ExerciseItem> exerciseItems = convertTaskGroupsToExerciseItems(taskGroups);
                
                if (egeNumberFilter != null && !egeNumberFilter.isEmpty()) {
                    exerciseItems = exerciseItems.stream()
                        .filter(item -> egeNumberFilter.equals(item.getTaskId()))
                        .collect(Collectors.toList());
                        
                    Timber.d("Применен фильтр по номеру ЕГЭ %s: осталось %d элементов", 
                        egeNumberFilter, exerciseItems.size());
                }
                
                if (exerciseItems.isEmpty()) {
                    Timber.w("Список групп заданий не пуст, но после конвертации/фильтрации получен пустой список ExerciseItem");
                    
                    if (egeNumberFilter != null && !egeNumberFilter.isEmpty()) {
                        Toast.makeText(requireContext(), 
                            "Для задания №" + egeNumberFilter + " нет доступных заданий", 
                            Toast.LENGTH_LONG).show();
                    }
                } else {
                    Timber.d("Сконвертировано %d элементов ExerciseItem", exerciseItems.size());
                }
                
                adapter.updateExerciseItems(exerciseItems);
            } else {
                Timber.d("Список групп заданий пуст");
            }
        });
        
    }
    
    private List<ExerciseItem> convertTaskGroupsToExerciseItems(List<com.ruege.mobile.data.local.entity.ContentEntity> taskGroups) {
        List<ExerciseItem> items = new ArrayList<>();
        
        int totalGroups = 0;
        int filteredGroups = 0;
        
        for (com.ruege.mobile.data.local.entity.ContentEntity group : taskGroups) {
            totalGroups++;
            String type = group.getType();
            String contentId = group.getContentId();
            Timber.d("Обрабатываем элемент: id=%s, title=%s, type=%s", 
                contentId, group.getTitle(), type);
                
            String description = group.getDescription() != null ? group.getDescription() : "";
            
            String egeNumber = contentId.replace("task_group_", "");
            
            ExerciseItem item = new ExerciseItem(
                group.getTitle(),        
                description,             
                "Средняя",               
                10,                      
                egeNumber,
                contentId
            );
            
            items.add(item);
            filteredGroups++;
        }
        
        Timber.d("Всего элементов: %d, отфильтровано групп заданий: %d", totalGroups, filteredGroups);
        return items;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 