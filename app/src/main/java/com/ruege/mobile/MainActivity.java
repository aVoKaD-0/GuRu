package com.ruege.mobile;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.res.Resources;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.view.Gravity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import androidx.webkit.WebSettingsCompat;
import android.util.TypedValue;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.ruege.mobile.adapter.ContentAdapter;
import com.ruege.mobile.adapter.NewsAdapter;
import com.ruege.mobile.adapter.ProgressAdapter;
import com.ruege.mobile.auth.GoogleAuthManager;
import com.ruege.mobile.data.local.AppDatabase;
import com.ruege.mobile.data.local.TokenManager;
import com.ruege.mobile.data.local.entity.CategoryEntity;
import com.ruege.mobile.data.local.entity.ContentEntity;
import com.ruege.mobile.data.local.entity.NewsEntity;
import com.ruege.mobile.data.local.entity.ProgressEntity;
import com.ruege.mobile.data.local.entity.UserEntity;
import com.ruege.mobile.data.repository.ContentRepository;
import com.ruege.mobile.databinding.ActivityMainBinding;
import com.ruege.mobile.model.ContentItem;
import com.ruege.mobile.model.NewsItem;
import com.ruege.mobile.model.ProgressItem;
import com.ruege.mobile.model.TaskItem;
import com.ruege.mobile.ui.viewmodel.ContentViewModel;
import com.ruege.mobile.ui.viewmodel.NewsViewModel;
import com.ruege.mobile.ui.viewmodel.ProgressViewModel;
import com.ruege.mobile.ui.sync.SyncStatusManager;
import com.ruege.mobile.ui.login.LoginActivity;
import com.ruege.mobile.ui.viewmodel.UserViewModel;
import com.ruege.mobile.util.SlowItemAnimator;
import com.ruege.mobile.viewmodel.ShpargalkaViewModel;
import com.ruege.mobile.viewmodel.VariantViewModel;
import com.ruege.mobile.adapter.VariantAdapter; 
import com.ruege.mobile.data.local.entity.VariantEntity; 
import com.ruege.mobile.utils.Resource; 
import com.github.barteksc.pdfviewer.PDFView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import android.media.MediaScannerConnection;


import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import com.ruege.mobile.ui.bottomsheet.VariantDetailBottomSheetDialogFragment;
import com.ruege.mobile.ui.bottomsheet.ProfileBottomSheetDialogFragment;
import com.ruege.mobile.ui.bottomsheet.TheoryBottomSheetDialogFragment;
import com.ruege.mobile.ui.bottomsheet.TaskDisplayBottomSheetDialogFragment;
import com.ruege.mobile.ui.bottomsheet.EssayBottomSheetDialogFragment;

