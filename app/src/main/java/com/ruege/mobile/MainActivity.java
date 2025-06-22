package com.ruege.mobile;

import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.res.Resources;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.ruege.mobile.ui.adapter.NewsAdapter;
import com.ruege.mobile.ui.adapter.ProgressAdapter;
import com.ruege.mobile.ui.adapter.ViewPagerAdapter;
import com.ruege.mobile.auth.GoogleAuthManager;
import com.ruege.mobile.data.local.AppDatabase;
import com.ruege.mobile.data.local.TokenManager;
import com.ruege.mobile.data.local.entity.ContentEntity;
import com.ruege.mobile.data.local.entity.NewsEntity;
import com.ruege.mobile.data.local.entity.ProgressEntity;
import com.ruege.mobile.data.local.entity.UserEntity;
import com.ruege.mobile.data.local.entity.VariantEntity;
import com.ruege.mobile.databinding.ActivityMainBinding;
import com.ruege.mobile.model.ContentItem;
import com.ruege.mobile.model.NewsItem;
import com.ruege.mobile.model.ProgressItem;
import com.ruege.mobile.model.TaskItem;
import com.ruege.mobile.ui.viewmodel.NewsViewModel;
import com.ruege.mobile.ui.viewmodel.ProgressViewModel;
import com.ruege.mobile.ui.sync.SyncStatusManager;
import com.ruege.mobile.ui.viewmodel.UserViewModel;
import com.ruege.mobile.ui.viewmodel.TasksViewModel;
import com.ruege.mobile.utils.SlowItemAnimator;
import com.ruege.mobile.ui.viewmodel.ShpargalkaViewModel;
import com.ruege.mobile.ui.viewmodel.VariantViewModel;
import com.ruege.mobile.ui.adapter.VariantAdapter;
import com.ruege.mobile.utilss.Resource;

import java.util.ArrayList;
import java.util.Collections;
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

import com.ruege.mobile.data.repository.ProgressSyncRepository;

import androidx.lifecycle.LifecycleOwnerKt;
import com.ruege.mobile.utils.FlowJavaHelper;
import com.ruege.mobile.utils.PdfDownloadHelper;
import timber.log.Timber;

import kotlinx.coroutines.Job;
import kotlinx.coroutines.DelayKt;
import java.util.concurrent.CancellationException;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.BuildersKt;
import androidx.viewpager2.widget.ViewPager2;

