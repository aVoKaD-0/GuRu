package com.ruege.mobile.ui.fragment;

import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.ruege.mobile.R;
import com.ruege.mobile.data.local.entity.ProgressEntity;
import com.ruege.mobile.databinding.CustomTabBinding;
import com.ruege.mobile.databinding.FragmentHomeBinding;
import com.ruege.mobile.model.NewsItem;
import com.ruege.mobile.model.ProgressItem;
import com.ruege.mobile.ui.adapter.NewsAdapter;
import com.ruege.mobile.ui.adapter.ProgressAdapter;
import com.ruege.mobile.ui.adapter.ViewPagerAdapter;
import com.ruege.mobile.ui.bottomsheet.NewsBottomSheetDialogFragment;
import com.ruege.mobile.ui.viewmodel.NewsViewModel;
import com.ruege.mobile.ui.viewmodel.OnboardingViewModel;
import com.ruege.mobile.ui.viewmodel.ProgressViewModel;
import com.ruege.mobile.utils.SlowItemAnimator;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private NewsViewModel newsViewModel;
    private ProgressViewModel progressViewModel;
    private OnboardingViewModel onboardingViewModel;

    private ProgressAdapter progressAdapter;
    private NewsAdapter newsAdapter;

    private final String[] categoryTitles = {"Теория", "Задания", "Сочинения", "Шпаргалки", "Варианты"};

    private final NewsAdapter.OnNewsClickListener onNewsClickListener = newsItem -> {
        Timber.d("Clicked news: " + newsItem.getTitle());
        NewsBottomSheetDialogFragment newsSheet = NewsBottomSheetDialogFragment.newInstance(
                newsItem.getTitle(),
                newsItem.getDescription(),
                "Дата публикации: " + newsItem.getDate(),
                newsItem.getImageUrl()
        );
        if (getParentFragmentManager().findFragmentByTag(NewsBottomSheetDialogFragment.TAG_NEWS_BS) == null) {
            newsSheet.show(getParentFragmentManager(), NewsBottomSheetDialogFragment.TAG_NEWS_BS);
        }
    };

    private final ProgressAdapter.OnProgressClickListener onProgressClickListener = progressItem -> {
        Timber.d("Clicked progress: " + progressItem.getTitle());
        Toast.makeText(requireContext(), "Прогресс: " + progressItem.getTitle() + " - " + progressItem.getPercentage() + "%",
                Toast.LENGTH_SHORT).show();
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViewModels();
        setupUI();
        setupAdaptersAndObservers();
        setupOnboarding();
    }

    private void setupViewModels() {
        newsViewModel = new ViewModelProvider(this).get(NewsViewModel.class);
        progressViewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        onboardingViewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);
    }

    private void setupUI() {
        setupTabLayout();
    }

    private void setupTabLayout() {
        binding.viewPager.setAdapter(new ViewPagerAdapter(requireActivity()));

        new TabLayoutMediator(binding.tabNavigation, binding.viewPager, (tab, position) -> {
            CustomTabBinding tabBinding = CustomTabBinding.inflate(getLayoutInflater());
            tabBinding.tabText.setText(categoryTitles[position]);
            tabBinding.tabText.setContentDescription(categoryTitles[position] + " - раздел");
            tab.setCustomView(tabBinding.getRoot());
        }).attach();

        binding.tabNavigation.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
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

    private void setupOnboarding() {
        onboardingViewModel.isToolbarOnboardingFinished().observe(getViewLifecycleOwner(), finished -> {
            if (finished) {
                SharedPreferences prefs = requireActivity().getSharedPreferences("app_settings", requireActivity().MODE_PRIVATE);
                boolean homeOnboardingShown = prefs.getBoolean("onboarding_home_shown", false);

                if (!homeOnboardingShown) {
                    new Handler(Looper.getMainLooper()).postDelayed(this::showHomeOnboardingSequence, 500);
                }
            }
        });
    }

    private void showHomeOnboardingSequence() {
        if (binding.recyclerProgress.getVisibility() != View.VISIBLE || binding.recyclerNews.getVisibility() != View.VISIBLE) {
            return;
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("app_settings", requireActivity().MODE_PRIVATE);

        new TapTargetSequence(requireActivity())
                .targets(
                        TapTarget.forView(binding.recyclerProgress, "(3/7) Тут находится твой прогресс", "Его можно прокручивать вправо, чтобы увидеть больше.")
                                .outerCircleColor(R.color.primary)
                                .targetCircleColor(android.R.color.white)
                                .textColor(android.R.color.white)
                                .dimColor(android.R.color.black)
                                .drawShadow(true)
                                .cancelable(false)
                                .tintTarget(false)
                                .transparentTarget(true),
                        TapTarget.forView(binding.recyclerNews, "(4/7) Самые свежие новости", "Все важные события в одном месте.")
                                .outerCircleColor(R.color.primary)
                                .targetCircleColor(android.R.color.white)
                                .textColor(android.R.color.white)
                                .dimColor(android.R.color.black)
                                .drawShadow(true)
                                .cancelable(false)
                                .tintTarget(false)
                                .transparentTarget(true),
                        TapTarget.forView(binding.tabNavigation, "(5/7) Смотри сколько разделов", "Их тоже можно прокручивать!")
                                .outerCircleColor(R.color.primary)
                                .targetCircleColor(android.R.color.white)
                                .textColor(android.R.color.white)
                                .dimColor(android.R.color.black)
                                .drawShadow(true)
                                .cancelable(false)
                                .tintTarget(false)
                                .transparentTarget(true)
                )
                .listener(new TapTargetSequence.Listener() {
                    @Override
                    public void onSequenceFinish() {
                        prefs.edit().putBoolean("onboarding_home_shown", true).apply();
                        onboardingViewModel.setHomeOnboardingFinished(true);
                    }

                    @Override
                    public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                        if (lastTarget.id() == binding.recyclerProgress.getId()) {
                            binding.recyclerProgress.smoothScrollBy(250, 0);
                        } else if (lastTarget.id() == binding.tabNavigation.getId()) {
                            binding.tabNavigation.smoothScrollTo(binding.tabNavigation.getScrollX() + 300, 0);
                        }
                    }

                    @Override
                    public void onSequenceCanceled(TapTarget lastTarget) {
                    }
                }).start();
    }

    public void navigateToTab(int position) {
        if (position >= 0 && position < binding.tabNavigation.getTabCount()) {
            binding.tabNavigation.selectTab(binding.tabNavigation.getTabAt(position));
        }
    }

    private void setupAdaptersAndObservers() {
        progressAdapter = new ProgressAdapter(new ArrayList<>(), onProgressClickListener);
        binding.recyclerProgress.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerProgress.setAdapter(progressAdapter);
        binding.recyclerProgress.setItemAnimator(new SlowItemAnimator());
        try {
            final LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fade_bottom);
            binding.recyclerProgress.setLayoutAnimation(controller);
        } catch (Resources.NotFoundException e) {
            Timber.e(e, "Layout animation resource not found for progressRecycler");
        }

        newsAdapter = new NewsAdapter(new ArrayList<>(), onNewsClickListener);
        binding.recyclerNews.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerNews.setAdapter(newsAdapter);
        binding.recyclerNews.setItemAnimator(new SlowItemAnimator());
        try {
            final LayoutAnimationController controller = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_fade_bottom);
            binding.recyclerNews.setLayoutAnimation(controller);
        } catch (Resources.NotFoundException e) {
            Timber.e(e, "Layout animation resource not found for newsRecycler");
        }

        showShimmer(binding.shimmerProgress, binding.recyclerProgress, binding.errorPlaceholderProgress);
        showShimmer(binding.shimmerNews, binding.recyclerNews, binding.errorPlaceholderNews);

        progressViewModel.getUserProgress().observe(getViewLifecycleOwner(), this::updateProgressUI);
        newsViewModel.getNewsItemsLiveData().observe(getViewLifecycleOwner(), this::updateNewsUI);
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

    private void updateNewsUI(List<NewsItem> newsItems) {
        Timber.d("Новости для UI: " + (newsItems != null ? newsItems.size() : 0));
        if (newsAdapter != null) {
            newsAdapter.submitList(newsItems);
            Timber.d("Список новостей отправлен в NewsAdapter.");
            showData(binding.shimmerNews, binding.recyclerNews, binding.errorPlaceholderNews);
            if (newsItems == null || newsItems.isEmpty()) {
                Timber.d("Список новостей пуст или null после наблюдения LiveData.");
            }
        } else {
            Timber.e("NewsAdapter is null в updateNewsUI!");
        }
    }

    private void updateProgressUI(List<ProgressEntity> progressEntities) {
        if (progressEntities == null || progressEntities.isEmpty()) {
            Timber.d("updateProgressUI: пустой список прогресса или null. Адаптер будет очищен.");
            if (progressAdapter != null) {
                progressAdapter.updateItems(new ArrayList<>());
            }
            showData(binding.shimmerProgress, binding.recyclerProgress, binding.errorPlaceholderProgress);
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
            binding.recyclerProgress.setAdapter(progressAdapter);
            Timber.d("updateProgressUI: создан новый адаптер с " + progressItemsForAdapter.size() + " элементами");
        } else {
            progressAdapter.updateItems(progressItemsForAdapter);
            Timber.d("updateProgressUI: обновлен существующий адаптер с " + progressItemsForAdapter.size() + " элементами");
        }
        showData(binding.shimmerProgress, binding.recyclerProgress, binding.errorPlaceholderProgress);
        Timber.d("updateProgressUI: данные прогресса обновлены, общий прогресс: " + overallPercentage + "%");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 