import androidx.lifecycle.LifecycleOwnerKt; 
import com.ruege.mobile.util.FlowJavaHelper;
import kotlinx.coroutines.BuildersKt;      
import kotlinx.coroutines.GlobalScope;     
import kotlin.Unit;                       
import kotlinx.coroutines.Dispatchers;    

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private GoogleAuthManager googleAuthManager;
    
    @Inject
    TokenManager tokenManager;
    
    private NewsViewModel newsViewModel;
    private ProgressViewModel progressViewModel;
    private ContentViewModel contentViewModel;
    private UserViewModel userViewModel;
    private VariantViewModel variantViewModel; 
    
    private BottomSheetBehavior<ConstraintLayout> contentDetailsSheetBehavior;
    
    private ProgressAdapter progressAdapter;
    private NewsAdapter newsAdapter;
    private ContentAdapter contentAdapter;
    private VariantAdapter variantAdapter;
    

    private final List<ContentItem> contentItems = new ArrayList<>();
    private final Map<String, List<VariantEntity>> variantDataCache = new HashMap<>();
    

    private final String[] categories = {"theory", "task", "shpargalka", "variant"};
    private final String[] categoryTitles = {"Теория", "Задания", "Шпаргалки", "Варианты"};


    private RecyclerView contentRecycler;
    private RecyclerView progressRecycler;
    private RecyclerView newsRecycler;
    private RecyclerView variantRecycler; 


    private ShimmerFrameLayout shimmerProgressLayout;
    private ShimmerFrameLayout shimmerNewsLayout;
    private ShimmerFrameLayout shimmerContentLayout;
    private ShimmerFrameLayout shimmerVariantLayout; 

    private View errorPlaceholderProgress;
    private View errorPlaceholderNews;
    private View errorPlaceholderContent;
    private View errorPlaceholderVariant; 
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int LOAD_DELAY_MS = 1500; 
    
    private final Map<String, List<ContentItem>> categoryDataCache = new HashMap<>();

    private final NewsAdapter.OnNewsClickListener onNewsClickListener = newsItem -> {
        Log.d(TAG, "Clicked news: " + newsItem.getTitle());
        showNewsBottomSheet(
            newsItem.getTitle(),
            newsItem.getDescription(),
            "Дата публикации: " + newsItem.getDate()
        );
    };

    private final ProgressAdapter.OnProgressClickListener onProgressClickListener = progressItem -> {
        Log.d(TAG, "Clicked progress: " + progressItem.getTitle());
        Toast.makeText(this, "Прогресс: " + progressItem.getTitle() + " - " + progressItem.getPercentage() + "%", 
                Toast.LENGTH_SHORT).show();
    };
    
    private final ContentAdapter.OnContentClickListener onContentClickListener = contentItem -> {
        Log.d(TAG, "Клик по контенту: " + contentItem.getTitle() + ", тип: " + contentItem.getType());
        
        String contentId = contentItem.getContentId();
        String title = contentItem.getTitle();
        String description = contentItem.getDescription();
        String type = contentItem.getType();

        if ("task_group".equals(type)) {
            type = "task"; 
            Log.d(TAG, "Тип изменен с task_group на task для " + title);
        }
        
        showBottomSheet(title, description, contentId, type);
    };
    private SyncStatusManager syncStatusManager;

    @Inject
    com.ruege.mobile.data.repository.AuthRepository authRepository;
    
    @Inject
    com.ruege.mobile.utils.UserDataCleaner userDataCleaner;

    @Inject
    com.ruege.mobile.utils.LogoutHandler logoutHandler;

    @Inject
    com.ruege.mobile.data.repository.ProgressSyncRepository progressSyncRepository;
    
    @Inject
    com.ruege.mobile.data.repository.ProgressRepository progressRepository;

    @Inject // Добавляем инъекцию ContentRepository
    ContentRepository contentRepository;

    private Observer<List<TaskItem>> tasksObserver;
    private Observer<String> errorMessageObserver;
    
    private int currentActiveTaskIndex = 0;

    private File pendingSourceFile;
    private String pendingFileName;
    private String pendingDescription;
    private static final int REQUEST_WRITE_STORAGE = 1001;
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Разрешение на запись получено");
                
                if (pendingSourceFile != null && pendingFileName != null) {
                    Log.d(TAG, "Продолжаем копирование отложенного файла");
                    copyPdfToDownloads(pendingSourceFile, pendingFileName, pendingDescription);
                    
                    pendingSourceFile = null;
                    pendingFileName = null;
                    pendingDescription = null;
                }
            } else {
                Log.d(TAG, "Разрешение на запись не получено");
                Toast.makeText(this, "Для сохранения файла необходимо разрешение на запись", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            Log.d(TAG, "MainActivity onCreate");
            


            googleAuthManager = new GoogleAuthManager(this);

            setupThemeToggle();
            
            setupUI();
            
            newsViewModel = new ViewModelProvider(this).get(NewsViewModel.class);
            progressViewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
            contentViewModel = new ViewModelProvider(this).get(ContentViewModel.class);
            userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
            variantViewModel = new ViewModelProvider(this).get(VariantViewModel.class); // <--- Инициализация VariantViewModel
            
            // УБИРАЕМ ИНИЦИАЛИЗАЦИЮ ПРОГРЕССА ОТСЮДА
            // if (progressRepository != null) {
            //     progressRepository.initialize(); 
            //     Log.d(TAG, "ProgressRepository.initialize() вызван в MainActivity");
            // }
            

            setupAdaptersAndObservers(); // Вызов должен остаться здесь, но loadInitialProgress будет ниже
            
            initializePanels();
            
            contentViewModel.loadInitialContent(); // Запускаем загрузку основного контента

            // Наблюдаем за состоянием загрузки контента
            if (contentRepository != null) {
                FlowJavaHelper.collectInScope(
                    contentRepository.getInitialContentLoaded(),
                    LifecycleOwnerKt.getLifecycleScope(this),
                    contentLoaded -> { // Лямбда для java.util.function.Consumer<Boolean>
                        if (Boolean.TRUE.equals(contentLoaded)) {
                            Log.d(TAG, "Весь основной контент загружен. Инициализируем прогресс через ProgressViewModel.");
                            if (progressViewModel != null) {
                                progressViewModel.checkAndInitializeProgress();
                            }
                        }
                        // Больше не нужен explicit return Unit.INSTANCE;
                    }
                );
            }
            
            // Этот блок был перенесен внутрь коллбэка contentLoaded, но затем логика изменилась
            // для вызова checkAndInitializeProgress, который управляет isFirstTimeUser и loadInitialProgress.
            // Убедимся, что loadInitialProgress вызывается корректно после checkAndInitializeProgress.
            // В ProgressViewModel:
            // checkAndInitializeProgress() -> обновляет _isFirstTimeUser
            // В MainActivity (setupAdaptersAndObservers):
            // progressViewModel.getIsFirstTimeUser().observe(...) -> вызывает loadInitialProgress()

            // Таким образом, этот блок больше не нужен здесь в таком виде:
            /*
            handler.postDelayed(() -> {
                try {
                    newsViewModel.loadLatestNews(7);
                    Log.d(TAG, "Запрос последних новостей выполнен после задержки");
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при загрузке новостей после задержки", e);
                }
            }, 2000); 
            */
            // Оставим загрузку новостей как есть, она не зависит от загрузки контента и прогресса.
             handler.postDelayed(() -> {
                try {
                    newsViewModel.loadLatestNews(7);
                    Log.d(TAG, "Запрос последних новостей выполнен после задержки");
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при загрузке новостей после задержки", e);
                }
            }, 2000); // Задержка в 2 секунды


            Log.d(TAG, "MainActivity setup complete");

            // Настройка обработчика нажатия на аватар пользователя
            ImageView userAvatar = binding.userAvatar;
            if (userAvatar != null) {
                userAvatar.setOnClickListener(v -> showProfilePanel());
            }
            
            // Загружаем данные пользователя
            userViewModel.getFirstUser();
            
            // Наблюдатель для данных пользователя
            userViewModel.getCurrentUser().observe(this, user -> {
                // Обновляем аватар пользователя в главном окне
                updateUserAvatar(user);
            });

            // Запуск загрузки вариантов при создании MainActivity
            Log.d(TAG, "onCreate: Запускаем начальную загрузку вариантов");
            variantViewModel.fetchVariants(); 
        } catch (Exception e) {
            // Ловим исключение миграции базы данных
            Log.e(TAG, "Ошибка при запуске приложения", e);
            
            if (e.getMessage() != null && e.getMessage().contains("duplicate column name: google_id")) {
                // Показываем диалог с предложением сбросить базу данных
                showDatabaseResetDialog();
            } else {
                // Показываем обычное сообщение об ошибке
                Toast.makeText(this, "Произошла ошибка при запуске приложения", Toast.LENGTH_LONG).show();
            }
        }
        
        // Инициализируем прогресс пользователя при запуске
        Log.d(TAG, "Инициализация прогресса пользователя при запуске...");
        progressViewModel.initializeProgress();
        
        // Принудительно запускаем синхронизацию с сервером
        progressViewModel.refreshProgress();
        
        // Инициализируем панели
        Log.d(TAG, "Инициализация панелей");
    }
    
    /**
     * Показывает диалог для сброса базы данных
     */
    private void showDatabaseResetDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Ошибка базы данных")
            .setMessage("Обнаружена проблема с базой данных. Для решения проблемы необходимо сбросить данные приложения. Продолжить?")
            .setPositiveButton("Сбросить", (dialog, which) -> {
                // Сбрасываем базу данных
                resetDatabase();
            })
            .setNegativeButton("Выход", (dialog, which) -> {
                // Закрываем приложение
                finish();
            })
            .setCancelable(false)
            .show();
    }
    
    /**
     * Сбрасывает базу данных при ошибках миграции
     */
    private void resetDatabase() {
        try {
            Log.w("MainActivity", "Resetting database due to migration error");
            AppDatabase.clearAndRebuildDatabase(this);
            Toast.makeText(this, "База данных сброшена. Пожалуйста, войдите снова.", Toast.LENGTH_LONG).show();
            
            // Перезапускаем приложение
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        } catch (Exception e) {
            Log.e("MainActivity", "Error while resetting database", e);
            // Если что-то пошло не так при сбросе базы, предлагаем пользователю удалить данные приложения
            new AlertDialog.Builder(this)
                .setTitle("Ошибка базы данных")
                .setMessage("Произошла критическая ошибка при обновлении базы данных. Рекомендуется удалить данные приложения в настройках.")
                .setPositiveButton("ОК", null)
                .show();
        }
    }
    
    /**
     * Временное решение: очистка таблицы новостей при запуске
     */
    // private void clearNewsTable() {
    //     Log.d(TAG, "Очищаем таблицу новостей...");
    //     if (newsViewModel != null) {
    //         // Сразу добавляем индикатор загрузки для UI
    //         Toast.makeText(this, "Очистка данных новостей...", Toast.LENGTH_SHORT).show();
            
    //         new Thread(() -> {
    //             try {
    //                 // Повторяем очистку несколько раз для надежности
    //                 for (int i = 0; i < 3; i++) {
    //                     Log.d(TAG, "Попытка очистки новостей #" + (i+1));
    //                     newsViewModel.clearAllNews();
    //                     Thread.sleep(300); // Короткая пауза между попытками
    //                 }
                    
    //                 // Обновляем UI на главном потоке
    //                 runOnUiThread(() -> {
    //                     Toast.makeText(MainActivity.this, "Таблица новостей очищена", Toast.LENGTH_SHORT).show();
    //                     Log.d(TAG, "Таблица новостей успешно очищена");
    //                 });
    //             } catch (Exception e) {
    //                 Log.e(TAG, "Ошибка при очистке таблицы новостей", e);
                    
    //                 // Показываем ошибку пользователю
    //                 runOnUiThread(() -> {
    //                     Toast.makeText(MainActivity.this, "Ошибка очистки новостей: " + e.getMessage(), Toast.LENGTH_LONG).show();
    //                 });
    //             }
    //         }).start();
    //     } else {
    //         Log.e(TAG, "newsViewModel равен null, очистка таблицы не выполнена");
    //     }
    // }
    
    /**
     * Инициализирует все панели и BottomSheet-ы.
     */
    private void initializePanels() {
        Log.d(TAG, "Инициализация панелей");
        
        // dimOverlay = binding.dimOverlay; // Это уже должно быть закомментировано
        
        // --- НАЧАЛО: БЛОК ДЛЯ УДАЛЕНИЯ/КОММЕНТИРОВАНИЯ ---
        /*
        // Инициализация профиля
        ConstraintLayout profilePanel = findViewById(R.id.user_profile_panel);
        if (profilePanel != null) {
            Log.d(TAG, "Панель профиля найдена через findViewById");
            
            profilePanel.setVisibility(View.VISIBLE);
            
            profileSheetBehavior = BottomSheetBehavior.from(profilePanel);
            profileSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            
            profileSheetBehavior.setFitToContents(true);
            profileSheetBehavior.setDraggable(true);
            
            profileSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    switch (newState) {
                        case BottomSheetBehavior.STATE_HIDDEN:
                            bottomSheet.setVisibility(View.GONE);
                            clearBottomSheetContent(); // Это может быть связано с contentDetailsSheetBehavior, проверить
                            // if (contentDetailsSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) { // Логика dimOverlay удалена
                            // }
                            break;
                        case BottomSheetBehavior.STATE_EXPANDED:
                        case BottomSheetBehavior.STATE_HALF_EXPANDED:
                        case BottomSheetBehavior.STATE_COLLAPSED:
                            bottomSheet.setVisibility(View.VISIBLE);
                            break;
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    // Логика dimOverlay удалена
                }
            });
            
            ImageView closeProfileButton = profilePanel.findViewById(R.id.close_profile);
            if (closeProfileButton != null) {
                closeProfileButton.setOnClickListener(v -> hideProfilePanel());
            }
            
            Button logoutButton = profilePanel.findViewById(R.id.btn_logout);
            if (logoutButton != null) {
                logoutButton.setOnClickListener(v -> {
                    hideProfilePanel();
                    new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("Выход из аккаунта")
                        .setMessage("Вы уверены, что хотите выйти из аккаунта?")
                        .setPositiveButton("Да", (dialog, which) -> {
                            logout();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
                });
            }
            
            Log.d(TAG, "Панель профиля инициализирована. Состояние: " + profileSheetBehavior.getState());
        } else {
            Log.e(TAG, "Не удалось найти панель профиля через findViewById!");
        }
        */
        // --- КОНЕЦ: БЛОК ДЛЯ УДАЛЕНИЯ/КОММЕНТИРОВАНИЯ ---
        
        // Инициализация панели деталей контента (остается, если используется)
        ConstraintLayout contentDetailsPanel = findViewById(R.id.content_details_panel);
        if (contentDetailsPanel != null) {
            contentDetailsPanel.setVisibility(View.VISIBLE);
            contentDetailsSheetBehavior = BottomSheetBehavior.from(contentDetailsPanel);
            contentDetailsSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            
            contentDetailsSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    switch (newState) {
                        case BottomSheetBehavior.STATE_HIDDEN:
                            bottomSheet.setVisibility(View.GONE);
                            clearBottomSheetContent();
                            // Логика dimOverlay удалена
                            break;
                        case BottomSheetBehavior.STATE_EXPANDED:
                        case BottomSheetBehavior.STATE_HALF_EXPANDED:
                        case BottomSheetBehavior.STATE_COLLAPSED:
                            bottomSheet.setVisibility(View.VISIBLE);
                            break;
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    // Логика dimOverlay удалена
                }
            });
            
            ImageView closePanelButton = contentDetailsPanel.findViewById(R.id.close_panel);
            if (closePanelButton != null) {
                closePanelButton.setVisibility(View.GONE);
            }
        }
        
        // dimOverlay click listener уже должен быть удален
    }
    
    private void setupUI() {
        // Находим шиммеры
        View scrollContentView = binding.contentMain.findViewById(R.id.scroll_content);
        
        shimmerProgressLayout = scrollContentView.findViewById(R.id.shimmer_progress);
        shimmerNewsLayout = scrollContentView.findViewById(R.id.shimmer_news);
        shimmerContentLayout = scrollContentView.findViewById(R.id.shimmer_content);
//        shimmerVariantLayout = scrollContentView.findViewById(R.id.shimmer_tasks); // <--- Добавлено поле
        
        // Находим плейсхолдеры ошибок
        errorPlaceholderProgress = scrollContentView.findViewById(R.id.error_placeholder_progress);
        errorPlaceholderNews = scrollContentView.findViewById(R.id.error_placeholder_news);
        errorPlaceholderContent = scrollContentView.findViewById(R.id.error_placeholder_content);
//        errorPlaceholderVariant = scrollContentView.findViewById(R.id.error_placeholder_variant); // <--- Добавлено поле
        
        // Находим RecyclerViews
        progressRecycler = scrollContentView.findViewById(R.id.recycler_progress);
        newsRecycler = scrollContentView.findViewById(R.id.recycler_news);
//        variantRecycler = scrollContentView.findViewById(R.id.recycler_variant); // <--- Добавлено поле
        
        // Добавляем обработчик клика на аватар для открытия профиля
        ImageView userAvatar = binding.getRoot().findViewById(R.id.user_avatar);
        
        if (userAvatar != null) {
            // Устанавливаем явные свойства кликабельности
            userAvatar.setClickable(true);
            userAvatar.setFocusable(true);
            
            // Добавляем обработчик нажатия с логированием
            userAvatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Avatar clicked, showing profile panel");
                    // Добавляем вибрацию или звук для обратной связи
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    // Открываем панель профиля
                    showProfilePanel();
                }
            });
            
            // Также устанавливаем onTouchListener для диагностики проблем с нажатием
            userAvatar.setOnTouchListener((v, event) -> {
                Log.d(TAG, "Avatar touch event: " + event.getAction());
                // Не поглощаем событие, позволяем ему продолжить к onClick
                return false;
            });
            
            Log.d(TAG, "Avatar click listener set up successfully");
        } else {
            Log.e(TAG, "Cannot find user avatar in binding!");
        }
        
        // Настройка TabLayout для навигации
        setupTabLayout();
    }
    
    private void setupTabLayout() {
        TabLayout tabLayout = binding.contentMain.findViewById(R.id.tab_navigation);
        
        // Очищаем существующие вкладки
        tabLayout.removeAllTabs();
        
        // Добавляем вкладки с пользовательским макетом
        for (int i = 0; i < categoryTitles.length; i++) {
            String title = categoryTitles[i];
            
            // Создаем пользовательское представление для вкладки
            View customView = getLayoutInflater().inflate(R.layout.custom_tab, null);
            TextView textView = customView.findViewById(R.id.tab_text);
            textView.setText(title);
            textView.setContentDescription(title + " - раздел");
            
            // Добавляем вкладку с пользовательским представлением
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setCustomView(customView);
            tabLayout.addTab(tab);
        }
        
        // Установка слушателя для обработки выбора вкладок
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                String categoryId = categories[position];
                Log.d(TAG, "Выбрана категория: " + categoryId);
                
                // Находим контейнеры
                FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
                FrameLayout contentContainer = binding.contentContainer;
                androidx.core.widget.NestedScrollView scrollContent = binding.contentMain.findViewById(R.id.scroll_content);
                
                // Очищаем содержимое контейнера content_container
                if (contentContainer != null) {
                    contentContainer.removeAllViews();
                    Log.d(TAG, "Контейнер content_container очищен от прежнего содержимого");
                }
                
                // Сначала скрываем все компоненты и фрагменты
                hideAllFragments();
                
                if (scrollContent != null) {
                    scrollContent.setVisibility(View.VISIBLE);
                }
                if (contentContainer != null) {
                    contentContainer.setVisibility(View.VISIBLE);
                }
                if (fragmentContainer != null) {
                    fragmentContainer.setVisibility(View.GONE);
                }
                
                // Для каждой категории заполняем content_container
                if ("theory".equals(categoryId)) {
                    Log.d(TAG, "Переход к теории");
                    
                    // Сначала отображаем закэшированные данные в content_container
                    displayCachedContent(categoryId);
                    
                    // Затем, если нужно, можно также показать фрагмент в fragment_container
                    //showTheoryFragment();
                }
                else if ("task".equals(categoryId)) {
                    Log.d(TAG, "Переход к заданиям");
                    
                    // Тот же подход, как для theory - сначала отображаем данные в content_container
                    displayCachedContent(categoryId);
                    
                    // Затем, если нужно, можно также показать фрагмент в fragment_container 
                    //showExercisesFragment();
                } else if ("shpargalka".equals(categoryId)) {
                    Log.d(TAG, "Переход к шпаргалкам");
                    
                    // Отображаем закэшированные данные шпаргалок
                    displayCachedContent(categoryId);
                    
                    // Если данных нет в кэше, загружаем их
                    if (!categoryDataCache.containsKey(categoryId) || categoryDataCache.get(categoryId).isEmpty()) {
                        ShpargalkaViewModel shpargalkaViewModel = new ViewModelProvider(MainActivity.this).get(ShpargalkaViewModel.class);
                        shpargalkaViewModel.loadShpargalkaItems();
                        Log.d(TAG, "Запущена загрузка шпаргалок, так как кэш пуст");
                    } else {
                        // Принудительно обновляем данные шпаргалок для актуализации
                        ShpargalkaViewModel shpargalkaViewModel = new ViewModelProvider(MainActivity.this).get(ShpargalkaViewModel.class);
                        shpargalkaViewModel.refreshShpargalkaData();
                        Log.d(TAG, "Запущено принудительное обновление шпаргалок");
                    }
                } else if ("variant".equals(categoryId)) { // <--- Условие для вариантов
                    Log.d(TAG, "Переход к вариантам");
                    // Показываем кэшированные варианты или шиммер, если кэш пуст
                    displayCachedVariants(); // <--- Новый метод для вариантов

                    // Запускаем загрузку вариантов, если кэш пуст
                    // (ViewModel уже делает это при инициализации, но можно добавить проверку)
                    if (!variantDataCache.containsKey("variants") || variantDataCache.get("variants").isEmpty()) {
                        variantViewModel.fetchVariants(); // Убедимся, что загрузка запущена
                        Log.d(TAG, "Запущена загрузка вариантов, так как кэш пуст или не содержит ключ 'variants'");
                    }

                } else {
                    // Для других категорий просто отображаем содержимое в content_container
                    displayCachedContent(categoryId);
                    Log.d(TAG, "Отображено содержимое для категории: " + categoryId);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Обновление внешнего вида происходит автоматически через селекторы
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // При повторном выборе можно обновить данные (опционально)
                // String selectedCategory = categories[tab.getPosition()];
                // fetchContentDataForCategory(selectedCategory);
            }
        });
    }
    
    private void setupAdaptersAndObservers() {
        // ----------------- Настройка адаптеров -----------------
        
        // Настройка адаптера прогресса
        progressAdapter = new ProgressAdapter(new ArrayList<>(), onProgressClickListener);
        progressRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        progressRecycler.setAdapter(progressAdapter);
        progressRecycler.setItemAnimator(new SlowItemAnimator());
        // Применяем LayoutAnimation для первого появления
        try {
            final LayoutAnimationController controller = 
                    AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fade_bottom);
            progressRecycler.setLayoutAnimation(controller);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Layout animation resource not found for progressRecycler", e);
        }
        
        // Настройка адаптера новостей
        newsAdapter = new NewsAdapter(new ArrayList<>(), onNewsClickListener);
        newsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        newsRecycler.setAdapter(newsAdapter);
        newsRecycler.setItemAnimator(new SlowItemAnimator());
        // Применяем LayoutAnimation для первого появления
        try {
            final LayoutAnimationController controller =
                    AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fade_bottom);
            newsRecycler.setLayoutAnimation(controller);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Layout animation resource not found for newsRecycler", e);
        }
        
        // ----------------- Наблюдатели LiveData -----------------
        
        // Показываем шиммер при загрузке данных
        showShimmer(shimmerProgressLayout, progressRecycler, errorPlaceholderProgress);
        showShimmer(shimmerNewsLayout, newsRecycler, errorPlaceholderNews);
        showShimmer(shimmerContentLayout, binding.contentContainer, errorPlaceholderContent);
        showShimmer(shimmerVariantLayout, variantRecycler, errorPlaceholderVariant); // <--- Добавлено поле
        
        // Очистка таблицы прогресса при запуске
        if (progressViewModel != null) {
            Log.d(TAG, "Инициализация прогресса пользователя при запуске...");
            // progressViewModel.clearAllUserProgressData(); // Удаляем эту строку, она мешает работе прогресса
        }
        
        // Проверка и инициализация прогресса пользователя (только локально, без обращения к серверу)
        progressViewModel.checkAndInitializeProgress();
        // НЕ обновляем прогресс с сервера при каждом запуске - это будет делать periodicSync или счетчик заданий
        // progressViewModel.refreshProgress();
        
        // Наблюдаем за статусом пользователя (новый или существующий)
        progressViewModel.getIsFirstTimeUser().observe(this, isFirstTimeUserValue -> {
            Log.d(TAG, "Статус первого входа пользователя: " + isFirstTimeUserValue);
            if (isFirstTimeUserValue != null && isFirstTimeUserValue) {
                // Это первый вход пользователя, показываем приветственное сообщение
                Toast.makeText(this, "Добро пожаловать! Мы подготовили для вас начальные материалы.", 
                        Toast.LENGTH_LONG).show();
            }
            // В любом случае загружаем прогресс, когда isFirstTimeUser становится известен
            progressViewModel.loadInitialProgress();
        });
        
        // Наблюдаем за данными прогресса
        progressViewModel.getUserProgress().observe(this, progressEntities -> {
            updateProgressUI(progressEntities);
        });
        
        // Наблюдаем за данными новостей
        newsViewModel.getNewsLiveData().observe(this, newsEntities -> {
            updateNewsUI(newsEntities);
        });
        
        // Наблюдаем за данными теории
        contentViewModel.getTheoryTopicsLiveData().observe(this, theoryTopics -> {
            // Преобразуем ContentEntity в ContentItem для кэша
            List<ContentItem> contentItems = new ArrayList<>();
            if (theoryTopics != null) {
                for (ContentEntity entity : theoryTopics) {
                    if (entity != null) {
                        contentItems.add(new ContentItem(
                            entity.getContentId(),
                            entity.getTitle(),
                            entity.getDescription(),
                            entity.getType(),
                            entity.getParentId(),
                            entity.isDownloaded()
                        ));
                    }
                }
            }
            
            // Кэшируем данные теории
            categoryDataCache.put("theory", contentItems);
            
            // Если первая вкладка выбрана (теория), отображаем данные,
            // но НЕ запрашиваем данные повторно
            TabLayout tabLayout = binding.contentMain.findViewById(R.id.tab_navigation);
            if (tabLayout.getSelectedTabPosition() == 0) {
                displayCachedContent("theory");
            }
            
            showData(shimmerContentLayout, binding.contentContainer, errorPlaceholderContent);
        });
        
        // Наблюдаем за данными заданий
        contentViewModel.getTasksTopicsLiveData().observe(this, taskGroups -> {
            // Преобразуем ContentEntity в ContentItem для кэша
            List<ContentItem> taskItems = new ArrayList<>();
            if (taskGroups != null) {
                for (ContentEntity entity : taskGroups) {
                    if (entity != null) {
                        taskItems.add(new ContentItem(
                            entity.getContentId(),
                            entity.getTitle(),
                            entity.getDescription(),
                            entity.getType(),
                            entity.getParentId(),
                            entity.isDownloaded()
                        ));
                    }
                }
            }
            
            // Кэшируем данные заданий
            categoryDataCache.put("task", taskItems);
            Log.d(TAG, "Закэшировано " + taskItems.size() + " элементов заданий");
            
            // Обновляем UI только если выбрана вкладка заданий
            TabLayout tabLayout = binding.contentMain.findViewById(R.id.tab_navigation);
            if (tabLayout.getSelectedTabPosition() == 1) { // Задания - вторая вкладка (индекс 1)
                Log.d(TAG, "Обновляем UI для заданий, так как выбрана вкладка заданий");
                displayCachedContent("task");
            }
        });
        
        // Инициализируем ViewModel для шпаргалок
        ShpargalkaViewModel shpargalkaViewModel = new ViewModelProvider(this).get(ShpargalkaViewModel.class);
        
        // Загружаем шпаргалки при инициализации
        shpargalkaViewModel.loadShpargalkaItems();
        
        // Наблюдаем за данными шпаргалок
        shpargalkaViewModel.getShpargalkaContents().observe(this, shpargalkaContents -> {
            // Преобразуем ContentItem в ContentItem для кэша (уже в правильном формате)
            List<ContentItem> shpargalkaItems = new ArrayList<>(shpargalkaContents);
            
            // Кэшируем данные шпаргалок
            categoryDataCache.put("shpargalka", shpargalkaItems);
            Log.d(TAG, "Закэшировано " + shpargalkaItems.size() + " элементов шпаргалок");
            
            // Обновляем UI только если выбрана вкладка шпаргалок
            TabLayout tabLayout = binding.contentMain.findViewById(R.id.tab_navigation);
            if (tabLayout.getSelectedTabPosition() == 2) { // Шпаргалки - третья вкладка (индекс 2)
                Log.d(TAG, "Обновляем UI для шпаргалок, так как выбрана вкладка шпаргалок");
                displayCachedContent("shpargalka");
            }
        });

        // Наблюдаем за данными вариантов
        variantViewModel.getVariantsLiveData().observe(this, resource -> {
            if (resource != null) {
                TabLayout tabLayout = binding.contentMain.findViewById(R.id.tab_navigation);
                boolean isVariantTabSelected = tabLayout.getSelectedTabPosition() == 3; // Индекс вкладки "Варианты"

                if (resource instanceof Resource.Success) {
                    Log.d(TAG, "Варианты успешно загружены: " + (resource.data != null ? resource.data.size() : 0) + " шт.");
                    if (resource.data != null) {
                        variantDataCache.put("variants", resource.data);
                        if (isVariantTabSelected) {
                            displayCachedVariants();
                        }
                    } else {
                        variantDataCache.put("variants", new ArrayList<>()); // Кладём пустой список, если данные null
                         if (isVariantTabSelected) {
                            displayCachedVariants(); // Отобразит "нет данных"
                        }
                    }
                } else if (resource instanceof Resource.Error) {
                    Log.e(TAG, "Ошибка загрузки вариантов: " + resource.message);
                    variantDataCache.put("variants", new ArrayList<>()); // Кладём пустой список при ошибке
                    if (isVariantTabSelected) {
                        displayCachedVariants(); // Отобразит ошибку
                    }
                } else if (resource instanceof Resource.Loading) {
                    Log.d(TAG, "Загрузка вариантов...");
                    if (isVariantTabSelected && (!variantDataCache.containsKey("variants") || variantDataCache.get("variants").isEmpty())) {
                         displayCachedVariants();
                    }
                }
            }
        });

        // Наблюдатель для деталей варианта (для отображения BottomSheet)
        variantViewModel.getVariantDetailsLiveData().observe(this, resource -> {
            if (resource instanceof Resource.Loading) {
                // Можно показать глобальный индикатор загрузки, если BottomSheet еще не открыт
                // или обработать состояние загрузки внутри BottomSheet
                Log.d(TAG, "MainActivity: VariantDetails Loading...");
            } else if (resource instanceof Resource.Success) {
                Log.d(TAG, "MainActivity: VariantDetails Success. Ready to show BottomSheet.");
                VariantEntity variantDetails = resource.data; // Прямой доступ к свойству
                if (variantDetails != null && !isFinishing() && !isDestroyed()) {
                    // Проверяем, не открыт ли уже такой BottomSheet
                    Fragment existingFragment = getSupportFragmentManager().findFragmentByTag("VariantDetailBottomSheet");
                    if (existingFragment == null) {
                        VariantDetailBottomSheetDialogFragment bottomSheet = 
                                VariantDetailBottomSheetDialogFragment.newInstance(String.valueOf(variantDetails.getVariantId()), variantDetails.getName());
                        bottomSheet.show(getSupportFragmentManager(), "VariantDetailBottomSheet");
                    }
                }
            } else if (resource instanceof Resource.Error) {
                Log.e(TAG, "MainActivity: VariantDetails Error: " + resource.message); // Прямой доступ к свойству
                Toast.makeText(this, "Ошибка загрузки деталей варианта: " + resource.message, Toast.LENGTH_LONG).show(); // Прямой доступ к свойству
            }
        });

        // Можно также подписаться на sharedTextsLiveData и tasksLiveData здесь,ENGTH_LONG).show();
        // если нужно выполнить какие-то действия в MainActivity при их загрузке,
        // но основное их отображение будет внутри BottomSheet.
    }
    
    private void showShimmer(ShimmerFrameLayout shimmerLayout, View contentView, @Nullable View errorPlaceholder) {
        if (shimmerLayout != null) {
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
        }
        if (contentView != null) {
            contentView.setVisibility(View.GONE);
        }
        if (errorPlaceholder != null) {
            errorPlaceholder.setVisibility(View.GONE);
        }
    }

    private void showData(ShimmerFrameLayout shimmerLayout, View contentView, @Nullable View errorPlaceholder) {
        if (shimmerLayout != null) {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
        }
        if (contentView != null) {
            contentView.setVisibility(View.VISIBLE);
        }
        if (errorPlaceholder != null) {
            errorPlaceholder.setVisibility(View.GONE);
        }
    }

    private void showError(ShimmerFrameLayout shimmerLayout, View contentView, @Nullable View errorPlaceholder) {
        if (shimmerLayout != null) {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
        }
        if (contentView != null) {
            contentView.setVisibility(View.GONE);
        }
        if (errorPlaceholder != null) {
            errorPlaceholder.setVisibility(View.VISIBLE);
            // Для errorPlaceholderProgress и errorPlaceholderNews текст ошибки задан в XML,
            // поэтому здесь не пытаемся его изменить.
            // Для errorPlaceholderContent текст будет установлен в displayCachedVariants.
        }
    }
    
    private void displayCachedContent(String categoryId) {
        FrameLayout contentContainer = binding.contentContainer;
        if (contentContainer == null) {
            Log.e(TAG, "Content container is null");
            return;
        }
        
        // Убедимся что контейнер пустой
        contentContainer.removeAllViews();
        Log.d(TAG, "Контейнер contentContainer очищен перед заполнением новыми данными для " + categoryId);
        
        // Получаем кэшированные данные
        List<ContentItem> items = categoryDataCache.getOrDefault(categoryId, new ArrayList<>());
        
        if (items.isEmpty()) {
            // Если нет данных в кэше, показываем шиммер
            showShimmer(shimmerContentLayout, contentContainer, errorPlaceholderContent);
            Log.d(TAG, "Нет данных в кэше для " + categoryId + ", показываем шиммер");
            return;
        }
        
        // Создаем RecyclerView для отображения контента
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setId(R.id.content_recycler); // Предполагается, что этот ID определен где-то
        
        // Создаем адаптер с обработчиком кликов для всех типов контента
        contentAdapter = new ContentAdapter(items, onContentClickListener);
        
        // Применяем разделители для улучшения читаемости
        recyclerView.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
            this, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL));
        
        // Устанавливаем отступы
        recyclerView.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        recyclerView.setClipToPadding(false);
        
        // Устанавливаем адаптер
        recyclerView.setAdapter(contentAdapter);
        
        // Добавляем анимацию к RecyclerView
        try {
            LayoutAnimationController animation = 
                AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fade_bottom);
            recyclerView.setLayoutAnimation(animation);
        } catch (Exception e) {
            Log.e(TAG, "Error loading animation", e);
        }
        
        // Заголовок для контента
        TextView headerView = new TextView(this);
        if ("task".equals(categoryId)) {
            headerView.setText("Задания ЕГЭ");
        } else if ("theory".equals(categoryId)) {
            headerView.setText("Теория");
        } else if ("shpargalka".equals(categoryId)) {
            headerView.setText("Шпаргалки"); // Изменено здесь
        } else {
            headerView.setText(categoryId.substring(0, 1).toUpperCase() + categoryId.substring(1));
        }
        
        headerView.setTextSize(20);
        headerView.setTypeface(headerView.getTypeface(), android.graphics.Typeface.BOLD);
        headerView.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(8));
        // Используем атрибут темы для цвета текста
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        headerView.setTextColor(ContextCompat.getColor(this, typedValue.resourceId));
        
        // Создаем LinearLayout для размещения заголовка и списка
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(headerView);
        linearLayout.addView(recyclerView);
        
        // Добавляем LinearLayout в контейнер вместо RecyclerView
        contentContainer.addView(linearLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 
                FrameLayout.LayoutParams.WRAP_CONTENT));
        
        // Убедимся, что контейнер видим
        contentContainer.setVisibility(View.VISIBLE);
        
        // Показываем данные
        showData(shimmerContentLayout, contentContainer, errorPlaceholderContent);
        
        Log.d(TAG, "Контейнер contentContainer заполнен данными для категории " + categoryId + ", " + items.size() + " элементов");
    }
    
    /**
     * Обновляет UI с новостями.
     */
    private void updateNewsUI(List<NewsEntity> newsEntities) {
        Log.d(TAG, "News received: " + (newsEntities != null ? newsEntities.size() : 0));
        
        // Маппинг Entity -> Item
        List<NewsItem> newsItems = new ArrayList<>();
        if (newsEntities != null) {
            for (NewsEntity entity : newsEntities) {
                if (entity != null) {
                    // Простое преобразование long даты в строку. Возможно, понадобится форматирование.
                    String dateString = String.valueOf(entity.getPublicationDate());
                    newsItems.add(new NewsItem(
                        entity.getTitle(),
                        dateString, 
                        entity.getDescription(),
                        entity.getImageUrl(),
                        null // fullContentUrl пока null
                    ));
                }
            }
        }

        if (newsAdapter != null) {
            newsAdapter.submitList(newsItems);
            Log.d(TAG, "Submitted list to NewsAdapter.");
            
            if (newsItems.isEmpty()) {
                showError(shimmerNewsLayout, newsRecycler, errorPlaceholderNews);
            } else {
                showData(shimmerNewsLayout, newsRecycler, errorPlaceholderNews);
            }
        } else {
            Log.e(TAG, "NewsAdapter is null during updateNewsUI!");
        }
    }
    
    /**
     * Обновляет UI с прогрессом.
     */
    private void updateProgressUI(List<ProgressEntity> progressEntities) {
        if (progressEntities == null || progressEntities.isEmpty()) {
            Log.d(TAG, "updateProgressUI: пустой список прогресса");
            
            // Если данных нет, показываем заглушку с ошибкой с небольшой задержкой
            if (progressRecycler != null) {
                handler.postDelayed(() -> {
                    // Проверяем, не появились ли данные за время задержки
                    if (progressViewModel.getUserProgress().getValue() == null || 
                        progressViewModel.getUserProgress().getValue().isEmpty()) {
                        showError(shimmerProgressLayout, progressRecycler, errorPlaceholderProgress);
                        Log.d(TAG, "updateProgressUI: данные все еще отсутствуют, показываем ошибку");
                        // Пробуем инициализировать прогресс еще раз
                        progressViewModel.initializeProgress();
                    }
                }, 2000); // Задержка в 2 секунды
            }
            return;
        }
        
        Log.d(TAG, "updateProgressUI: обновление данных прогресса, получено " + progressEntities.size() + " записей");
        
        // Выводим информацию о всех записях для диагностики
        for (ProgressEntity entity : progressEntities) {
            Log.d(TAG, "Запись прогресса: contentId=" + entity.getContentId() + 
                  ", title=" + entity.getTitle() + ", percentage=" + entity.getPercentage() +
                  ", completed=" + entity.isCompleted());
        }
        
        // Фильтруем только записи для заданий (task_group)
        List<ProgressEntity> taskGroupEntities = new ArrayList<>();
        for (ProgressEntity entity : progressEntities) {
            if (entity.getContentId() != null && entity.getContentId().startsWith("task_group_")) {
                taskGroupEntities.add(entity);
                Log.d(TAG, "Найдена запись прогресса для задания: " + entity.getContentId() + ", завершено: " + entity.isCompleted());
            }
        }
        
        Log.d(TAG, "updateProgressUI: после фильтрации осталось " + taskGroupEntities.size() + " записей типа task_group_");
        
        // Если нет заданий, создадим список из всех возможных номеров заданий ЕГЭ
        if (taskGroupEntities.isEmpty()) {
            Log.d(TAG, "Записей с task_group_ не найдено в БД, инициализируем прогресс");
            progressViewModel.initializeProgress();
            return;
        }
        
        // Если есть задания, обновляем адаптер
        if (!taskGroupEntities.isEmpty()) {
            // Получаем общее количество завершенных и незавершенных заданий
            int completedTasksCount = 0;
            for (ProgressEntity entity : taskGroupEntities) {
                if (entity.isCompleted()) {
                    completedTasksCount++;
                }
            }
            
            // Создаем элементы прогресса для отображения
            ArrayList<com.ruege.mobile.model.ProgressItem> progressItems = new ArrayList<>();
            
            // Добавляем главный элемент с общим прогрессом
            // Используем константу 27 - общее количество типов заданий в ЕГЭ
            final int TOTAL_EGE_TASK_TYPES = 27;
            int overallPercentage = (completedTasksCount * 100) / TOTAL_EGE_TASK_TYPES;
            
            Log.d(TAG, "Создание элемента общего прогресса: процент = " + overallPercentage);
                
            com.ruege.mobile.model.ProgressItem overallProgress = new com.ruege.mobile.model.ProgressItem(
                "overall_progress", 
                "Общий прогресс", 
                "Выполнено " + completedTasksCount + " из " + TOTAL_EGE_TASK_TYPES + " типов заданий",
                overallPercentage,
                "PROGRESS"
            );
            progressItems.add(overallProgress);
            
            // Добавляем прогресс по конкретным группам заданий
            for (ProgressEntity entity : taskGroupEntities) {
                String taskNumber = entity.getContentId().replace("task_group_", "");
                String title = "Задание " + taskNumber;
                String description = entity.isCompleted() ? "Завершено" : "В процессе";
                
                Log.d(TAG, "Создание элемента задания: contentId = " + entity.getContentId() + 
                      ", title = " + title + ", процент = " + entity.getPercentage() + 
                      ", завершено = " + entity.isCompleted());
                
                com.ruege.mobile.model.ProgressItem item = new com.ruege.mobile.model.ProgressItem(
                    entity.getContentId(),
                    title,
                    description,
                    entity.getPercentage(),
                    "TASK" // Всегда устанавливаем "TASK" для элементов заданий
                );
                progressItems.add(item);
            }
            
            // Проверяем все созданные элементы перед передачей в адаптер
            for (int i = 0; i < progressItems.size(); i++) {
                com.ruege.mobile.model.ProgressItem item = progressItems.get(i);
                Log.d(TAG, "Проверка элемента " + i + ": id = " + item.getId() + 
                      ", title = " + item.getTitle() + ", type = " + item.getType() + 
                      ", процент = " + item.getPercentage());
            }
            
            // Обновляем адаптер и показываем данные
            if (progressAdapter == null) {
                progressAdapter = new ProgressAdapter(progressItems, onProgressClickListener);
                progressRecycler.setAdapter(progressAdapter);
                Log.d(TAG, "updateProgressUI: создан новый адаптер с " + progressItems.size() + " элементами");
            } else {
                progressAdapter.updateItems(progressItems);
                Log.d(TAG, "updateProgressUI: обновлен существующий адаптер с " + progressItems.size() + " элементами");
            }
            
            showData(shimmerProgressLayout, progressRecycler, errorPlaceholderProgress);
            Log.d(TAG, "updateProgressUI: данные прогресса обновлены, общий прогресс: " + overallPercentage + "%");
        } else {
            // Если заданий нет, показываем ошибку
            showError(shimmerProgressLayout, progressRecycler, errorPlaceholderProgress);
            Log.w(TAG, "updateProgressUI: нет записей прогресса для типов заданий");
            
            // Инициализируем прогресс, если нет записей типа task_group
            if (progressViewModel != null) {
                Log.d(TAG, "Запуск инициализации прогресса, так как нет записей типа task_group");
                progressViewModel.initializeProgress();
            }
        }
    }
    
    /**
     * Настраивает переключатель темы приложения.
     */
    private void setupThemeToggle() {
        MaterialButton themeToggle = findViewById(R.id.theme_toggle);
        if (themeToggle != null) {
            // Установка начального состояния
            boolean useDarkTheme = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("dark_theme", false);
                
            // Установка иконки при старте на основе текущей темы
            updateThemeToggleIcon(themeToggle, useDarkTheme);
            
            // Установка обработчика клика
            themeToggle.setOnClickListener(v -> {
                // Получаем текущее состояние темы
                boolean currentDarkTheme = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getBoolean("dark_theme", false);
                
                // Меняем на противоположное
                boolean newDarkTheme = !currentDarkTheme;
                
                // Сохраняем новое состояние
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("dark_theme", newDarkTheme)
                    .apply();
                
                // Анимируем иконку
                updateThemeToggleIcon(themeToggle, newDarkTheme);
                
                // Применяем новую тему
                AppCompatDelegate.setDefaultNightMode(
                    newDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                
                // Активность будет пересоздана из-за изменения конфигурации
            });
        }
    }
    
    /**
     * Обновляет иконку переключателя темы и запускает анимацию.
     */
    private void updateThemeToggleIcon(MaterialButton button, boolean isDarkTheme) {
        if (button != null) {
            button.setIcon(isDarkTheme ? 
                getDrawable(R.drawable.ic_lantern_on) : 
                getDrawable(R.drawable.ic_lantern_off));
            
            // Запускаем анимацию, если доступна
            Drawable drawable = button.getIcon();
            if (drawable instanceof Animatable) {
                ((Animatable) drawable).start();
            }
        }
    }
    
    /**
     * Показывает панель профиля пользователя
     */
    private void showProfilePanel() {
        Log.d(TAG, "showProfilePanel called - displaying ProfileBottomSheetDialogFragment");
        
        // Сначала скрываем панель деталей контента, если она открыта и это все еще релевантно
        // (убедитесь, что contentDetailsSheetBehavior инициализирован, если этот код остается)
        if (contentDetailsSheetBehavior != null && 
            contentDetailsSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            contentDetailsSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        
        ProfileBottomSheetDialogFragment profileSheet = ProfileBottomSheetDialogFragment.newInstance();
        // Можно проверить, не показан ли уже такой фрагмент, чтобы избежать дублирования
        Fragment existingFragment = getSupportFragmentManager().findFragmentByTag("ProfileBottomSheet");
        if (existingFragment == null) {
            profileSheet.show(getSupportFragmentManager(), "ProfileBottomSheet");
        }
    }

    /**
     * Скрывает панель профиля пользователя
     */
    private void hideProfilePanel() {
        if (contentDetailsSheetBehavior != null) {
            contentDetailsSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }
    
    /**
     * Обновляет содержимое панели деталей контента.
     */
    private void updatePanelContent(String title, String description, String actionText) {
        TextView titleView = binding.contentDetailsPanel.findViewById(R.id.content_title);
        TextView descriptionView = binding.contentDetailsPanel.findViewById(R.id.content_description);
        TextView panelTitleView = binding.contentDetailsPanel.findViewById(R.id.panel_title);
        
        if (titleView != null) titleView.setText(title);
        if (descriptionView != null) descriptionView.setText(description);
        if (panelTitleView != null) panelTitleView.setText("Детали");
        
        // Настраиваем кнопку действия
        //if (actionText != null && !actionText.isEmpty()) {
        //    actionButton.setText(actionText);
        //    actionButton.setVisibility(View.VISIBLE);
        //    actionButton.setOnClickListener(v -> {
        //        // Закрываем панель деталей
        //        hideContentDetailsSheet();
        //
        //        // TODO: Открываем полную теорию в отдельном экране
        //        Toast.makeText(this, "Открытие полной теории: " + title, Toast.LENGTH_SHORT).show();
        //    });
        //} else {
        //    actionButton.setVisibility(View.GONE);
        //}
    }
    
    /**
     * Показывает нижнюю панель с деталями контента
     */
    private void showBottomSheet(String title, String description, String contentId, String type) {
        if (contentDetailsSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            contentDetailsSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        // Очищаем предыдущее содержимое
        clearBottomSheetContent();
        
        Log.d(TAG, "showBottomSheet: вызван с title=" + title + ", contentId=" + contentId + ", type=" + type);

        // В зависимости от типа контента вызываем соответствующий метод для отображения
        switch(type) {
            case "theory":
                TheoryBottomSheetDialogFragment theorySheet = TheoryBottomSheetDialogFragment.newInstance(contentId, title);
                if (getSupportFragmentManager().findFragmentByTag("TheoryBottomSheet") == null) {
                    theorySheet.show(getSupportFragmentManager(), "TheoryBottomSheet");
                }
                break;
            case "task":
                //   showTasksBottomSheet(title, description, contentId);
                Log.d(TAG, "showBottomSheet: Показываем TaskDisplayBottomSheetDialogFragment для категории: " + contentId + ", title: " + title);
                TaskDisplayBottomSheetDialogFragment taskSheet = TaskDisplayBottomSheetDialogFragment.newInstance(contentId, title);
                if (getSupportFragmentManager().findFragmentByTag(TaskDisplayBottomSheetDialogFragment.TAG_TASK_DISPLAY_BS) == null) { // Используем TAG из компаньона TaskDisplayBottomSheetDialogFragment
                    taskSheet.show(getSupportFragmentManager(), TaskDisplayBottomSheetDialogFragment.TAG_TASK_DISPLAY_BS);
                }
                break;
            case "news":
                showNewsBottomSheet(title, description, contentId); // Этот метод специфичен для новостей
                break;
            case "shpargalka":
                showShpargalkaBottomSheet(title, description, contentId); // Этот метод использует свой собственный BottomSheetDialog
                break;
            default:
                Log.e(TAG, "showBottomSheet: неизвестный тип контента: " + type);
                
                // Используем существующие View в content_details_panel
                final TextView panelTitleTextView = binding.contentDetailsPanel.findViewById(R.id.panel_title);
                final TextView contentTitleTextView = binding.contentDetailsPanel.findViewById(R.id.content_title);
                final TextView contentDescriptionTextView = binding.contentDetailsPanel.findViewById(R.id.content_description);
                
                // Убедимся, что специфичные для других типов контейнеры скрыты
                final WebView webView = binding.contentDetailsPanel.findViewById(R.id.content_web_view);
                final FrameLayout taskViewHostContainer = binding.contentDetailsPanel.findViewById(R.id.task_sheet_host_container);

                if (webView != null) webView.setVisibility(View.GONE);
                if (taskViewHostContainer != null) taskViewHostContainer.setVisibility(View.GONE);
                
                if (panelTitleTextView != null) {
                    panelTitleTextView.setText(title != null ? title : "Детали");
                    panelTitleTextView.setVisibility(View.VISIBLE);
                }
                
                // Для неизвестного типа, используем content_title для заголовка и content_description для описания
                // (Можно решить, нужен ли contentTitleTextView, если panelTitleTextView уже используется)
                if (contentTitleTextView != null) {
                    contentTitleTextView.setText(title); 
                    contentTitleTextView.setVisibility(View.VISIBLE); 
                }
                if (contentDescriptionTextView != null) {
                    contentDescriptionTextView.setText(description != null && !description.isEmpty() ? 
                            description : "Контент этого типа пока не поддерживается.");
                    contentDescriptionTextView.setVisibility(View.VISIBLE);
                }
                
                // Убедимся, что сама панель деталей видима и развернута
                if (binding.contentDetailsPanel.getVisibility() != View.VISIBLE) {
                    binding.contentDetailsPanel.setVisibility(View.VISIBLE);
                }
                contentDetailsSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }
    
    /**
     * Настраивает WebView для отображения контента
     */
    private void configureWebView(WebView webView) {
        // Используем современные API для WebView
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_AUTO);
        }
        
        // Остальные настройки WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        
        // Настройки для лучшей адаптации контента
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        
        // Дополнительные настройки для фиксации проблем с размерами WebView
        // webView.setLayoutParams(new LinearLayout.LayoutParams(
        //     LinearLayout.LayoutParams.MATCH_PARENT, 
        //     dpToPx(500))); // <-- УДАЛЯЕМ ЭТУ СТРОКУ / БЛОК
        
        // Отключаем NestedScrolling для WebView, чтобы избежать конфликтов со ScrollView
        webView.setNestedScrollingEnabled(false);
        
        // Устанавливаем размер текста в зависимости от размера экрана
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        
        // Адаптируем размер текста к ширине экрана
        int textZoom = 200; // Стандартный размер
        if (screenWidth <= 480) {
            textZoom = 190; // Маленький экран - уменьшаем текст
        } else if (screenWidth >= 1200) {
            textZoom = 220; // Большой экран - увеличиваем текст
        }
        webView.getSettings().setTextZoom(textZoom);
        
        // Устанавливаем отступы внутри WebView в 0
        webView.setPadding(0, 0, 0, 0);
        
        // Включаем поддержку отображения в темном режиме
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Определяем текущую тему для WebView
            boolean isWebViewDarkTheme = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("dark_theme", false);
                
            webView.getSettings().setForceDark(
                isWebViewDarkTheme ? 
                android.webkit.WebSettings.FORCE_DARK_ON : 
                android.webkit.WebSettings.FORCE_DARK_OFF
            );
        }
        
        // Сбрасываем отступы у WebView
        if (webView.getLayoutParams() instanceof MarginLayoutParams) {
            MarginLayoutParams params = (MarginLayoutParams) webView.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
            webView.setLayoutParams(params);
        }
        
        // Настраиваем WebView для лучшей обработки событий прокрутки
        webView.setNestedScrollingEnabled(true);
                        
        // Отключаем события перетаскивания во время прокрутки контента 
        webView.setOnTouchListener(new View.OnTouchListener() {
            private float startY;
            private float startX;
            
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                WebView webView = (WebView) v;
                
                // Определяем текущее событие
                int action = event.getAction();
                
                switch (action) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        // Запоминаем координаты начала касания
                        startY = event.getRawY();
                        startX = event.getRawX();
                        Log.d(TAG, "WebView: Начало касания");
                        
                        // Блокируем перетаскивание BottomSheet во время работы с WebView
                        contentDetailsSheetBehavior.setDraggable(false);
                        break;
                        
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        Log.d(TAG, "WebView: Конец касания");
                        
                        // Снова разрешаем перетаскивание после окончания жеста
                        contentDetailsSheetBehavior.setDraggable(true);
                        break;
                        
                    case android.view.MotionEvent.ACTION_MOVE:
                        // Вычисляем смещение
                        float deltaY = event.getRawY() - startY;
                        float deltaX = event.getRawX() - startX;
                        
                        // Определяем, является ли жест вертикальной прокруткой
                        boolean isVerticalScroll = Math.abs(deltaY) > Math.abs(deltaX);
                        
                        if (isVerticalScroll) {
                            // Для вертикальной прокрутки проверяем граничные условия
                            if (webView.getScrollY() == 0 && deltaY > 50) {
                                // Достигли верха и тянем вниз - это может быть жест для закрытия BottomSheet
                                Log.d(TAG, "WebView: Достигли верха и тянем вниз");
                                contentDetailsSheetBehavior.setDraggable(true);
                            } else if (!webView.canScrollVertically(1) && deltaY < -50) {
                                // Достигли дна и тянем вверх - больше не можем прокручивать вниз
                                Log.d(TAG, "WebView: Достигли дна и тянем вверх");
                            } else {
                                // Обычная вертикальная прокрутка
                                contentDetailsSheetBehavior.setDraggable(false);
                            }
                        }
                        break;
                }
                
                // Возвращаем false, чтобы продолжить стандартную обработку WebView
                return false;
            }
        });
    }

    /**
     * Применяет стили к HTML-контенту в зависимости от выбранной темы
     */
    public String applyStylesToHtml(String htmlContent, boolean isDarkTheme) {
        // Добавляем CSS-стили для темной/светлой темы
        String styles = "<style>" +
            "body {" +
            "  padding: 16px;" +
            "  margin: 0;" +
            "  font-family: -apple-system, BlinkMacSystemFont, sans-serif;" +
            "  font-size: 16px;" +
            "  line-height: 1.5;" +
            (isDarkTheme ? 
                "  background-color: #121212;" +
                "  color: #EEEEEE;" 
              : 
                "  background-color: #FFFFFF;" +
                "  color: #212121;"
            ) +
            "}" +
            "img { max-width: 100%; height: auto; }" +
            "h1, h2, h3 { margin-top: 16px; margin-bottom: 8px; }" +
            "p { margin-bottom: 16px; }" +
            
            // Усиленные стили для таблиц с явно видимыми границами
            "table {" +
            "  border-collapse: collapse;" +
            "  border-spacing: 0;" +
            "  width: 100%;" +
            "  margin-bottom: 16px;" +
            "  border: 2px solid " + (isDarkTheme ? "#777777" : "#666666") + ";" +
            "  overflow-x: auto;" +
            "  display: block;" +
            "}" +
            "th, td {" +
            "  text-align: left;" +
            "  padding: 8px;" +
            "  border: 2px solid " + (isDarkTheme ? "#777777" : "#666666") + ";" +
            "}" +
            "th {" +
            "  background-color: " + (isDarkTheme ? "#333333" : "#E0E0E0") + ";" +
            "  font-weight: bold;" +
            "}" +
            "tr:nth-child(even) {" +
            "  background-color: " + (isDarkTheme ? "#222222" : "#F9F9F9") + ";" +
            "}" +
            // Дополнительные стили для различных HTML-таблиц
            "table[border], table[border] td, table[border] th {" +
            "  border: 2px solid " + (isDarkTheme ? "#777777" : "#666666") + " !important;" +
            "}" +
            "table.bordered, table.bordered td, table.bordered th {" +
            "  border: 2px solid " + (isDarkTheme ? "#777777" : "#666666") + " !important;" +
            "}" +
            "</style>";
        
        // Вставляем стили в HTML
        if (htmlContent.contains("<head>")) {
            return htmlContent.replace("<head>", "<head>" + styles);
        } else {
            return "<html><head>" + styles + "</head><body>" + htmlContent + "</body></html>";
        }
    }
    
    /**
     * Конвертирует dp в пиксели
     * @param dp значение в dp
     * @return значение в пикселях
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    
    /**
     * Выход из приложения с очисткой токенов и локального кэша.
     */
    private void logout() {
        // Используем Kotlin-класс LogoutHandler для корректной работы с корутинами
        logoutHandler.performLogout(
            this,                      // контекст
            this,                      // lifecycleOwner 
            googleAuthManager,         // менеджер Google аутентификации
            categoryDataCache          // кэш категорий (передаем как Map)
        );
    }

    /**
     * Обновляет информацию профиля пользователя
     */
    private void updateProfileInfo() {
        TextView nameTextView = findViewById(R.id.profile_name);
        TextView emailTextView = findViewById(R.id.profile_email);
        ImageView profileAvatar = findViewById(R.id.profile_avatar);
        Button logoutButton = findViewById(R.id.btn_logout);
        Button practiceStatsButton = findViewById(R.id.btn_practice_statistics);
        Button supportButton = findViewById(R.id.btn_support);
        
        // Обработчик для кнопки выхода из аккаунта
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }
        
        // Обработчик для кнопки статистики практики
        if (practiceStatsButton != null) {
            practiceStatsButton.setOnClickListener(v -> {
                // Скрываем панель профиля
                hideProfilePanel();
                
                // Скрываем затемнение
//                if (// dimOverlay != null) {
//                    // dimOverlay.animate().alpha(0f).setDuration(200).withEndAction(() ->
//                        // dimOverlay.setVisibility(View.GONE));
//                }
                
                // Переходим к экрану статистики практики
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new com.ruege.mobile.ui.fragment.PracticeStatisticsFragment())
                    .addToBackStack(null)
                    .commit();
                
                Log.d(TAG, "Переход к экрану статистики практики");
            });
        }
        
        // Обработчик для кнопки "Связаться с поддержкой"
        if (supportButton != null) {
            supportButton.setOnClickListener(v -> {
                // Ссылка на Telegram канал поддержки
                String telegramLink = "https://t.me/GuRu_ege_official";
                
                // Создаем Intent для открытия ссылки
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(telegramLink));
                
                // Проверяем, что есть приложение для обработки этого Intent
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    Log.d(TAG, "Переход в Telegram для связи с поддержкой");
                } else {
                    // Если нет приложения для обработки, показываем сообщение
                    Toast.makeText(this, "Пожалуйста, напишите в тг: @GuRu_ege_official. Не удалось открыть Telegram.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Невозможно открыть Telegram: нет приложения для обработки Intent");
                }
            });
        }
        
        // Получаем данные текущего пользователя из ViewModel
        UserEntity currentUser = userViewModel.getCurrentUser().getValue();
        
        // Имя и email пользователя
        if (nameTextView != null) {
            if (currentUser != null && currentUser.getUsername() != null && !currentUser.getUsername().isEmpty()) {
                nameTextView.setText(currentUser.getUsername());
            } else {
                nameTextView.setText("Ученик"); // Заглушка для имени пользователя
            }
        }
        
        if (emailTextView != null) {
            if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                emailTextView.setText(currentUser.getEmail());
            } else {
                emailTextView.setText("Локальный режим"); // Заглушка для email пользователя
            }
        }
        
        // Аватар пользователя
        if (profileAvatar != null) {
            if (currentUser != null && currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                // Загружаем аватар пользователя с помощью Glide
                Glide.with(this)
                    .load(currentUser.getAvatarUrl())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(profileAvatar);
            } else {
                // Устанавливаем иконку по умолчанию
                profileAvatar.setImageResource(R.drawable.ic_profile);
            }
        }
        
        // Инициализация и настройка компонента мониторинга синхронизации
        try {
            View syncStatusCard = findViewById(R.id.sync_status_card);
            if (syncStatusCard != null) {
                // Создаем менеджер статуса синхронизации, если еще не создан
                if (syncStatusManager == null) {
                    syncStatusManager = new SyncStatusManager(this, syncStatusCard, progressViewModel);
                    // Инициализируем наблюдателей
                    syncStatusManager.initialize(this);
                    Log.d(TAG, "Компонент мониторинга синхронизации инициализирован");
                }
            } else {
                Log.e(TAG, "sync_status_card не найден в макете профиля");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации компонента мониторинга синхронизации", e);
        }
    }
    
    /**
     * Очищает содержимое BottomSheet-панели
     */
    private void clearBottomSheetContent() {
        Log.d(TAG, "clearBottomSheetContent: Очищаем содержимое панели деталей контента");
        
        if (binding == null || binding.contentDetailsPanel == null) {
            Log.w(TAG, "clearBottomSheetContent: binding или contentDetailsPanel равны null, очистка невозможна.");
            return;
        }

        // Получаем все основные контейнеры контента в content_details_panel
        final WebView webView = binding.contentDetailsPanel.findViewById(R.id.content_web_view);
        final FrameLayout taskViewHostContainer = binding.contentDetailsPanel.findViewById(R.id.task_sheet_host_container);
        final TextView generalTitleView = binding.contentDetailsPanel.findViewById(R.id.content_title); // Общий заголовок
        final TextView generalDescriptionView = binding.contentDetailsPanel.findViewById(R.id.content_description); // Общее описание
        final ProgressBar loadingIndicator = binding.contentDetailsPanel.findViewById(R.id.content_loading);
        final TextView errorView = binding.contentDetailsPanel.findViewById(R.id.content_error);

        // Скрываем и очищаем WebView
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank"); // Очищаем содержимое
            webView.setVisibility(View.GONE);
            Log.d(TAG, "clearBottomSheetContent: WebView очищен и скрыт.");
        }

        // Скрываем и очищаем контейнер Заданий
        if (taskViewHostContainer != null) {
            taskViewHostContainer.removeAllViews();
            taskViewHostContainer.setVisibility(View.GONE);
            Log.d(TAG, "clearBottomSheetContent: Контейнер заданий очищен и скрыт.");
        }
        
        // Скрываем общие текстовые поля и кнопку действия
        if (generalTitleView != null) generalTitleView.setVisibility(View.GONE);
        if (generalDescriptionView != null) generalDescriptionView.setVisibility(View.GONE);

        // Скрываем индикаторы загрузки и ошибок
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
        if (errorView != null) errorView.setVisibility(View.GONE);

        // Очистка ViewModel (некоторые части специфичны для задач, могут потребовать доработки)
        if (contentViewModel != null) {
            contentViewModel.getTasks().setValue(null); // Очищаем текущие задачи
            contentViewModel.clearContent(); // Общая очистка контента в ViewModel
            contentViewModel.clearTaskCache(null); // Очищаем кеш списка задач
            contentViewModel.clearTaskDetailCache(); // Очищаем кеш деталей задач
            // TODO: Добавить аналогичную очистку ViewModel для теории, если есть свои LiveData
        }

        Log.d(TAG, "clearBottomSheetContent: Содержимое панели и кеши ViewModel очищены.");
    }
    
    /**
     * Публичный метод для отображения BottomSheet с теорией, используется из TheoryFragment
     */
    public void showTheoryBottomSheet(String title, String description, String contentId) {
        showBottomSheet(title, description, contentId, "theory");
    }

