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
    private String egeNumberFilter; // Фильтр по номеру ЕГЭ

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExercisesBinding.inflate(inflater, container, false);
        
        // Получаем параметр из аргументов фрагмента
        if (getArguments() != null) {
            egeNumberFilter = getArguments().getString("ege_number");
            Timber.d("Получен параметр фильтрации по номеру ЕГЭ: %s", egeNumberFilter);
        }
        
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Если задан фильтр, показываем заголовок с указанием номера задания
        if (egeNumberFilter != null && !egeNumberFilter.isEmpty() && binding != null && binding.titleExercises != null) {
            binding.titleExercises.setText("Задания №" + egeNumberFilter);
        }
        
        // Получаем ViewModel из активности, а не создаем новый
        contentViewModel = new ViewModelProvider(requireActivity()).get(ContentViewModel.class);
        
        RecyclerView recyclerView = binding.recyclerViewExercises;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Создаем адаптер с пустым списком, который будет заполнен позже
        adapter = new ExercisesAdapter(new ArrayList<>(), getParentFragmentManager());
        recyclerView.setAdapter(adapter);
        
        // Настраиваем отображение загрузки
        View progressBar = binding.progressBar;
        
        // Проверяем, есть ли уже загруженные данные
        List<ContentEntity> existingData = contentViewModel.getTasksTopicsLiveData().getValue();
        if (existingData != null && !existingData.isEmpty()) {
            // Данные уже загружены, сразу обновляем UI
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            
            // Обновляем адаптер с существующими данными
            List<ExerciseItem> exerciseItems = convertTaskGroupsToExerciseItems(existingData);
            
            // Применяем фильтр по номеру ЕГЭ, если он задан
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
            // Данных ещё нет, показываем индикатор загрузки
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            Timber.d("Данные ещё не загружены, отображаем индикатор загрузки");
        }
        
        // Наблюдаем за состоянием загрузки
        contentViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Timber.d("Состояние загрузки: %s", isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });
        
        // Наблюдаем за ошибками
        contentViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                Timber.e("Ошибка загрузки заданий: %s", error);
            }
        });
        
        // Наблюдаем за списком заданий из локальной БД (ContentEntity)
        contentViewModel.getTasksTopicsLiveData().observe(getViewLifecycleOwner(), taskGroups -> {
            if (taskGroups != null && !taskGroups.isEmpty()) {
                Timber.d("Получено %d групп заданий", taskGroups.size());
                
                // Добавляем дополнительный лог для отладки
                for (ContentEntity group : taskGroups) {
                    Timber.d("Группа заданий: id=%s, title=%s, type=%s", 
                        group.getContentId(), group.getTitle(), group.getType());
                }
                
                List<ExerciseItem> exerciseItems = convertTaskGroupsToExerciseItems(taskGroups);
                
                // Применяем фильтр по номеру ЕГЭ, если он задан
                if (egeNumberFilter != null && !egeNumberFilter.isEmpty()) {
                    exerciseItems = exerciseItems.stream()
                        .filter(item -> egeNumberFilter.equals(item.getTaskId()))
                        .collect(Collectors.toList());
                        
                    Timber.d("Применен фильтр по номеру ЕГЭ %s: осталось %d элементов", 
                        egeNumberFilter, exerciseItems.size());
                }
                
                // Проверяем, что список не пустой
                if (exerciseItems.isEmpty()) {
                    Timber.w("Список групп заданий не пуст, но после конвертации/фильтрации получен пустой список ExerciseItem");
                    
                    // Показываем сообщение пользователю, если после фильтрации нет заданий
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
        
        // Данные заданий загружаются при запуске приложения
    }
    
    private List<ExerciseItem> convertTaskGroupsToExerciseItems(List<com.ruege.mobile.data.local.entity.ContentEntity> taskGroups) {
        List<ExerciseItem> items = new ArrayList<>();
        
        // Добавляем счетчик для отладки
        int totalGroups = 0;
        int filteredGroups = 0;
        
        for (com.ruege.mobile.data.local.entity.ContentEntity group : taskGroups) {
            totalGroups++;
            // Элементы уже отфильтрованы по типу task_group в ContentRepository
            String type = group.getType();
            String contentId = group.getContentId();
            Timber.d("Обрабатываем элемент: id=%s, title=%s, type=%s", 
                contentId, group.getTitle(), type);
                
            // В поле description хранится количество заданий: "30 заданий"
            String description = group.getDescription() != null ? group.getDescription() : "";
            
            // Получаем номер задания ЕГЭ из contentId (например, из task_group_1 получаем 1)
            String egeNumber = contentId.replace("task_group_", "");
            
            // Создаем элемент с указанием и taskId, и contentId
            ExerciseItem item = new ExerciseItem(
                group.getTitle(),        
                description,             
                "Средняя",               
                10,                      
                egeNumber,               // taskId - номер задания ЕГЭ
                contentId                // contentId - полный идентификатор группы заданий
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