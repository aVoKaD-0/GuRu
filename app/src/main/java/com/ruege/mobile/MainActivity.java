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
import android.media.MediaScannerConnection;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import com.ruege.mobile.ui.bottomsheet.VariantDetailBottomSheetDialogFragment;
import com.ruege.mobile.ui.bottomsheet.ProfileBottomSheetDialogFragment;
import com.ruege.mobile.ui.bottomsheet.TheoryBottomSheetDialogFragment;
import com.ruege.mobile.ui.bottomsheet.TaskDisplayBottomSheetDialogFragment;
import com.ruege.mobile.ui.bottomsheet.EssayBottomSheetDialogFragment;
import com.ruege.mobile.ui.bottomsheet.NewsBottomSheetDialogFragment;
import com.ruege.mobile.ui.bottomsheet.ShpargalkaBottomSheetDialogFragment;

import androidx.lifecycle.LifecycleOwnerKt;
import com.ruege.mobile.util.FlowJavaHelper;

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
        NewsBottomSheetDialogFragment newsSheet = NewsBottomSheetDialogFragment.newInstance(
            newsItem.getTitle(),
            newsItem.getDescription(),
            "Дата публикации: " + newsItem.getDate(),
            newsItem.getImageUrl()
        );
        if (getSupportFragmentManager().findFragmentByTag(NewsBottomSheetDialogFragment.TAG_NEWS_BS) == null) {
            newsSheet.show(getSupportFragmentManager(), NewsBottomSheetDialogFragment.TAG_NEWS_BS);
        }
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

    @Inject
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
            variantViewModel = new ViewModelProvider(this).get(VariantViewModel.class);

            setupAdaptersAndObservers();

            initializePanels();

            contentViewModel.loadInitialContent();

            if (contentRepository != null) {
                FlowJavaHelper.collectInScope(
                    contentRepository.getInitialContentLoaded(),
                    LifecycleOwnerKt.getLifecycleScope(this),
                    contentLoaded -> { 
                        if (Boolean.TRUE.equals(contentLoaded)) {
                            Log.d(TAG, "Весь основной контент загружен. Инициализируем прогресс через ProgressViewModel.");
                            if (progressViewModel != null) {
                                progressViewModel.checkAndInitializeProgressAndLoad();
                            }
                        }
                    }
                );
            }

            handler.postDelayed(() -> {
                try {
                    newsViewModel.loadLatestNews(7);
                    Log.d(TAG, "Запрос последних новостей выполнен после задержки");
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при загрузке новостей после задержки", e);
                }
            }, 2000);

            Log.d(TAG, "MainActivity setup complete");

            ImageView userAvatar = binding.userAvatar;
            if (userAvatar != null) {
                userAvatar.setOnClickListener(v -> showProfilePanel());
            }

            userViewModel.getFirstUser();
            userViewModel.getCurrentUser().observe(this, user -> {
                updateUserAvatar(user);
            });
            Log.d(TAG, "onCreate: Запускаем начальную загрузку вариантов");
            variantViewModel.fetchVariants();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при запуске приложения", e);

            if (e.getMessage() != null && e.getMessage().contains("duplicate column name: google_id")) {
                showDatabaseResetDialog();
            } else {
                Toast.makeText(this, "Произошла ошибка при запуске приложения", Toast.LENGTH_LONG).show();
            }
        }
        Log.d(TAG, "Инициализация прогресса пользователя при запуске... MainActivity onCreate - этот вызов будет удален");
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
                resetDatabase();
            })
            .setNegativeButton("Выход", (dialog, which) -> {
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

            Intent intent = getIntent();
            finish();
            startActivity(intent);
        } catch (Exception e) {
            Log.e("MainActivity", "Error while resetting database", e);
            new AlertDialog.Builder(this)
                .setTitle("Ошибка базы данных")
                .setMessage("Произошла критическая ошибка при обновлении базы данных. Рекомендуется удалить данные приложения в настройках.")
                .setPositiveButton("ОК", null)
                .show();
        }
    }

    /**
     * Инициализирует все панели и BottomSheet-ы.
     */
    private void initializePanels() {
        Log.d(TAG, "Инициализация панелей");
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

                }
            });

            ImageView closePanelButton = contentDetailsPanel.findViewById(R.id.close_panel);
            if (closePanelButton != null) {
                closePanelButton.setVisibility(View.GONE);
            }
        }

    }

    private void setupUI() {
        View scrollContentView = binding.contentMain.findViewById(R.id.scroll_content);

        shimmerProgressLayout = scrollContentView.findViewById(R.id.shimmer_progress);
        shimmerNewsLayout = scrollContentView.findViewById(R.id.shimmer_news);
        shimmerContentLayout = scrollContentView.findViewById(R.id.shimmer_content);
        errorPlaceholderProgress = scrollContentView.findViewById(R.id.error_placeholder_progress);
        errorPlaceholderNews = scrollContentView.findViewById(R.id.error_placeholder_news);
        errorPlaceholderContent = scrollContentView.findViewById(R.id.error_placeholder_content);
        progressRecycler = scrollContentView.findViewById(R.id.recycler_progress);
        newsRecycler = scrollContentView.findViewById(R.id.recycler_news);
        ImageView userAvatar = binding.getRoot().findViewById(R.id.user_avatar);

        if (userAvatar != null) {
            userAvatar.setClickable(true);
            userAvatar.setFocusable(true);

            userAvatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Avatar clicked, showing profile panel");
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    showProfilePanel();
                }
            });

            userAvatar.setOnTouchListener((v, event) -> {
                Log.d(TAG, "Avatar touch event: " + event.getAction());
                return false;
            });

            Log.d(TAG, "Avatar click listener set up successfully");
        } else {
            Log.e(TAG, "Cannot find user avatar in binding!");
        }

        setupTabLayout();
    }

    private void setupTabLayout() {
        TabLayout tabLayout = binding.contentMain.findViewById(R.id.tab_navigation);

        tabLayout.removeAllTabs();

        for (int i = 0; i < categoryTitles.length; i++) {
            String title = categoryTitles[i];

            View customView = getLayoutInflater().inflate(R.layout.custom_tab, null);
            TextView textView = customView.findViewById(R.id.tab_text);
            textView.setText(title);
            textView.setContentDescription(title + " - раздел");

            TabLayout.Tab tab = tabLayout.newTab();
            tab.setCustomView(customView);
            tabLayout.addTab(tab);
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                String categoryId = categories[position];
                Log.d(TAG, "Выбрана категория: " + categoryId);

                FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
                FrameLayout contentContainer = binding.contentContainer;
                androidx.core.widget.NestedScrollView scrollContent = binding.contentMain.findViewById(R.id.scroll_content);

                if (contentContainer != null) {
                    contentContainer.removeAllViews();
                    Log.d(TAG, "Контейнер content_container очищен от прежнего содержимого");
                }

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

                if ("theory".equals(categoryId)) {
                    Log.d(TAG, "Переход к теории");

                    displayCachedContent(categoryId);
                }
                else if ("task".equals(categoryId)) {
                    Log.d(TAG, "Переход к заданиям");

                    displayCachedContent(categoryId);
                } else if ("shpargalka".equals(categoryId)) {
                    Log.d(TAG, "Переход к шпаргалкам");

                    displayCachedContent(categoryId);

                    if (!categoryDataCache.containsKey(categoryId) || categoryDataCache.get(categoryId).isEmpty()) {
                        ShpargalkaViewModel shpargalkaViewModel = new ViewModelProvider(MainActivity.this).get(ShpargalkaViewModel.class);
                        shpargalkaViewModel.loadShpargalkaItems();
                        Log.d(TAG, "Запущена загрузка шпаргалок, так как кэш пуст");
                    } else {
                        ShpargalkaViewModel shpargalkaViewModel = new ViewModelProvider(MainActivity.this).get(ShpargalkaViewModel.class);
                        shpargalkaViewModel.refreshShpargalkaData();
                        Log.d(TAG, "Запущено принудительное обновление шпаргалок");
                    }
                } else if ("variant".equals(categoryId)) { 
                    Log.d(TAG, "Переход к вариантам");
                    displayCachedVariants(); 

                    if (!variantDataCache.containsKey("variants") || variantDataCache.get("variants").isEmpty()) {
                        variantViewModel.fetchVariants(); 
                        Log.d(TAG, "Запущена загрузка вариантов, так как кэш пуст или не содержит ключ \'variants\'");
                    }

                } else {
                    displayCachedContent(categoryId);
                    Log.d(TAG, "Отображено содержимое для категории: " + categoryId);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupAdaptersAndObservers() {
        progressAdapter = new ProgressAdapter(new ArrayList<>(), onProgressClickListener);
        progressRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        progressRecycler.setAdapter(progressAdapter);
        progressRecycler.setItemAnimator(new SlowItemAnimator());
        try {
            final LayoutAnimationController controller =
                    AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fade_bottom);
            progressRecycler.setLayoutAnimation(controller);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Layout animation resource not found for progressRecycler", e);
        }

        newsAdapter = new NewsAdapter(new ArrayList<>(), onNewsClickListener);
        newsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        newsRecycler.setAdapter(newsAdapter);
        newsRecycler.setItemAnimator(new SlowItemAnimator());
        try {
            final LayoutAnimationController controller =
                    AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fade_bottom);
            newsRecycler.setLayoutAnimation(controller);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Layout animation resource not found for newsRecycler", e);
        }
        showShimmer(shimmerProgressLayout, progressRecycler, errorPlaceholderProgress);
        showShimmer(shimmerNewsLayout, newsRecycler, errorPlaceholderNews);
        showShimmer(shimmerContentLayout, binding.contentContainer, errorPlaceholderContent);
        showShimmer(shimmerVariantLayout, variantRecycler, errorPlaceholderVariant); 

        progressViewModel.getUserProgress().observe(this, progressEntities -> {
            updateProgressUI(progressEntities);
        });

        newsViewModel.getNewsLiveData().observe(this, newsEntities -> {
            updateNewsUI(newsEntities);
        });

        contentViewModel.getTheoryTopicsLiveData().observe(this, theoryTopics -> {
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

            categoryDataCache.put("theory", contentItems);

            TabLayout tabLayout = binding.contentMain.findViewById(R.id.tab_navigation);
            if (tabLayout.getSelectedTabPosition() == 0) {
                displayCachedContent("theory");
            }

            showData(shimmerContentLayout, binding.contentContainer, errorPlaceholderContent);
        });

        contentViewModel.getTasksTopicsLiveData().observe(this, taskGroups -> {
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

            categoryDataCache.put("task", taskItems);
            Log.d(TAG, "Закэшировано " + taskItems.size() + " элементов заданий");

            TabLayout tabLayout = binding.contentMain.findViewById(R.id.tab_navigation);
            if (tabLayout.getSelectedTabPosition() == 1) { 
                Log.d(TAG, "Обновляем UI для заданий, так как выбрана вкладка заданий");
                displayCachedContent("task");
            }
        });

        ShpargalkaViewModel shpargalkaViewModel = new ViewModelProvider(this).get(ShpargalkaViewModel.class);

        shpargalkaViewModel.loadShpargalkaItems();

        shpargalkaViewModel.getShpargalkaContents().observe(this, shpargalkaContents -> {
            List<ContentItem> shpargalkaItems = new ArrayList<>(shpargalkaContents);

            categoryDataCache.put("shpargalka", shpargalkaItems);
            Log.d(TAG, "Закэшировано " + shpargalkaItems.size() + " элементов шпаргалок");

            TabLayout tabLayout = binding.contentMain.findViewById(R.id.tab_navigation);
            if (tabLayout.getSelectedTabPosition() == 2) { 
                Log.d(TAG, "Обновляем UI для шпаргалок, так как выбрана вкладка шпаргалок");
                displayCachedContent("shpargalka");
            }
        });

        variantViewModel.getVariantsLiveData().observe(this, resource -> {
            if (resource != null) {
                TabLayout tabLayout = binding.contentMain.findViewById(R.id.tab_navigation);
                boolean isVariantTabSelected = tabLayout.getSelectedTabPosition() == 3; 

                if (resource instanceof Resource.Success) {
                    Log.d(TAG, "Варианты успешно загружены: " + (resource.data != null ? resource.data.size() : 0) + " шт.");
                    if (resource.data != null) {
                        variantDataCache.put("variants", resource.data);
                        if (isVariantTabSelected) {
                            displayCachedVariants();
                        }
                    } else {
                        variantDataCache.put("variants", new ArrayList<>()); 
                         if (isVariantTabSelected) {
                            displayCachedVariants(); 
                        }
                    }
                } else if (resource instanceof Resource.Error) {
                    Log.e(TAG, "Ошибка загрузки вариантов: " + resource.message);
                    variantDataCache.put("variants", new ArrayList<>()); 
                    if (isVariantTabSelected) {
                        displayCachedVariants(); 
                    }
                } else if (resource instanceof Resource.Loading) {
                    Log.d(TAG, "Загрузка вариантов...");
                    if (isVariantTabSelected && (!variantDataCache.containsKey("variants") || variantDataCache.get("variants").isEmpty())) {
                         displayCachedVariants();
                    }
                }
            }
        });

        variantViewModel.getVariantDetailsLiveData().observe(this, resource -> {
            if (resource instanceof Resource.Loading) {

                Log.d(TAG, "MainActivity: VariantDetails Loading...");
            } else if (resource instanceof Resource.Success) {
                Log.d(TAG, "MainActivity: VariantDetails Success. Ready to show BottomSheet.");
                VariantEntity variantDetails = resource.data; 
                if (variantDetails != null && !isFinishing() && !isDestroyed()) {
                    Fragment existingFragment = getSupportFragmentManager().findFragmentByTag("VariantDetailBottomSheet");
                    if (existingFragment == null) {
                        VariantDetailBottomSheetDialogFragment bottomSheet =
                                VariantDetailBottomSheetDialogFragment.newInstance(String.valueOf(variantDetails.getVariantId()), variantDetails.getName());
                        bottomSheet.show(getSupportFragmentManager(), "VariantDetailBottomSheet");
                        // variantViewModel.consumeVariantDetails(); // Убираем этот вызов отсюда
                    }
                }
            } else if (resource instanceof Resource.Error) {
                Log.e(TAG, "MainActivity: VariantDetails Error: " + resource.message); 
                Toast.makeText(this, "Ошибка загрузки деталей варианта: " + resource.message, Toast.LENGTH_LONG).show(); 
            }
        });
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
        }
    }

    private void displayCachedContent(String categoryId) {
        FrameLayout contentContainer = binding.contentContainer;
        if (contentContainer == null) {
            Log.e(TAG, "Content container is null");
            return;
        }

        contentContainer.removeAllViews();
        Log.d(TAG, "Контейнер contentContainer очищен перед заполнением новыми данными для " + categoryId);

        List<ContentItem> items = categoryDataCache.getOrDefault(categoryId, new ArrayList<>());

        if (items.isEmpty()) {
            showShimmer(shimmerContentLayout, contentContainer, errorPlaceholderContent);
            Log.d(TAG, "Нет данных в кэше для " + categoryId + ", показываем шиммер");
            return;
        }

        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setId(R.id.content_recycler); 

        contentAdapter = new ContentAdapter(items, onContentClickListener);

        recyclerView.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
            this, androidx.recyclerview.widget.DividerItemDecoration.VERTICAL));

        recyclerView.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        recyclerView.setClipToPadding(false);

        recyclerView.setAdapter(contentAdapter);

        try {
            LayoutAnimationController animation =
                AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fade_bottom);
            recyclerView.setLayoutAnimation(animation);
        } catch (Exception e) {
            Log.e(TAG, "Error loading animation", e);
        }

        TextView headerView = new TextView(this);
        if ("task".equals(categoryId)) {
            headerView.setText("Задания ЕГЭ");
        } else if ("theory".equals(categoryId)) {
            headerView.setText("Теория");
        } else if ("shpargalka".equals(categoryId)) {
            headerView.setText("Шпаргалки"); 
        } else {
            headerView.setText(categoryId.substring(0, 1).toUpperCase() + categoryId.substring(1));
        }

        headerView.setTextSize(20);
        headerView.setTypeface(headerView.getTypeface(), android.graphics.Typeface.BOLD);
        headerView.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(8));
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        headerView.setTextColor(ContextCompat.getColor(this, typedValue.resourceId));

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(headerView);
        linearLayout.addView(recyclerView);

        contentContainer.addView(linearLayout, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));

        contentContainer.setVisibility(View.VISIBLE);

        showData(shimmerContentLayout, contentContainer, errorPlaceholderContent);

        Log.d(TAG, "Контейнер contentContainer заполнен данными для категории " + categoryId + ", " + items.size() + " элементов");
    }

    /**
     * Обновляет UI с новостями.
     */
    private void updateNewsUI(List<NewsEntity> newsEntities) {
        Log.d(TAG, "News received: " + (newsEntities != null ? newsEntities.size() : 0));

        List<NewsItem> newsItems = new ArrayList<>();
        if (newsEntities != null) {
            for (NewsEntity entity : newsEntities) {
                if (entity != null) {
                    String dateString = String.valueOf(entity.getPublicationDate());
                    newsItems.add(new NewsItem(
                        entity.getTitle(),
                        dateString,
                        entity.getDescription(),
                        entity.getImageUrl(),
                        null
                    ));
                }
            }
        }

        if (newsAdapter != null) {
            newsAdapter.submitList(newsItems);
            Log.d(TAG, "Submitted list to NewsAdapter.");
            showData(shimmerNewsLayout, newsRecycler, errorPlaceholderNews);

        } else {
            Log.e(TAG, "NewsAdapter is null during updateNewsUI!");
        }
    }

    /**
     * Обновляет UI с прогрессом.
     */
    private void updateProgressUI(List<ProgressEntity> progressEntities) {
        if (progressEntities == null || progressEntities.isEmpty()) {
            Log.d(TAG, "updateProgressUI: пустой список прогресса или null. Адаптер будет очищен.");
            if (progressAdapter != null) {
                progressAdapter.updateItems(new ArrayList<>()); 
            }
            showData(shimmerProgressLayout, progressRecycler, errorPlaceholderProgress);
            return;
        }

        Log.d(TAG, "updateProgressUI: обновление данных прогресса, получено " + progressEntities.size() + " записей");

        for (ProgressEntity entity : progressEntities) {
            Log.d(TAG, "Запись прогресса: contentId=" + entity.getContentId() +
                  ", title=" + entity.getTitle() + ", percentage=" + entity.getPercentage() +
                  ", completed=" + entity.isCompleted());
        }

        List<ProgressEntity> taskGroupEntities = new ArrayList<>();
        for (ProgressEntity entity : progressEntities) {
            if (entity.getContentId() != null && entity.getContentId().startsWith("task_group_")) {
                taskGroupEntities.add(entity);
                Log.d(TAG, "Найдена запись прогресса для задания: " + entity.getContentId() + ", завершено: " + entity.isCompleted());
            }
        }

        Log.d(TAG, "updateProgressUI: после фильтрации осталось " + taskGroupEntities.size() + " записей типа task_group_");

        if (taskGroupEntities.isEmpty()) {
            Log.d(TAG, "Записей с task_group_ не найдено в БД, инициализируем прогресс");
            progressViewModel.initializeProgress();
            return;
        }

        if (!taskGroupEntities.isEmpty()) {
            int completedTasksCount = 0;
            for (ProgressEntity entity : taskGroupEntities) {
                if (entity.isCompleted()) {
                    completedTasksCount++;
                }
            }

            ArrayList<com.ruege.mobile.model.ProgressItem> progressItems = new ArrayList<>();

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
                    "TASK" 
                );
                progressItems.add(item);
            }

            for (int i = 0; i < progressItems.size(); i++) {
                com.ruege.mobile.model.ProgressItem item = progressItems.get(i);
                Log.d(TAG, "Проверка элемента " + i + ": id = " + item.getId() +
                      ", title = " + item.getTitle() + ", type = " + item.getType() +
                      ", процент = " + item.getPercentage());
            }

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
            showError(shimmerProgressLayout, progressRecycler, errorPlaceholderProgress);
            Log.w(TAG, "updateProgressUI: нет записей прогресса для типов заданий");

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
            boolean useDarkTheme = getSharedPreferences("app_settings", MODE_PRIVATE)
                .getBoolean("dark_theme", false);

            updateThemeToggleIcon(themeToggle, useDarkTheme);

            themeToggle.setOnClickListener(v -> {
                boolean currentDarkTheme = getSharedPreferences("app_settings", MODE_PRIVATE)
                    .getBoolean("dark_theme", false);

                boolean newDarkTheme = !currentDarkTheme;

                getSharedPreferences("app_settings", MODE_PRIVATE)
                    .edit()
                    .putBoolean("dark_theme", newDarkTheme)
                    .apply();

                updateThemeToggleIcon(themeToggle, newDarkTheme);

                AppCompatDelegate.setDefaultNightMode(
                    newDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

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
        if (contentDetailsSheetBehavior != null &&
            contentDetailsSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            contentDetailsSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        ProfileBottomSheetDialogFragment profileSheet = ProfileBottomSheetDialogFragment.newInstance();
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
    }

    /**
     * Показывает нижнюю панель с деталями контента
     */
    private void showBottomSheet(String title, String description, String contentId, String type) {
        if (contentDetailsSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            contentDetailsSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        clearBottomSheetContent();

        Log.d(TAG, "showBottomSheet: вызван с title=" + title + ", contentId=" + contentId + ", type=" + type);

        switch(type) {
            case "theory":
                TheoryBottomSheetDialogFragment theorySheet = TheoryBottomSheetDialogFragment.newInstance(contentId, title);
                if (getSupportFragmentManager().findFragmentByTag("TheoryBottomSheet") == null) {
                    theorySheet.show(getSupportFragmentManager(), "TheoryBottomSheet");
                }
                break;
            case "task":
                Log.d(TAG, "showBottomSheet: Показываем TaskDisplayBottomSheetDialogFragment для категории: " + contentId + ", title: " + title);
                TaskDisplayBottomSheetDialogFragment taskSheet = TaskDisplayBottomSheetDialogFragment.newInstance(contentId, title);
                    if (getSupportFragmentManager().findFragmentByTag(TaskDisplayBottomSheetDialogFragment.TAG_TASK_DISPLAY_BS) == null) { 
                    taskSheet.show(getSupportFragmentManager(), TaskDisplayBottomSheetDialogFragment.TAG_TASK_DISPLAY_BS);
                }
                break;
            case "news":
                Log.w(TAG, "showBottomSheet: тип 'news' вызван здесь, но новости теперь должны обрабатываться через NewsAdapter.OnNewsClickListener и NewsBottomSheetDialogFragment.");
                break;
            case "shpargalka":
                ShpargalkaBottomSheetDialogFragment shpargalkaSheet = ShpargalkaBottomSheetDialogFragment.newInstance(contentId, title, description);
                if (getSupportFragmentManager().findFragmentByTag(ShpargalkaBottomSheetDialogFragment.TAG_SHPARGALKA_BS) == null) {
                    shpargalkaSheet.show(getSupportFragmentManager(), ShpargalkaBottomSheetDialogFragment.TAG_SHPARGALKA_BS);
                }
                break;
            default:
                Log.e(TAG, "showBottomSheet: неизвестный тип контента: " + type);

                final TextView panelTitleTextView = binding.contentDetailsPanel.findViewById(R.id.panel_title);
                final TextView contentTitleTextView = binding.contentDetailsPanel.findViewById(R.id.content_title);
                final TextView contentDescriptionTextView = binding.contentDetailsPanel.findViewById(R.id.content_description);

                final WebView webView = binding.contentDetailsPanel.findViewById(R.id.content_web_view);
                final FrameLayout taskViewHostContainer = binding.contentDetailsPanel.findViewById(R.id.task_sheet_host_container);

                if (webView != null) webView.setVisibility(View.GONE);
                if (taskViewHostContainer != null) taskViewHostContainer.setVisibility(View.GONE);

                if (panelTitleTextView != null) {
                    panelTitleTextView.setText(title != null ? title : "Детали");
                    panelTitleTextView.setVisibility(View.VISIBLE);
                }

                if (contentTitleTextView != null) {
                    contentTitleTextView.setText(title);
                    contentTitleTextView.setVisibility(View.VISIBLE);
                }
                if (contentDescriptionTextView != null) {
                    contentDescriptionTextView.setText(description != null && !description.isEmpty() ?
                            description : "Контент этого типа пока не поддерживается.");
                    contentDescriptionTextView.setVisibility(View.VISIBLE);
                }

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
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_AUTO);
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setNestedScrollingEnabled(false);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        int textZoom = 200; 
        if (screenWidth <= 480) {
            textZoom = 190; 
        } else if (screenWidth >= 1200) {
            textZoom = 220; 
        }
        webView.getSettings().setTextZoom(textZoom);

        webView.setPadding(0, 0, 0, 0);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            boolean isWebViewDarkTheme = getSharedPreferences("app_settings", MODE_PRIVATE)
                .getBoolean("dark_theme", false);

            webView.getSettings().setForceDark(
                isWebViewDarkTheme ?
                android.webkit.WebSettings.FORCE_DARK_ON :
                android.webkit.WebSettings.FORCE_DARK_OFF
            );
        }

        if (webView.getLayoutParams() instanceof MarginLayoutParams) {
            MarginLayoutParams params = (MarginLayoutParams) webView.getLayoutParams();
            params.setMargins(0, 0, 0, 0);
            webView.setLayoutParams(params);
        }

        webView.setNestedScrollingEnabled(true);

        webView.setOnTouchListener(new View.OnTouchListener() {
            private float startY;
            private float startX;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                WebView webView = (WebView) v;

                int action = event.getAction();

                switch (action) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startY = event.getRawY();
                        startX = event.getRawX();
                        Log.d(TAG, "WebView: Начало касания");

                        contentDetailsSheetBehavior.setDraggable(false);
                        break;

                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        Log.d(TAG, "WebView: Конец касания");

                        contentDetailsSheetBehavior.setDraggable(true);
                        break;

                    case android.view.MotionEvent.ACTION_MOVE:
                        float deltaY = event.getRawY() - startY;
                        float deltaX = event.getRawX() - startX;

                        boolean isVerticalScroll = Math.abs(deltaY) > Math.abs(deltaX);

                        if (isVerticalScroll) {
                            if (webView.getScrollY() == 0 && deltaY > 50) {
                                Log.d(TAG, "WebView: Достигли верха и тянем вниз");
                                contentDetailsSheetBehavior.setDraggable(true);
                            } else if (!webView.canScrollVertically(1) && deltaY < -50) {
                                Log.d(TAG, "WebView: Достигли дна и тянем вверх");
                            } else {
                                contentDetailsSheetBehavior.setDraggable(false);
                            }
                        }
                        break;
                }

                return false;
            }
        });
    }

    /**
     * Применяет стили к HTML-контенту в зависимости от выбранной темы
     */
    public String applyStylesToHtml(String htmlContent, boolean isDarkTheme) {
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
            "table[border], table[border] td, table[border] th {" +
            "  border: 2px solid " + (isDarkTheme ? "#777777" : "#666666") + " !important;" +
            "}" +
            "table.bordered, table.bordered td, table.bordered th {" +
            "  border: 2px solid " + (isDarkTheme ? "#777777" : "#666666") + " !important;" +
            "}" +
            "</style>";

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
        logoutHandler.performLogout(
            this,                      
            this,                      
            googleAuthManager,         
            categoryDataCache          
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

        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }

        if (practiceStatsButton != null) {
            practiceStatsButton.setOnClickListener(v -> {
                hideProfilePanel();
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new com.ruege.mobile.ui.fragment.PracticeStatisticsFragment())
                    .addToBackStack(null)
                    .commit();

                Log.d(TAG, "Переход к экрану статистики практики");
            });
        }

        if (supportButton != null) {
            supportButton.setOnClickListener(v -> {
                String telegramLink = "https://t.me/GuRu_ege_official";

                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(telegramLink));

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    Log.d(TAG, "Переход в Telegram для связи с поддержкой");
                } else {
                    Toast.makeText(this, "Пожалуйста, напишите в тг: @GuRu_ege_official. Не удалось открыть Telegram.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Невозможно открыть Telegram: нет приложения для обработки Intent");
                }
            });
        }

        UserEntity currentUser = userViewModel.getCurrentUser().getValue();

        if (nameTextView != null) {
            if (currentUser != null && currentUser.getUsername() != null && !currentUser.getUsername().isEmpty()) {
                nameTextView.setText(currentUser.getUsername());
            } else {
                nameTextView.setText("Ученик"); 
            }
        }

        if (emailTextView != null) {
            if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                emailTextView.setText(currentUser.getEmail());
            } else {
                emailTextView.setText("Локальный режим"); 
            }
        }

        if (profileAvatar != null) {
            if (currentUser != null && currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                Glide.with(this)
                    .load(currentUser.getAvatarUrl())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(profileAvatar);
            } else {
                profileAvatar.setImageResource(R.drawable.ic_profile);
            }
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

        final WebView webView = binding.contentDetailsPanel.findViewById(R.id.content_web_view);
        final FrameLayout taskViewHostContainer = binding.contentDetailsPanel.findViewById(R.id.task_sheet_host_container);
        final TextView generalTitleView = binding.contentDetailsPanel.findViewById(R.id.content_title); 
        final TextView generalDescriptionView = binding.contentDetailsPanel.findViewById(R.id.content_description); 
        final ProgressBar loadingIndicator = binding.contentDetailsPanel.findViewById(R.id.content_loading);
        final TextView errorView = binding.contentDetailsPanel.findViewById(R.id.content_error);

        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank"); 
            webView.setVisibility(View.GONE);
            Log.d(TAG, "clearBottomSheetContent: WebView очищен и скрыт.");
        }

        if (taskViewHostContainer != null) {
            taskViewHostContainer.removeAllViews();
            taskViewHostContainer.setVisibility(View.GONE);
            Log.d(TAG, "clearBottomSheetContent: Контейнер заданий очищен и скрыт.");
        }

        if (generalTitleView != null) generalTitleView.setVisibility(View.GONE);
        if (generalDescriptionView != null) generalDescriptionView.setVisibility(View.GONE);

        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
        if (errorView != null) errorView.setVisibility(View.GONE);

        if (contentViewModel != null) {
            contentViewModel.getTasks().setValue(null); 
            contentViewModel.clearContent(); 
            contentViewModel.clearTaskCache(null); 
            contentViewModel.clearTaskDetailCache(); 
        }

        Log.d(TAG, "clearBottomSheetContent: Содержимое панели и кеши ViewModel очищены.");
    }

    /**
     * Публичный метод для отображения BottomSheet с теорией, используется из TheoryFragment
     */
    public void showTheoryBottomSheet(String title, String description, String contentId) {
        showBottomSheet(title, description, contentId, "theory");
    }

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
     * Показывает bottomSheet специально для новостей, используя новый NewsBottomSheetDialogFragment.
     * Этот метод больше не нужен, если вся логика перенесена в onNewsClickListener.
     * Если он все еще вызывается откуда-то, его нужно адаптировать или удалить.
     */
    private void showNewsBottomSheet(String title, String description, String additionalInfo) {
        Log.d(TAG, "showNewsBottomSheet (старый) вызван для: " + title + ". Перенаправляем на новый NewsBottomSheetDialogFragment.");
        NewsBottomSheetDialogFragment newsSheet = NewsBottomSheetDialogFragment.newInstance(
            title, 
            description, 
            additionalInfo,
            null
        );
        if (getSupportFragmentManager().findFragmentByTag(NewsBottomSheetDialogFragment.TAG_NEWS_BS) == null) {
            newsSheet.show(getSupportFragmentManager(), NewsBottomSheetDialogFragment.TAG_NEWS_BS);
        }
    }

    /**
     * Обновляет аватар пользователя в главном окне
     */
    private void updateUserAvatar(UserEntity user) {
        ImageView userAvatar = binding.userAvatar;
        if (userAvatar != null) {
            if (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(this)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(userAvatar);
            } else {
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
            if (progressSyncRepository != null) {
                progressSyncRepository.syncNow(true, true); 
                Log.d(TAG, "startExitSync: Запущена batch-синхронизация через репозиторий");
            }

            com.ruege.mobile.worker.ProgressSyncWorker.startOneTimeSync(
                this.getApplicationContext(), 
                true,  
                true   
            );
            Log.d(TAG, "startExitSync: Запущена batch-синхронизация через Worker");

            Log.d(TAG, "startExitSync: Синхронизация при выходе запущена успешно");
        } catch (Exception e) {
            Log.e(TAG, "startExitSync: Ошибка при запуске синхронизации", e);
        }
    }

    /**
     * Сохраняет файл в директорию загрузок устройства
     * @param sourceFile исходный файл
     * @param fileName имя файла в директории загрузок
     * @param description описание файла
     */
    public void downloadPdfToDownloads(File sourceFile, String fileName, String description) {
        try {
            Log.d(TAG, "Начало копирования PDF в загрузки: " + sourceFile.getAbsolutePath());

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                copyPdfToDownloads(sourceFile, fileName, description);
            } else {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Запрашиваем разрешение на запись во внешнее хранилище");

                    pendingSourceFile = sourceFile;
                    pendingFileName = fileName;
                    pendingDescription = description;

                    requestPermissions(
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_STORAGE
                    );

                    return; 
                } else {
                    copyPdfToDownloads(sourceFile, fileName, description);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при проверке разрешений", e);
            Toast.makeText(this, "Ошибка при сохранении файла: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyPdfToDownloads(File sourceFile, String fileName, String description) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                saveUsingMediaStore(sourceFile, fileName, description);
            } else {
                saveUsingDirectFile(sourceFile, fileName, description);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при копировании файла в загрузки", e);
            Toast.makeText(this, "Ошибка при сохранении файла: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @android.annotation.TargetApi(android.os.Build.VERSION_CODES.Q)
    private void saveUsingMediaStore(File sourceFile, String fileName, String description) {
        try {
            Log.d(TAG, "Сохранение файла используя MediaStore API: " + fileName);

            android.content.ContentValues contentValues = new android.content.ContentValues();
            contentValues.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
            contentValues.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf");
            contentValues.put(android.provider.MediaStore.Downloads.TITLE, fileName);
            contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 1);

            android.content.ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

            if (uri != null) {
                try (OutputStream outputStream = resolver.openOutputStream(uri);
                     FileInputStream inputStream = new FileInputStream(sourceFile)) {

                    if (outputStream == null) {
                        Log.e(TAG, "Не удалось открыть выходной поток для URI: " + uri);
                        return;
                    }

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }

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

    private void saveUsingDirectFile(File sourceFile, String fileName, String description) {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                boolean created = downloadsDir.mkdirs();
                Log.d(TAG, "Создана директория загрузок: " + created);
            }

            File destinationFile = new File(downloadsDir, fileName);

            if (destinationFile.exists()) {
                boolean deleted = destinationFile.delete();
                Log.d(TAG, "Удаление существующего файла: " + deleted);
            }

            if (!sourceFile.exists()) {
                Log.e(TAG, "Исходный файл не существует: " + sourceFile.getAbsolutePath());
                Toast.makeText(this, "Ошибка: файл не найден", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Начало копирования файла из " + sourceFile.getAbsolutePath() +
                       " в " + destinationFile.getAbsolutePath());

            FileInputStream inputStream = new FileInputStream(sourceFile);
            FileOutputStream outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int length;
            long totalCopied = 0;
            long fileSize = sourceFile.length();

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
                totalCopied += length;

                if (fileSize > 0 && totalCopied % 1024000 < 1024) { 
                    int progress = (int) (totalCopied * 100 / fileSize);
                    Log.d(TAG, "Прогресс копирования: " + progress + "%");
                }
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Log.d(TAG, "Файл успешно скопирован в загрузки: " + destinationFile.getAbsolutePath());

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
        FrameLayout contentContainer = binding.contentContainer; 
        if (contentContainer == null) {
            Log.e(TAG, "contentContainer не найден в макете!");
            return;
        }
        contentContainer.removeAllViews(); 

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

                VariantAdapter adapter = new VariantAdapter(listener); 
                recyclerView.setAdapter(adapter);
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
        if (googleAuthManager == null) {
            Log.e(TAG, "GoogleAuthManager is not initialized in MainActivity. Cannot perform logout.");
            Toast.makeText(this, "Ошибка: Менеджер авторизации не инициализирован.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (logoutHandler != null) {
             logoutHandler.performLogout(
                this,
                this,
                googleAuthManager,
                categoryDataCache
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
        return getSharedPreferences("app_settings", MODE_PRIVATE)
                .getBoolean("dark_theme", false);
    }

    public void showEssayBottomSheet(String title, String description, String contentId) {
        Log.d(TAG, "showEssayBottomSheet: Показываем сочинение с ID: " + contentId + " и заголовком: " + title);
        EssayBottomSheetDialogFragment essaySheet = EssayBottomSheetDialogFragment.newInstance(contentId, title);
        essaySheet.show(getSupportFragmentManager(), EssayBottomSheetDialogFragment.TAG_ESSAY_BS);
    }

}