//    /**
//     * Отображает фрагмент с заданиями ЕГЭ
//     */
//    private void showExercisesFragment() {
//        // Находим контейнер для фрагмента
//        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
//
//        // Если контейнер не найден, создаем его программно и добавляем в основной контейнер
//        if (fragmentContainer == null) {
//            FrameLayout mainContent = findViewById(R.id.content_main);
//            if (mainContent != null) {
//                fragmentContainer = new FrameLayout(this);
//                fragmentContainer.setId(R.id.fragment_container);
//
//                // Создаем параметры макета, занимающие весь экран
//                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
//                    new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
//                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
//                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT);
//
//                // Устанавливаем привязки ко всем сторонам родительского элемента
//                params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
//                params.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
//                params.leftToLeft = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
//                params.rightToRight = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
//                params.topMargin = dpToPx(60); // Отступ сверху для видимости TabLayout
//
//                fragmentContainer.setLayoutParams(params);
//
//                // Устанавливаем фоновый цвет, чтобы контейнер был виден
//                fragmentContainer.setBackgroundColor(getResources().getColor(android.R.color.background_light, getTheme()));
//
//                // Добавляем контейнер в ConstraintLayout, а не в ScrollView
//                // чтобы избежать проблем с прокруткой
//                mainContent.addView(fragmentContainer);
//                Log.d(TAG, "Создан программный контейнер для фрагмента с полноэкранными параметрами");
//            } else {
//                Log.e(TAG, "Не найден основной контейнер content_main");
//                return;
//            }
//        }
//
//        // Делаем контейнер видимым и выносим наверх иерархии
//        fragmentContainer.setVisibility(View.VISIBLE);
//        fragmentContainer.bringToFront();
//
//        // Начинаем транзакцию фрагментов
//        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//
//        // Проверяем, существует ли уже фрагмент заданий
//        Fragment exercisesFragment = getSupportFragmentManager().findFragmentByTag("exercises_fragment");
//
//        if (exercisesFragment != null) {
//            // Фрагмент уже существует
//            Log.d(TAG, "Используем существующий фрагмент с заданиями");
//
//            // Если фрагмент скрыт, показываем его
//            if (!exercisesFragment.isVisible()) {
//                transaction.show(exercisesFragment);
//                transaction.commit();
//                Log.d(TAG, "Показываем существующий фрагмент заданий");
//            }
//        } else {
//            // Создаем и отображаем новый фрагмент
//            com.ruege.mobile.ui.exercises.ExercisesFragment fragment = new com.ruege.mobile.ui.exercises.ExercisesFragment();
//            transaction.replace(R.id.fragment_container, fragment, "exercises_fragment");
//            transaction.commit();
//            Log.d(TAG, "Добавлен новый фрагмент с заданиями с тегом exercises_fragment");
//        }
//    }
//
//    /**
//     * Отображает фрагмент с теорией
//     */
//    private void showTheoryFragment() {
//        // Находим контейнер для фрагмента
//        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
//
//        // Если контейнер не найден, создаем его программно и добавляем в основной контейнер
//        if (fragmentContainer == null) {
//            FrameLayout mainContent = findViewById(R.id.content_main);
//            if (mainContent != null) {
//                fragmentContainer = new FrameLayout(this);
//                fragmentContainer.setId(R.id.fragment_container);
//
//                // Создаем параметры макета, занимающие весь экран
//                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
//                    new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
//                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
//                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT);
//
//                // Устанавливаем привязки ко всем сторонам родительского элемента
//                params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
//                params.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
//                params.leftToLeft = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
//                params.rightToRight = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
//                params.topMargin = dpToPx(60); // Отступ сверху для видимости TabLayout
//
//                fragmentContainer.setLayoutParams(params);
//
//                // Устанавливаем фоновый цвет, чтобы контейнер был виден
//                fragmentContainer.setBackgroundColor(getResources().getColor(android.R.color.background_light, getTheme()));
//
//                // Добавляем контейнер в ConstraintLayout, а не в ScrollView
//                // чтобы избежать проблем с прокруткой
//                mainContent.addView(fragmentContainer);
//                Log.d(TAG, "Создан программный контейнер для фрагмента с полноэкранными параметрами");
//            } else {
//                Log.e(TAG, "Не найден основной контейнер content_main");
//                return;
//            }
//        }
//
//        // Делаем контейнер видимым и выносим наверх иерархии
//        fragmentContainer.setVisibility(View.VISIBLE);
//        fragmentContainer.bringToFront();
//
//        // Начинаем транзакцию фрагментов
//        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//
//        // Проверяем, существует ли уже фрагмент теории
//        Fragment theoryFragment = getSupportFragmentManager().findFragmentByTag("theory_fragment");
//
//        if (theoryFragment != null) {
//            // Фрагмент уже существует
//            Log.d(TAG, "Используем существующий фрагмент с теорией");
//
//            // Если фрагмент скрыт, показываем его
//            if (!theoryFragment.isVisible()) {
//                transaction.show(theoryFragment);
//                transaction.commit();
//                Log.d(TAG, "Показываем существующий фрагмент теории");
//            }
//        } else {
//            // Создаем и отображаем новый фрагмент
//            com.ruege.mobile.ui.theory.TheoryFragment fragment = new com.ruege.mobile.ui.theory.TheoryFragment();
//            transaction.replace(R.id.fragment_container, fragment, "theory_fragment");
//            transaction.commit();
//            Log.d(TAG, "Добавлен новый фрагмент с теорией с тегом theory_fragment");
//        }
//    }

    /**
     * Скрывает все фрагменты и контейнер фрагментов
     */
    private void hideAllFragments() {
        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.GONE);
            Log.d(TAG, "Контейнер фрагмента скрыт");
        }
    }

    /**
     * Показывает bottomSheet специально для новостей, без попытки загрузить контент по ID
     */
    private void showNewsBottomSheet(String title, String description, String additionalInfo) {
        // Настраиваем содержимое панели
        TextView titleTextView = binding.contentDetailsPanel.findViewById(R.id.content_title);
        TextView descriptionTextView = binding.contentDetailsPanel.findViewById(R.id.content_description);
        TextView errorView = binding.contentDetailsPanel.findViewById(R.id.content_error);
        WebView webView = binding.contentDetailsPanel.findViewById(R.id.content_web_view);
        ProgressBar loadingIndicator = binding.contentDetailsPanel.findViewById(R.id.content_loading);
        TextView panelTitleView = binding.contentDetailsPanel.findViewById(R.id.panel_title);

        // Устанавливаем заголовок панели
        if (panelTitleView != null) {
            panelTitleView.setText(title);
            panelTitleView.setVisibility(View.VISIBLE);
        }
        
        // Скрываем дублирующийся заголовок в содержимом
        if (titleTextView != null) {
            titleTextView.setVisibility(View.GONE);
        }
        
        // Устанавливаем описание как основной текст
        descriptionTextView.setText(description);
        descriptionTextView.setVisibility(View.VISIBLE); // <<< ДОБАВЛЕНО
        
        // Скрываем ненужные элементы
        loadingIndicator.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        
//        // Явно скрываем кнопку действия
//        if (actionButton != null) {
//            actionButton.setVisibility(View.GONE);
//        }
        
        // Показываем панель
        binding.contentDetailsPanel.setVisibility(View.VISIBLE);
        
        // Настраиваем поведение BottomSheet
        contentDetailsSheetBehavior.setSkipCollapsed(true);
        contentDetailsSheetBehavior.setHideable(true);
        contentDetailsSheetBehavior.setDraggable(true); // Разрешаем перетаскивание
        contentDetailsSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        
        // Скрываем кнопку закрытия
        ImageView closeButton = binding.contentDetailsPanel.findViewById(R.id.close_panel);
        if (closeButton != null) {
            closeButton.setVisibility(View.GONE);
        }
    }

    /**
     * Обновляет аватар пользователя в главном окне
     */
    private void updateUserAvatar(UserEntity user) {
        ImageView userAvatar = binding.userAvatar;
        if (userAvatar != null) {
            if (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                // Загружаем аватар пользователя с помощью Glide
                Glide.with(this)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(userAvatar);
            } else {
                // Используем иконку по умолчанию
                userAvatar.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    /**
     * Вызывается при приостановке активности (когда активность больше не видна пользователю).
     * Запускаем синхронизацию прогресса с сервером.
     */
    @Override
    protected void onPause() {
        super.onPause();
        
        Log.d(TAG, "onPause: запускаем синхронизацию прогресса");
        startExitSync();
    }
    
    /**
     * Вызывается когда активность становится невидимой пользователю.
     * Также запускаем синхронизацию для повышения надежности.
     */
    @Override
    protected void onStop() {
        super.onStop();
        
        Log.d(TAG, "onStop: запускаем синхронизацию прогресса");
        startExitSync();
    }

    /**
     * Вызывается при уничтожении активности.
     * Запускаем синхронизацию прогресса с сервером.
     */
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: запускаем синхронизацию прогресса");
        startExitSync();
        
        super.onDestroy();
    }
    
    /**
     * Запускает синхронизацию при выходе из приложения.
     * Использует batch-запросы для эффективной синхронизации
     * всех пендинг-данных перед закрытием приложения.
     */
    private void startExitSync() {
        Log.d(TAG, "startExitSync: Запуск синхронизации при выходе");
        
        try {
            // Для надежности запускаем оба метода синхронизации
            
            // 1. Синхронизация через репозиторий с флагом isAppClosing=true
            if (progressSyncRepository != null) {
                progressSyncRepository.syncNow(true, true); // второй параметр означает isAppClosing=true
                Log.d(TAG, "startExitSync: Запущена batch-синхронизация через репозиторий");
            }
            
            // 2. Синхронизация через Worker с флагом isExitSync=true
            com.ruege.mobile.worker.ProgressSyncWorker.startOneTimeSync(
                this.getApplicationContext(), // context
                true,  // expedited = true - высокий приоритет
                true   // isExitSync = true - режим выхода
            );
            Log.d(TAG, "startExitSync: Запущена batch-синхронизация через Worker");
            
            Log.d(TAG, "startExitSync: Синхронизация при выходе запущена успешно");
        } catch (Exception e) {
            Log.e(TAG, "startExitSync: Ошибка при запуске синхронизации", e);
        }
    }

    /**
     * Показывает BottomSheet с шпаргалкой в виде PDF
     */
    private void showShpargalkaBottomSheet(String title, String description, String contentId) {
        Log.d(TAG, "showShpargalkaBottomSheet: Показываем шпаргалку с ID: " + contentId);
        
        // Создаем BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        
        // Инфлейтим custom view
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_shpargalka, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        
        // Инициализируем View из макета
        TextView tvTitle = bottomSheetView.findViewById(R.id.tv_shpargalka_title);
        PDFView pdfView = bottomSheetView.findViewById(R.id.pdf_view);
        Button btnDownloadPdf = bottomSheetView.findViewById(R.id.btn_download_pdf);
        TextView tvErrorMessage = bottomSheetView.findViewById(R.id.tv_error_message);
        ProgressBar progressBar = bottomSheetView.findViewById(R.id.progress_bar);
        
        // Устанавливаем заголовок
        tvTitle.setText(title);
        
        // Скрываем PDFView, так как он больше не используется для отображения
        if (pdfView != null) {
            pdfView.setVisibility(View.GONE);
        }
        
        // Кнопка "Скачать" должна быть всегда видна
        if (btnDownloadPdf != null) {
            btnDownloadPdf.setVisibility(View.VISIBLE);
        }
        
        // Скрываем сообщение об ошибке и индикатор загрузки по умолчанию
        tvErrorMessage.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        
        // Получаем ID шпаргалки из contentId
        String[] parts = contentId.split("_");
        if (parts.length != 2 || !parts[0].equals("shpargalka")) {
            Log.e(TAG, "Некорректный ID шпаргалки: " + contentId);
            tvErrorMessage.setText("Некорректный ID шпаргалки");
            tvErrorMessage.setVisibility(View.VISIBLE);
            return;
        }
        
        int pdfId;
        try {
            pdfId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Ошибка при парсинге ID шпаргалки: " + parts[1], e);
            tvErrorMessage.setText("Неверный формат ID шпаргалки");
            tvErrorMessage.setVisibility(View.VISIBLE);
            return;
        }
        
        // Получаем ViewModel для работы со шпаргалками
        ShpargalkaViewModel shpargalkaViewModel = new ViewModelProvider(this).get(ShpargalkaViewModel.class);
        
        // Убираем наблюдателей LiveData, связанных с PDFView, так как он не используется
        // shpargalkaViewModel.getPdfLoadingStatus().observe(...);
        // shpargalkaViewModel.getPdfLoadError().observe(...);
        // shpargalkaViewModel.currentPdfFile().observe(...);

        if (btnDownloadPdf != null) {
            btnDownloadPdf.setOnClickListener(v -> {
                Log.d(TAG, "Нажата кнопка скачать для PDF ID: " + pdfId);
                
                // Деактивируем кнопку и показываем прогресс бар
                btnDownloadPdf.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                tvErrorMessage.setVisibility(View.GONE); // Скрываем предыдущие ошибки
                
                // Скачиваем файл во внутреннее хранилище
                shpargalkaViewModel.downloadPdf(pdfId, new kotlin.jvm.functions.Function1<Boolean, kotlin.Unit>() {
                    @Override
                    public kotlin.Unit invoke(Boolean success) {
                        // Скрываем прогресс бар и активируем кнопку
                        progressBar.setVisibility(View.GONE);
                        btnDownloadPdf.setEnabled(true);
                        
                        if (success) {
                            Log.d(TAG, "PDF ID: " + pdfId + " успешно скачан во внутреннее хранилище.");
                            File localPdfFile = shpargalkaViewModel.getLocalPdfFile(pdfId);
                            if (localPdfFile != null && localPdfFile.exists()) {
                                Log.d(TAG, "Локальный файл найден: " + localPdfFile.getAbsolutePath());
                                // Копируем файл в загрузки с названием из title
                                String sanitizedFileName = title.replaceAll("[\\/:*?\"<>|]", "_") + ".pdf";
                                downloadPdfToDownloads(localPdfFile, sanitizedFileName, title); // Используем оригинальный title для описания
                            } else {
                                Log.e(TAG, "Локальный PDF файл не найден после успешного скачивания. ID: " + pdfId);
                                tvErrorMessage.setText("Ошибка: Файл скачан, но не найден локально.");
                                tvErrorMessage.setVisibility(View.VISIBLE);
                                Toast.makeText(MainActivity.this, "Ошибка при подготовке файла к сохранению", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Не удалось скачать PDF ID: " + pdfId + " во внутреннее хранилище.");
                            tvErrorMessage.setText("Не удалось скачать PDF. Проверьте интернет или попробуйте позже.");
                            tvErrorMessage.setVisibility(View.VISIBLE);
                            Toast.makeText(MainActivity.this, "Ошибка при скачивании файла", Toast.LENGTH_SHORT).show();
                        }
                        return kotlin.Unit.INSTANCE;
                    }
                });
            });
        }
        
        // Убираем автоматическую загрузку PDF, так как PDFView не используется
        // final boolean isPdfDownloaded = shpargalkaViewModel.isPdfDownloaded(pdfId);
        // if (!isPdfDownloaded) {
        //     shpargalkaViewModel.loadShpargalkaPdf(pdfId);
        // }
        
        // Показываем BottomSheet
        bottomSheetDialog.show();
    }

    /**
     * Сохраняет файл в директорию загрузок устройства
     * @param sourceFile исходный файл
     * @param fileName имя файла в директории загрузок
     * @param description описание файла
     */
    private void downloadPdfToDownloads(File sourceFile, String fileName, String description) {
        try {
            Log.d(TAG, "Начало копирования PDF в загрузки: " + sourceFile.getAbsolutePath());
            
            // Проверяем разрешения для устройств Android 10 и выше
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // На Android 10+ используем MediaStore API, не требующее разрешения на запись
                copyPdfToDownloads(sourceFile, fileName, description);
            } else {
                // Для Android 9 и ниже требуется проверка разрешения WRITE_EXTERNAL_STORAGE
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Запрашиваем разрешение на запись во внешнее хранилище");
                    
                    // Сохраняем информацию о файле для использования после получения разрешения
                    pendingSourceFile = sourceFile;
                    pendingFileName = fileName;
                    pendingDescription = description;
                    
                    // Запрашиваем разрешение
                    requestPermissions(
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_STORAGE
                    );
                    
                    return; // Выходим из метода пока не получено разрешение
                } else {
                    // Разрешение уже есть, можно продолжать
                    copyPdfToDownloads(sourceFile, fileName, description);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке разрешений", e);
            Toast.makeText(this, "Ошибка при сохранении файла: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // Выделяем фактическое копирование в отдельный метод
    private void copyPdfToDownloads(File sourceFile, String fileName, String description) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Для Android 10+ используем MediaStore API
                saveUsingMediaStore(sourceFile, fileName, description);
            } else {
                // Для более ранних версий используем прямое копирование в Downloads
                saveUsingDirectFile(sourceFile, fileName, description);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при копировании файла в загрузки", e);
            Toast.makeText(this, "Ошибка при сохранении файла: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Метод для сохранения с использованием MediaStore API (Android 10+)
    @android.annotation.TargetApi(android.os.Build.VERSION_CODES.Q)
    private void saveUsingMediaStore(File sourceFile, String fileName, String description) {
        try {
            Log.d(TAG, "Сохранение файла используя MediaStore API: " + fileName);
            
            // Создаем ContentValues для вставки в MediaStore
            android.content.ContentValues contentValues = new android.content.ContentValues();
            contentValues.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
            contentValues.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf");
            contentValues.put(android.provider.MediaStore.Downloads.TITLE, fileName);
            // удаляем поле DESCRIPTION, которое не существует в MediaStore.Downloads
            contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 1);
            
            // Вставляем запись в MediaStore и получаем URI
            android.content.ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            
            if (uri != null) {
                // Открываем выходной поток для записи данных
                try (OutputStream outputStream = resolver.openOutputStream(uri);
                     FileInputStream inputStream = new FileInputStream(sourceFile)) {
                    
                    // Проверяем, открылись ли потоки успешно
                    if (outputStream == null) {
                        Log.e(TAG, "Не удалось открыть выходной поток для URI: " + uri);
                        return;
                    }
                    
                    // Копируем данные
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    
                    // Сбрасываем флаг IS_PENDING
                    contentValues.clear();
                    contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0);
                    resolver.update(uri, contentValues, null, null);
                    
                    Log.d(TAG, "Файл успешно сохранен через MediaStore: " + uri);
                    Toast.makeText(this, "Файл сохранен в загрузки", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Не удалось получить URI для сохранения файла");
                Toast.makeText(this, "Ошибка: Не удалось получить URI для сохранения", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сохранении файла через MediaStore", e);
            Toast.makeText(this, "Ошибка при сохранении файла: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // Метод для сохранения с использованием прямого файлового доступа (до Android 10)
    private void saveUsingDirectFile(File sourceFile, String fileName, String description) {
        try {
            // Получаем директорию загрузок
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                boolean created = downloadsDir.mkdirs();
                Log.d(TAG, "Создана директория загрузок: " + created);
            }
            
            // Создаем файл назначения
            File destinationFile = new File(downloadsDir, fileName);
            
            // Если файл уже существует, удаляем его
            if (destinationFile.exists()) {
                boolean deleted = destinationFile.delete();
                Log.d(TAG, "Удаление существующего файла: " + deleted);
            }
            
            // Проверяем, существует ли исходный файл
            if (!sourceFile.exists()) {
                Log.e(TAG, "Исходный файл не существует: " + sourceFile.getAbsolutePath());
                Toast.makeText(this, "Ошибка: файл не найден", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d(TAG, "Начало копирования файла из " + sourceFile.getAbsolutePath() + 
                       " в " + destinationFile.getAbsolutePath());
            
            // Копируем файл
            FileInputStream inputStream = new FileInputStream(sourceFile);
            FileOutputStream outputStream = new FileOutputStream(destinationFile);
            
            byte[] buffer = new byte[1024];
            int length;
            long totalCopied = 0;
            long fileSize = sourceFile.length();
            
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
                totalCopied += length;
                
                // Логируем прогресс для больших файлов
                if (fileSize > 0 && totalCopied % 1024000 < 1024) { // Каждый мегабайт
                    int progress = (int) (totalCopied * 100 / fileSize);
                    Log.d(TAG, "Прогресс копирования: " + progress + "%");
                }
            }
            
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            
            Log.d(TAG, "Файл успешно скопирован в загрузки: " + destinationFile.getAbsolutePath());
            
            // Сканируем файл, чтобы он был доступен в галерее
            MediaScannerConnection.scanFile(
                this,
                new String[]{destinationFile.getAbsolutePath()},
                new String[]{"application/pdf"},
                (path, uri) -> {
                    Log.d(TAG, "Файл отсканирован и доступен в системе: " + uri);
                    Toast.makeText(this, "Файл сохранен в загрузки", Toast.LENGTH_SHORT).show();
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при копировании файла через прямой файловый доступ", e);
            Toast.makeText(this, "Ошибка при сохранении файла: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Отображает кэшированные варианты или состояние загрузки/ошибки.
     * Этот метод вызывается, когда выбрана вкладка "Варианты" или когда обновляются данные вариантов.
     */
    private void displayCachedVariants() {
        Log.d(TAG, "Отображение вариантов (displayCachedVariants)");
        FrameLayout contentContainer = binding.contentContainer; // Используем contentContainer
        if (contentContainer == null) {
            Log.e(TAG, "contentContainer не найден в макете!");
            return;
        }
        contentContainer.removeAllViews(); // Очищаем предыдущее содержимое

        Resource<List<VariantEntity>> currentResource = variantViewModel.getVariantsLiveData().getValue();

        if (currentResource instanceof Resource.Loading) {
            Log.d(TAG, "displayCachedVariants: Загрузка списка вариантов...");
            showShimmer(shimmerContentLayout, contentContainer, errorPlaceholderContent);
        } else if (currentResource instanceof Resource.Success) {
            List<VariantEntity> variants = ((Resource.Success<List<VariantEntity>>) currentResource).data; 
            if (variants != null && !variants.isEmpty()) {
                Log.d(TAG, "displayCachedVariants: Варианты успешно загружены: " + variants.size() + " шт.");
                
                RecyclerView recyclerView = new RecyclerView(this);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                recyclerView.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
                recyclerView.setClipToPadding(false);

                VariantAdapter.OnVariantClickListener listener = variant -> {
                    Log.d(TAG, "Клик на вариант: " + variant.getName() + " (ID: " + variant.getVariantId() + ")");
                    Toast.makeText(MainActivity.this, "Загрузка варианта: " + variant.getName(), Toast.LENGTH_SHORT).show();
                    variantViewModel.fetchVariantDetails(variant.getVariantId());
                    variantViewModel.updateVariantLastAccessedTime(variant.getVariantId());
                };

                VariantAdapter adapter = new VariantAdapter(listener); // Конструктор принимает только listener
                recyclerView.setAdapter(adapter);
                // Поскольку VariantAdapter наследует ListAdapter, используем submitList
                adapter.submitList(variants);

                TextView headerView = new TextView(this);
                headerView.setText("Варианты");
                headerView.setTextSize(20);
                headerView.setTypeface(headerView.getTypeface(), android.graphics.Typeface.BOLD);
                headerView.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(8));
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
                headerView.setTextColor(ContextCompat.getColor(this, typedValue.resourceId));

                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                linearLayout.addView(headerView);
                linearLayout.addView(recyclerView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                
                contentContainer.addView(linearLayout);
                showData(shimmerContentLayout, contentContainer, errorPlaceholderContent);

            } else {
                Log.d(TAG, "displayCachedVariants: Список вариантов пуст или null.");
                TextView emptyView = new TextView(this);
                emptyView.setText("Нет доступных вариантов для отображения.");
                emptyView.setGravity(Gravity.CENTER);
                emptyView.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
                contentContainer.addView(emptyView);
                showData(shimmerContentLayout, contentContainer, errorPlaceholderContent); 
            }
        } else if (currentResource instanceof Resource.Error) {
            Log.e(TAG, "displayCachedVariants: Ошибка загрузки списка вариантов: " + ((Resource.Error<List<VariantEntity>>) currentResource).message);
            TextView errorView = new TextView(this);
            errorView.setText("Ошибка загрузки списка вариантов. Попробуйте позже.");
            errorView.setGravity(Gravity.CENTER);
            errorView.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            if (errorPlaceholderContent != null && errorPlaceholderContent instanceof LinearLayout) {
                ((LinearLayout) errorPlaceholderContent).removeAllViews();
                TextView errorMsg = new TextView(this);
                errorMsg.setText("Ошибка загрузки списка вариантов. Проверьте подключение к интернету и попробуйте снова.");
                errorMsg.setGravity(Gravity.CENTER);
                errorMsg.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                ((LinearLayout) errorPlaceholderContent).addView(errorMsg);
            }
            contentContainer.addView(errorView); 
            showError(shimmerContentLayout, contentContainer, errorPlaceholderContent);
        } else {
             Log.d(TAG, "displayCachedVariants: currentResource is null or unknown. Показываем шиммер.");
             showShimmer(shimmerContentLayout, contentContainer, errorPlaceholderContent);
        }
    }

    public void initiateLogout() {
        Log.d(TAG, "Logout initiated from ProfileBottomSheet");
        // Убедитесь, что googleAuthManager инициализирован
        if (googleAuthManager == null) {
            // Попытка инициализации, если это возможно здесь, или выброс ошибки
            // Например, если он создается в onCreate:
            // googleAuthManager = new GoogleAuthManager(this, 서버_CLIENT_ID); 
            // Если его нельзя здесь создать, нужно пересмотреть, как передавать его в logoutHandler
             Log.e(TAG, "GoogleAuthManager is not initialized in MainActivity. Cannot perform logout.");
             Toast.makeText(this, "Ошибка: Менеджер авторизации не инициализирован.", Toast.LENGTH_SHORT).show();
             return;
        }

        // logoutHandler должен быть инжектирован или получен иначе
        if (logoutHandler != null) {
             logoutHandler.performLogout(
                this, // context
                this, // lifecycleOwner
                googleAuthManager,
                categoryDataCache // Ваш кэш категорий
            );
        } else {
            Log.e(TAG, "LogoutHandler is null. Cannot perform logout.");
            Toast.makeText(this, "Ошибка: Обработчик выхода не инициализирован.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Возвращает текущее состояние темы (темная/светлая)
     */
    public boolean isDarkTheme() {
        return getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("dark_theme", false);
    }

    public void showEssayBottomSheet(String title, String description, String contentId) {
        Log.d(TAG, "showEssayBottomSheet: Показываем сочинение с ID: " + contentId + " и заголовком: " + title);
        EssayBottomSheetDialogFragment essaySheet = EssayBottomSheetDialogFragment.newInstance(contentId, title);
        essaySheet.show(getSupportFragmentManager(), EssayBottomSheetDialogFragment.TAG_ESSAY_BS);
    }

}