import com.ruege.mobile.utilss.AuthEvent;
import com.ruege.mobile.utilss.AuthEventBus;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private GoogleAuthManager googleAuthManager;
    private PdfDownloadHelper pdfDownloadHelper;

    @Inject
    TokenManager tokenManager;
    @Inject
    com.ruege.mobile.data.repository.AuthRepository authRepository;
    @Inject
    com.ruege.mobile.utilss.UserDataCleaner userDataCleaner;
    @Inject
    com.ruege.mobile.utilss.LogoutHandler logoutHandler;
    @Inject
    AuthEventBus authEventBus;

    private ProgressSyncRepository progressSyncRepository ;

    private NewsViewModel newsViewModel;
    private ProgressViewModel progressViewModel;

    private TasksViewModel tasksViewModel;
    private UserViewModel userViewModel;
    private VariantViewModel variantViewModel;
    private ShpargalkaViewModel shpargalkaViewModel;

    private ProgressAdapter progressAdapter;
    private NewsAdapter newsAdapter;

    private final String[] categories = {"theory", "task", "shpargalka", "variant"};
    private final String[] categoryTitles = {"Теория", "Задания", "Шпаргалки", "Варианты"};

    private RecyclerView progressRecycler;
    private RecyclerView newsRecycler;

    private ShimmerFrameLayout shimmerProgressLayout;
    private ShimmerFrameLayout shimmerNewsLayout;
    private ViewPager2 viewPager;

    private View errorPlaceholderProgress;
    private View errorPlaceholderNews;

    private final Handler handler = new Handler(Looper.getMainLooper());
    
    private File pendingSourceFile;
    private String pendingFileName;
    private String pendingDescription;
    private static final int REQUEST_WRITE_STORAGE = 1001;


    private final NewsAdapter.OnNewsClickListener onNewsClickListener = newsItem -> {
        Timber.d("Clicked news: " + newsItem.getTitle());
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
        Timber.d("Clicked progress: " + progressItem.getTitle());
        Toast.makeText(this, "Прогресс: " + progressItem.getTitle() + " - " + progressItem.getPercentage() + "%",
                Toast.LENGTH_SHORT).show();
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            Timber.d("Разрешение на запись получено");
                if (pendingSourceFile != null && pendingFileName != null) {
                Timber.d("Продолжаем копирование отложенного файла");
                    copyPdfToDownloads(pendingSourceFile, pendingFileName, pendingDescription);
                    pendingSourceFile = null;
                    pendingFileName = null;
                    pendingDescription = null;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            Timber.d("MainActivity onCreate");

            googleAuthManager = new GoogleAuthManager(this);
            pdfDownloadHelper = new PdfDownloadHelper(this);
            setupThemeToggle();
            setupUI();
            observeAuthEvents();

            newsViewModel = new ViewModelProvider(this).get(NewsViewModel.class);
            progressViewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
            userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
            variantViewModel = new ViewModelProvider(this).get(VariantViewModel.class);
            shpargalkaViewModel = new ViewModelProvider(this).get(ShpargalkaViewModel.class);

            setupAdaptersAndObservers();

            Timber.d("MainActivity setup complete");

            ImageView userAvatar = binding.userAvatar;
            if (userAvatar != null) {
                userAvatar.setOnClickListener(v -> showProfilePanel());
            }

            userViewModel.getFirstUser();
            userViewModel.getCurrentUser().observe(this, user -> {
                if (user != null) {
                    Timber.d("LOG_CHAIN: MainActivity.onCreate - Пользователь авторизован.");
                    // contentViewModel.syncInitialContent(); // Этот вызов теперь в init ContentViewModel
                } else {
                    Timber.d("LOG_CHAIN: MainActivity.onCreate - Пользователь не авторизован. Синхронизация контента не запускается.");
                }
                updateUserAvatar(user);
            });
        } catch (Exception e) {
            Timber.e(e, "Error in MainActivity onCreate");
            Toast.makeText(this, "Произошла ошибка при запуске. Пожалуйста, перезапустите приложение.", Toast.LENGTH_LONG).show();
        }
    }

    private void observeAuthEvents() {
        FlowJavaHelper.collectInScope(
            authEventBus.getEvents(),
            LifecycleOwnerKt.getLifecycleScope(this),
            event -> {
                if (event instanceof AuthEvent.SessionExpired) {
                    Timber.w("Session expired event received. Forcing logout.");
                    Toast.makeText(this, "Сессия истекла. Пожалуйста, войдите снова.", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(this, com.ruege.mobile.ui.activity.LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        );
    }

    private void showDatabaseResetDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Ошибка базы данных")
            .setMessage("Обнаружена проблема с базой данных. Для решения проблемы необходимо сбросить данные приложения. Продолжить?")
            .setPositiveButton("Сбросить", (dialog, which) -> resetDatabase())
            .setNegativeButton("Выход", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private void resetDatabase() {
        try {
            Timber.w("Сброс базы данных...");
            AppDatabase.clearAndRebuildDatabase(this);
            Toast.makeText(this, "База данных сброшена. Пожалуйста, войдите снова.", Toast.LENGTH_LONG).show();

            Intent intent = getIntent();
            finish();
            startActivity(intent);
        } catch (Exception e) {
            Timber.e(e, "Ошибка при сбросе базы данных");
            new AlertDialog.Builder(this)
                .setTitle("Критическая ошибка")
                .setMessage("Произошла критическая ошибка при сбросе базы данных. Рекомендуется переустановить приложение.")
                .setPositiveButton("ОК", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
        }
    }

    private void setupUI() {
        View scrollContentView = binding.contentMain.findViewById(R.id.scroll_content);

        shimmerProgressLayout = scrollContentView.findViewById(R.id.shimmer_progress);
        shimmerNewsLayout = scrollContentView.findViewById(R.id.shimmer_news);

        errorPlaceholderProgress = scrollContentView.findViewById(R.id.error_placeholder_progress);
        errorPlaceholderNews = scrollContentView.findViewById(R.id.error_placeholder_news);

        progressRecycler = scrollContentView.findViewById(R.id.recycler_progress);
        newsRecycler = scrollContentView.findViewById(R.id.recycler_news);
        viewPager = binding.contentMain.findViewById(R.id.view_pager);

        ImageView userAvatar = binding.getRoot().findViewById(R.id.user_avatar);
        if (userAvatar != null) {
            userAvatar.setClickable(true);
            userAvatar.setFocusable(true);
            userAvatar.setOnClickListener(v -> {
                Timber.d("Аватар нажат, показываем панель профиля");
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    showProfilePanel();
            });
        } else {
            Timber.e("Не удалось найти user_avatar в binding!");
        }
        setupTabLayout();
    }

    private void setupTabLayout() {
        TabLayout tabLayout = binding.contentMain.findViewById(R.id.tab_navigation);
        viewPager.setAdapter(new ViewPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            View customView = getLayoutInflater().inflate(R.layout.custom_tab, null);
            TextView textView = customView.findViewById(R.id.tab_text);
            textView.setText(categoryTitles[position]);
            textView.setContentDescription(categoryTitles[position] + " - раздел");
            tab.setCustomView(customView);
        }).attach();


        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                 Timber.d("Выбрана категория: " + categoryTitles[tab.getPosition()]);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Timber.d("Вкладка " + categoryTitles[tab.getPosition()] + " выбрана повторно.");
            }
        });
    }

    private void setupAdaptersAndObservers() {
        progressAdapter = new ProgressAdapter(new ArrayList<>(), onProgressClickListener);
        progressRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        progressRecycler.setAdapter(progressAdapter);
        progressRecycler.setItemAnimator(new SlowItemAnimator());
        try {
            final LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fade_bottom);
            progressRecycler.setLayoutAnimation(controller);
        } catch (Resources.NotFoundException e) {
            Timber.e(e, "Layout animation resource not found for progressRecycler");
        }


        newsAdapter = new NewsAdapter(new ArrayList<>(), onNewsClickListener);
        newsRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        newsRecycler.setAdapter(newsAdapter);
        newsRecycler.setItemAnimator(new SlowItemAnimator());
        try {
            final LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fade_bottom);
            newsRecycler.setLayoutAnimation(controller);
        } catch (Resources.NotFoundException e) {
            Timber.e(e, "Layout animation resource not found for newsRecycler");
        }

        showShimmer(shimmerProgressLayout, progressRecycler, errorPlaceholderProgress);
        showShimmer(shimmerNewsLayout, newsRecycler, errorPlaceholderNews);

        progressViewModel.getUserProgress().observe(this, this::updateProgressUI);

        newsViewModel.getNewsItemsLiveData().observe(this, this::updateNewsUI);
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

    /**
     * Обновляет UI с новостями.
     */
    private void updateNewsUI(List<NewsItem> newsItems) {
        Timber.d("Новости для UI: " + (newsItems != null ? newsItems.size() : 0));
        if (newsAdapter != null) {
            newsAdapter.submitList(newsItems);
            Timber.d("Список новостей отправлен в NewsAdapter.");
            showData(shimmerNewsLayout, newsRecycler, errorPlaceholderNews);
            if (newsItems == null || newsItems.isEmpty()) {
                Timber.d("Список новостей пуст или null после наблюдения LiveData.");
            }
        } else {
            Timber.e("NewsAdapter is null в updateNewsUI!");
        }
    }

    /**
     * Обновляет UI с прогрессом.
     */
    private void updateProgressUI(List<ProgressEntity> progressEntities) {
        if (progressEntities == null || progressEntities.isEmpty()) {
            Timber.d("updateProgressUI: пустой список прогресса или null. Адаптер будет очищен.");
            if (progressAdapter != null) {
                progressAdapter.updateItems(new ArrayList<>()); 
            }
            showData(shimmerProgressLayout, progressRecycler, errorPlaceholderProgress);
            return;
        }

        Timber.d("updateProgressUI: обновление данных прогресса, получено " + progressEntities.size() + " записей");

        List<ProgressEntity> taskGroupEntities = new ArrayList<>();
        for (ProgressEntity entity : progressEntities) {
            if (entity.getContentId() != null && entity.getContentId().startsWith("task_group_")) {
                taskGroupEntities.add(entity);
            }
        }
        
        Timber.d("updateProgressUI: после фильтрации осталось " + taskGroupEntities.size() + " записей типа task_group_");

        if (taskGroupEntities.isEmpty() && progressViewModel != null) {
            Timber.d("Записей с task_group_ не найдено в БД, инициализируем прогресс");
        }
        
        ArrayList<ProgressItem> progressItemsForAdapter = new ArrayList<>();
            int completedTasksCount = 0;
            for (ProgressEntity entity : taskGroupEntities) {
                if (entity.isCompleted()) {
                    completedTasksCount++;
                }
            }

        final int TOTAL_EGE_TASK_TYPES = 27;
        int overallPercentage = (taskGroupEntities.isEmpty() || TOTAL_EGE_TASK_TYPES == 0) ? 0 : (completedTasksCount * 100) / TOTAL_EGE_TASK_TYPES;

        ProgressItem overallProgress = new ProgressItem(
                "overall_progress",
                "Общий прогресс",
                "Выполнено " + completedTasksCount + " из " + TOTAL_EGE_TASK_TYPES + " типов заданий",
                overallPercentage,
                "PROGRESS"
            );
        progressItemsForAdapter.add(overallProgress);

            for (ProgressEntity entity : taskGroupEntities) {
                String taskNumber = entity.getContentId().replace("task_group_", "");
                String title = "Задание " + taskNumber;
                String description = entity.isCompleted() ? "Завершено" : "В процессе";
            ProgressItem item = new ProgressItem(
                    entity.getContentId(),
                    title,
                    description,
                    entity.getPercentage(),
                    "TASK" 
                );
            progressItemsForAdapter.add(item);
            }

            if (progressAdapter == null) {
            progressAdapter = new ProgressAdapter(progressItemsForAdapter, onProgressClickListener);
                progressRecycler.setAdapter(progressAdapter);
            Timber.d("updateProgressUI: создан новый адаптер с " + progressItemsForAdapter.size() + " элементами");
            } else {
            progressAdapter.updateItems(progressItemsForAdapter);
            Timber.d("updateProgressUI: обновлен существующий адаптер с " + progressItemsForAdapter.size() + " элементами");
            }
            showData(shimmerProgressLayout, progressRecycler, errorPlaceholderProgress);
        Timber.d("updateProgressUI: данные прогресса обновлены, общий прогресс: " + overallPercentage + "%");
    }

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

    public boolean isDarkThemeEnabled() {
        return getSharedPreferences("app_settings", MODE_PRIVATE)
            .getBoolean("dark_theme", false);
    }

    private void updateThemeToggleIcon(MaterialButton button, boolean isDarkTheme) {
        if (button != null) {
            button.setIconResource(isDarkTheme ? R.drawable.ic_lantern_on : R.drawable.ic_lantern_off);
            Drawable drawable = button.getIcon();
            if (drawable instanceof Animatable) {
                ((Animatable) drawable).start();
            }
        }
    }

    private void showProfilePanel() {
        Fragment existingFragment = getSupportFragmentManager().findFragmentByTag(ProfileBottomSheetDialogFragment.TAG_PROFILE_BS);
        if (existingFragment == null && !isFinishing()) {
        ProfileBottomSheetDialogFragment profileSheet = ProfileBottomSheetDialogFragment.newInstance();
            profileSheet.show(getSupportFragmentManager(), ProfileBottomSheetDialogFragment.TAG_PROFILE_BS);
            Timber.d("showProfilePanel called - displaying ProfileBottomSheetDialogFragment");
        } else {
            Timber.w("Панель профиля уже показана или активность ззавершается.");
        }
    }
    
    private void hideProfilePanel() {
        Fragment profileSheet = getSupportFragmentManager().findFragmentByTag(ProfileBottomSheetDialogFragment.TAG_PROFILE_BS);
        if (profileSheet instanceof ProfileBottomSheetDialogFragment) {
            ((ProfileBottomSheetDialogFragment) profileSheet).dismissAllowingStateLoss();
        }
    }

    /**
     * Общий метод для показа BottomSheet для разных типов контента.
     */
    public void showBottomSheet(String title, String description, String contentId, String type) {
        if (type == null) {
            Timber.e("showBottomSheet: Type is null for contentId: " + contentId);
            return;
        }

        switch (type) {
            case "theory":
                TheoryBottomSheetDialogFragment.newInstance(contentId, title)
                    .show(getSupportFragmentManager(), "TheoryBottomSheet");
                break;
            case "task_group":
                 TaskDisplayBottomSheetDialogFragment taskSheet = TaskDisplayBottomSheetDialogFragment.newInstance(contentId, title);
                 if (getSupportFragmentManager().findFragmentByTag(TaskDisplayBottomSheetDialogFragment.TAG_TASK_BS) == null) {
                     taskSheet.show(getSupportFragmentManager(), TaskDisplayBottomSheetDialogFragment.TAG_TASK_BS);
                 }
                break;
            case "shpargalka":
                ShpargalkaBottomSheetDialogFragment shpargalkaSheet = ShpargalkaBottomSheetDialogFragment.newInstance(contentId, title, description);
                    shpargalkaSheet.show(getSupportFragmentManager(), ShpargalkaBottomSheetDialogFragment.TAG_SHPARGALKA_BS);
                break;
            case "essay":
                 showEssayBottomSheet(title, description, contentId);
                 break;
            default:
                Timber.e("showBottomSheet: неизвестный тип контента: " + type + " для contentId: " + contentId);
                Toast.makeText(this, "Тип контента '" + type + "' не поддерживается", Toast.LENGTH_SHORT).show();
                break;
        }
    }
    
    public String applyStylesToHtml(String htmlContent, boolean isDarkTheme) {
        String cssThemeVariables = isDarkTheme ? "<style> " +
            ":root { " +
            "  --text-color: #E0E0E0; " +
            "  --background-color: #121212; " +
            "  --link-color: #BB86FC; " +
            "  --code-background-color: #2C2C2C; " +
            "  --border-color: #424242; " +
            "} " +
            "</style>" :
            "<style> " +
            ":root { " +
            "  --text-color: #212121; " +
            "  --background-color: #FFFFFF; " +
            "  --link-color: #6200EE; " +
            "  --code-background-color: #f5f5f5; " +
            "  --border-color: #e0e0e0; " +
            "} " +
            "</style>";

        String commonCss = "<style> " +
            "body { font-family: sans-serif; margin: 16px; color: var(--text-color); background-color: var(--background-color); line-height: 1.6; } " +
            "h1, h2, h3, h4, h5, h6 { margin-top: 24px; margin-bottom: 16px; font-weight: bold; } " +
            "h1 { font-size: 2em; } " +
            "h2 { font-size: 1.75em; } " +
            "h3 { font-size: 1.5em; } " +
            "p { margin-bottom: 16px; } " +
            "a { color: var(--link-color); text-decoration: none; } " +
            "ul, ol { margin-bottom: 16px; padding-left: 30px; } " +
            "li { margin-bottom: 8px; } " +
            "img { max-width: 100%; height: auto; border-radius: 8px; margin-top: 8px; margin-bottom: 8px; } " +
            "pre, code { font-family: monospace; background-color: var(--code-background-color); padding: 2px 4px; border-radius: 4px; font-size: 0.9em; overflow-x: auto; } " +
            "pre { padding: 16px; display: block; white-space: pre-wrap; word-wrap: break-word; } " +
            "blockquote { border-left: 4px solid var(--border-color); padding-left: 16px; margin-left: 0; margin-right: 0; margin-bottom: 16px; font-style: italic; } " +
            "table { width: 100%; border-collapse: collapse; margin-bottom: 16px; } " +
            "th, td { border: 1px solid var(--border-color); padding: 8px; text-align: left; } " +
            "th { background-color: var(--code-background-color); } " +
            ".math-display { display: block; overflow-x: auto; overflow-y: hidden; padding: 0.5em; } " +
            "</style>";

        return "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                cssThemeVariables + commonCss + "</head><body>" + htmlContent + "</body></html>";
    }


    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    public void initiateLogout() {
        Timber.d("Logout initiated from MainActivity");
        if (logoutHandler != null) {
            Map<String, ?> categoryCache = (tasksViewModel != null)
                ? tasksViewModel.getTasksCategoryCache()
                : Collections.emptyMap();

            logoutHandler.performLogout(
                this,
                this,
                googleAuthManager,
                categoryCache
            );
        } else {
            Timber.e("LogoutHandler is null, cannot perform logout.");
            Toast.makeText(this, "Ошибка выхода из аккаунта.", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideAllFragments() {
        Fragment theorySheet = getSupportFragmentManager().findFragmentByTag("TheoryBottomSheet");
        if (theorySheet != null) ((TheoryBottomSheetDialogFragment) theorySheet).dismiss();

        Fragment taskSheet = getSupportFragmentManager().findFragmentByTag(TaskDisplayBottomSheetDialogFragment.TAG_TASK_BS);
        if (taskSheet != null) ((TaskDisplayBottomSheetDialogFragment) taskSheet).dismiss();

        Fragment shpargalkaSheet = getSupportFragmentManager().findFragmentByTag("ShpargalkaBottomSheet");
        if (shpargalkaSheet != null) ((ShpargalkaBottomSheetDialogFragment) shpargalkaSheet).dismiss();

    }

    public void showTheoryBottomSheet(String title, String description, String contentId) {
        Timber.d("showTheoryBottomSheet: Показываем теорию с ID: " + contentId + " и заголовком: " + title);
        TheoryBottomSheetDialogFragment theorySheet = TheoryBottomSheetDialogFragment.newInstance(contentId, title);
        theorySheet.show(getSupportFragmentManager(), "TheoryBottomSheet");
    }
    
    public void showEssayBottomSheet(String title, String description, String contentId) {
        EssayBottomSheetDialogFragment.newInstance(contentId, title)
            .show(getSupportFragmentManager(), "EssayBottomSheet");
    }
    
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

    @Override
    protected void onPause() {
        super.onPause();
        Timber.d("onPause: запускаем синхронизацию прогресса");
        startExitSync();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Timber.d("onStop: запускаем синхронизацию прогресса");
        startExitSync();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Timber.d("onDestroy: запускаем синхронизацию прогресса (если еще не завершена)");
        if (handler != null) { 
            handler.removeCallbacksAndMessages(null);
        }
    }

    private void startExitSync() {
            if (progressSyncRepository != null) {
            Timber.d("Запуск синхронизации прогресса при выходе/паузе MainActivity");
                progressSyncRepository.syncNow(true, true); 
        } else {
            Timber.w("progressSyncRepository is null, не могу запустить синхронизацию");
        }
    }
    
    public void downloadPdfToDownloads(File sourceFile, String fileName, String description) {
        if (sourceFile == null || !sourceFile.exists() || fileName == null || fileName.isEmpty()) {
            Toast.makeText(this, "Ошибка: Файл для скачивания не найден или имя не указано.", Toast.LENGTH_LONG).show();
            Timber.e("downloadPdfToDownloads: sourceFile is null, не существует или fileName пуст.");
            return;
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            this.pendingSourceFile = sourceFile;
            this.pendingFileName = fileName;
            this.pendingDescription = description;
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                } else {
                    pdfDownloadHelper.copyPdfToDownloads(sourceFile, fileName, description);
        }
    }

    private void copyPdfToDownloads(File sourceFile, String fileName, String description) {
        pdfDownloadHelper.copyPdfToDownloads(sourceFile, fileName, description);
    }
}