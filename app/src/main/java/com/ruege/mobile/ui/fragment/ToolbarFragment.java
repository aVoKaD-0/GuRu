package com.ruege.mobile.ui.fragment;

import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.button.MaterialButton;
import com.ruege.mobile.R;
import com.ruege.mobile.auth.GoogleAuthManager;
import com.ruege.mobile.data.local.entity.UserEntity;
import com.ruege.mobile.databinding.FragmentToolbarBinding;
import com.ruege.mobile.ui.bottomsheet.ProfileBottomSheetDialogFragment;
import com.ruege.mobile.ui.viewmodel.OnboardingViewModel;
import com.ruege.mobile.ui.viewmodel.TasksViewModel;
import com.ruege.mobile.ui.viewmodel.UserViewModel;
import com.ruege.mobile.utils.LogoutHandler;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class ToolbarFragment extends Fragment {

    private FragmentToolbarBinding binding;

    private UserViewModel userViewModel;
    private TasksViewModel tasksViewModel;
    private OnboardingViewModel onboardingViewModel;
    private GoogleAuthManager googleAuthManager;

    @Inject
    LogoutHandler logoutHandler;
    @Inject
    GoogleSignInClient googleSignInClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentToolbarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        googleAuthManager = new GoogleAuthManager(requireActivity(), googleSignInClient);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        tasksViewModel = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);
        onboardingViewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        setupToolbar();
        setupThemeToggle();
        showOnboardingSequence();

        userViewModel.getFirstUser();
        userViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                Timber.d("LOG_CHAIN: ToolbarFragment - Пользователь авторизован.");
            } else {
                Timber.d("LOG_CHAIN: ToolbarFragment - Пользователь не авторизован.");
            }
            updateUserAvatar(user);
        });
    }

    private void setupToolbar() {
        binding.userAvatar.setOnClickListener(v -> showProfilePanel());
    }

    private void setupThemeToggle() {
        boolean useDarkTheme = requireActivity().getSharedPreferences("app_settings", requireActivity().MODE_PRIVATE)
                .getBoolean("dark_theme", false);
        updateThemeToggleIcon(binding.themeToggle, useDarkTheme);
        binding.themeToggle.setOnClickListener(v -> {
            boolean currentDarkTheme = requireActivity().getSharedPreferences("app_settings", requireActivity().MODE_PRIVATE)
                    .getBoolean("dark_theme", false);
            boolean newDarkTheme = !currentDarkTheme;
            requireActivity().getSharedPreferences("app_settings", requireActivity().MODE_PRIVATE)
                    .edit()
                    .putBoolean("dark_theme", newDarkTheme)
                    .apply();
            updateThemeToggleIcon(binding.themeToggle, newDarkTheme);
            AppCompatDelegate.setDefaultNightMode(
                    newDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
    }

    private void showOnboardingSequence() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("app_settings", requireActivity().MODE_PRIVATE);
        boolean onboardingShown = prefs.getBoolean("onboarding_toolbar_shown", false);

        if (!onboardingShown) {
            new TapTargetSequence(requireActivity())
                    .targets(
                            TapTarget.forView(binding.themeToggle, "(1/7) Нажми, чтобы включить и выключить свет", "P.S. сменить тему")
                                    .outerCircleColor(R.color.primary)
                                    .targetCircleColor(android.R.color.white)
                                    .textColor(android.R.color.white)
                                    .dimColor(android.R.color.black)
                                    .drawShadow(true)
                                    .tintTarget(false)
                                    .cancelable(false)
                                    .transparentTarget(true)
                                    .targetRadius(60),
                            TapTarget.forView(binding.userAvatar, "(2/7) Здесь ты можешь посмотреть профиль и другую информацию")
                                    .outerCircleColor(R.color.primary)
                                    .targetCircleColor(android.R.color.white)
                                    .textColor(android.R.color.white)
                                    .dimColor(android.R.color.black)
                                    .drawShadow(true)
                                    .tintTarget(false)
                                    .cancelable(false)
                                    .transparentTarget(true)
                                    .targetRadius(60)
                    )
                    .listener(new TapTargetSequence.Listener() {
                        @Override
                        public void onSequenceFinish() {
                            prefs.edit().putBoolean("onboarding_toolbar_shown", true).apply();
                            onboardingViewModel.setToolbarOnboardingFinished(true);
                        }

                        @Override
                        public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                        }

                        @Override
                        public void onSequenceCanceled(TapTarget lastTarget) {
                            
                        }
                    }).start();
        }
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
        Fragment existingFragment = getParentFragmentManager().findFragmentByTag(ProfileBottomSheetDialogFragment.TAG_PROFILE_BS);
        if (existingFragment == null && !requireActivity().isFinishing()) {
            ProfileBottomSheetDialogFragment profileSheet = ProfileBottomSheetDialogFragment.newInstance();
            profileSheet.show(getParentFragmentManager(), ProfileBottomSheetDialogFragment.TAG_PROFILE_BS);
            Timber.d("showProfilePanel called - displaying ProfileBottomSheetDialogFragment");
        } else {
            Timber.w("Панель профиля уже показана или активность завершается.");
        }
    }

    private void updateUserAvatar(UserEntity user) {
        if (binding != null) {
            ImageView userAvatar = binding.userAvatar;
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 