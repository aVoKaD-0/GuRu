package com.ruege.mobile.ui.bottomsheet

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.R
import com.ruege.mobile.auth.GoogleAuthManager
import com.ruege.mobile.data.local.entity.UserEntity
import com.ruege.mobile.databinding.LayoutProfileContentBinding
import com.ruege.mobile.ui.viewmodel.TasksViewModel
import com.ruege.mobile.ui.viewmodel.UserViewModel
import com.ruege.mobile.utils.LogoutHandler
import dagger.hilt.android.AndroidEntryPoint
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class ProfileBottomSheetDialogFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var logoutHandler: LogoutHandler
    @Inject
    lateinit var googleSignInClient: GoogleSignInClient

    private val userViewModel: UserViewModel by activityViewModels()
    private val tasksViewModel: TasksViewModel by activityViewModels()
    private lateinit var googleAuthManager: GoogleAuthManager

    private var _binding: LayoutProfileContentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutProfileContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        googleAuthManager = GoogleAuthManager(requireActivity(), googleSignInClient)

        setupClickListeners()
        observeUserData()
        setAppVersion()
        
        userViewModel.getFirstUser() 
    }

    private fun setAppVersion() {
        try {
            val version = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
            binding.appVersion.text = "Версия $version"
        } catch (e: Exception) {
            Timber.e(TAG_PROFILE_BS, "Не удалось получить версию приложения", e)
            binding.appVersion.text = "Версия N/A"
        }
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            val categoryCache = tasksViewModel.getTasksCategoryCache() ?: Collections.emptyMap<String, Any>()
            logoutHandler.performLogout(
                requireContext(),
                googleAuthManager,
                categoryCache
            )
            dismiss()
        }
        
        binding.btnAccountSettings.setOnClickListener {
            Timber.d(TAG_PROFILE_BS, "Кнопка 'Настройка аккаунта' нажата")
            val settingsSheet = AccountSettingsBottomSheetDialogFragment.newInstance()
            settingsSheet.show(parentFragmentManager, AccountSettingsBottomSheetDialogFragment.TAG)
        }
        
        binding.btnPracticeStatistics.setOnClickListener {
            Timber.d(TAG_PROFILE_BS, "Кнопка 'Статистика практики' нажата")
            val statisticsSheet = PracticeStatisticsBottomSheetDialogFragment.newInstance()
            statisticsSheet.show(parentFragmentManager, PracticeStatisticsBottomSheetDialogFragment.TAG)
        }
        
        binding.btnSupport.setOnClickListener {
            Timber.d(TAG_PROFILE_BS, "Кнопка 'Связаться с поддержкой' нажата")
            val supportLinksSheet = SupportLinksBottomSheetDialogFragment.newInstance()
            supportLinksSheet.show(parentFragmentManager, SupportLinksBottomSheetDialogFragment.TAG)
        }

        binding.btnPrivacyPolicyProfile.setOnClickListener {
            Timber.d(TAG_PROFILE_BS, "Нажата кнопка Политики конфиденциальности в профиле")
            val url = "http://46.8.232.191/privacy_policy"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)
            startActivity(intent)
        }

        binding.btnClearCache.setOnClickListener {
            Timber.d(TAG_PROFILE_BS, "Нажата кнопка 'Очистить кеш'")
            val clearCacheSheet = ClearCacheBottomSheetDialogFragment.newInstance()
            clearCacheSheet.show(parentFragmentManager, ClearCacheBottomSheetDialogFragment.TAG)
        }
    }

    private fun observeUserData() {
        userViewModel.currentUser.observe(viewLifecycleOwner) { user: UserEntity? ->
            if (user != null) {
                updateProfileUI(user)
            } else {
                binding.profileName.text = "Гость"
                binding.profileEmail.text = "Войдите, чтобы синхронизировать данные"
                binding.profileAvatar.setImageResource(R.drawable.ic_profile) 
            }
        }
    }
    
    private fun updateProfileUI(user: UserEntity) {
        binding.profileName.text = user.username ?: "Пользователь"
        binding.profileEmail.text = user.email ?: "Email не указан"
        
        if (!user.avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.avatarUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.profileAvatar)
        } else {
            binding.profileAvatar.setImageResource(R.drawable.ic_profile)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { bottomSheet ->
                val behavior = BottomSheetBehavior.from(bottomSheet)
                val layoutParams = bottomSheet.layoutParams
                
                val windowHeight = getWindowHeight()
                if (layoutParams != null) {
                    layoutParams.height = windowHeight
                }
                bottomSheet.layoutParams = layoutParams
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = windowHeight 
                behavior.isFitToContents = false 
                behavior.skipCollapsed = true 
                
                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                           dismiss() 
                        }
                    }
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })
            }
        }
        return dialog
    }
    
    private fun getWindowHeight(): Int {
        val displayMetrics = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    companion object {
        const val TAG_PROFILE_BS = "ProfileBottomSheet"

        @JvmStatic
        fun newInstance(): ProfileBottomSheetDialogFragment {
            return ProfileBottomSheetDialogFragment()
        }
    }
} 