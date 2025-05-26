package com.ruege.mobile.ui.bottomsheet

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.MainActivity
import com.ruege.mobile.R
import com.ruege.mobile.data.local.entity.UserEntity
import com.ruege.mobile.ui.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
// Если LogoutHandler используется, убедитесь, что импорт есть, или он получается другим способом
// import com.ruege.mobile.utils.LogoutHandler 
// import javax.inject.Inject // Если LogoutHandler инжектируется

private const val TAG_PROFILE_BS = "ProfileBottomSheet"

@AndroidEntryPoint
class ProfileBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val userViewModel: UserViewModel by activityViewModels()
    
    // Если LogoutHandler нужен и инжектируется:
    // @Inject
    // lateinit var logoutHandler: LogoutHandler

    private var profileAvatar: ImageView? = null
    private var profileName: TextView? = null
    private var profileEmail: TextView? = null
    private var btnPracticeStatistics: Button? = null
    private var btnSupport: Button? = null
    private var btnLogout: Button? = null
    private var appVersionTextView: TextView? = null // Переименовал для ясности
    private var btnPrivacyPolicyProfile: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Инфлейтим оригинальный макет для профиля
        return inflater.inflate(R.layout.layout_profile_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Инициализация View
        profileAvatar = view.findViewById(R.id.profile_avatar)
        profileName = view.findViewById(R.id.profile_name)
        profileEmail = view.findViewById(R.id.profile_email)
        btnPracticeStatistics = view.findViewById(R.id.btn_practice_statistics)
        btnSupport = view.findViewById(R.id.btn_support)
        btnLogout = view.findViewById(R.id.btn_logout)
        appVersionTextView = view.findViewById(R.id.app_version) // Используем новое имя
        btnPrivacyPolicyProfile = view.findViewById(R.id.btn_privacy_policy_profile)

        setupClickListeners()
        observeUserData()
        setAppVersion()
        
        userViewModel.getFirstUser() 
    }

    private fun setAppVersion() {
        try {
            val version = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
            appVersionTextView?.text = "Версия $version"
        } catch (e: Exception) {
            Log.e(TAG_PROFILE_BS, "Не удалось получить версию приложения", e)
            appVersionTextView?.text = "Версия N/A"
        }
    }

    private fun setupClickListeners() {
        btnLogout?.setOnClickListener {
            // Убедимся, что activity это MainActivity и вызываем метод выхода
            (activity as? MainActivity)?.initiateLogout() 
            dismiss() 
        }
        
        btnPracticeStatistics?.setOnClickListener {
            Log.d(TAG_PROFILE_BS, "Кнопка 'Статистика практики' нажата")
            val statisticsSheet = PracticeStatisticsBottomSheetDialogFragment.newInstance()
            statisticsSheet.show(parentFragmentManager, PracticeStatisticsBottomSheetDialogFragment.TAG)
            // Не закрываем профиль сразу, чтобы пользователь мог вернуться
        }
        
        btnSupport?.setOnClickListener {
            Log.d(TAG_PROFILE_BS, "Кнопка 'Связаться с поддержкой' нажата")
            val supportLinksSheet = SupportLinksBottomSheetDialogFragment.newInstance()
            supportLinksSheet.show(parentFragmentManager, SupportLinksBottomSheetDialogFragment.TAG)
        }

        btnPrivacyPolicyProfile?.setOnClickListener {
            Log.d(TAG_PROFILE_BS, "Нажата кнопка Политики конфиденциальности в профиле")
            val url = "https://gu-ru-ashen.vercel.app/privacy_policy"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)
            startActivity(intent)
        }
    }

    private fun observeUserData() {
        userViewModel.currentUser.observe(viewLifecycleOwner) { user: UserEntity? ->
            if (user != null) {
                updateProfileUI(user)
            } else {
                // UI для состояния, когда пользователь не загружен или гость
                profileName?.text = "Гость"
                profileEmail?.text = "Войдите, чтобы синхронизировать данные"
                profileAvatar?.setImageResource(R.drawable.ic_profile) // Иконка по умолчанию
            }
        }
    }
    
    private fun updateProfileUI(user: UserEntity) {
        profileName?.text = user.username ?: "Пользователь"
        profileEmail?.text = user.email ?: "Email не указан"
        
        profileAvatar?.let { avatarView ->
            if (!user.avatarUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(user.avatarUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(avatarView)
            } else {
                avatarView.setImageResource(R.drawable.ic_profile)
            }
        }
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
                        if (newState == BottomSheetBehavior.STATE_DRAGGING && behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                           // Поведение как в VariantDetail
                        }
                         if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            dismiss() // Добавляем закрытие при скрытии, если нужно
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
        @JvmStatic
        fun newInstance(): ProfileBottomSheetDialogFragment {
            return ProfileBottomSheetDialogFragment()
        }
    }
} 