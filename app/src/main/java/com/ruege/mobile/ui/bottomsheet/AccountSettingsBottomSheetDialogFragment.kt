package com.ruege.mobile.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ruege.mobile.data.local.entity.UserEntity
import com.ruege.mobile.databinding.BottomsheetAccountSettingsBinding
import com.ruege.mobile.ui.viewmodel.UserViewModel
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AccountSettingsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomsheetAccountSettingsBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAccountSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnChangePassword.setOnClickListener {
            userViewModel.changePassword()
        }

        binding.switch2fa.setOnCheckedChangeListener { _, isChecked ->
            if (binding.switch2fa.isPressed) {
                userViewModel.toggleTwoFactorAuth(isChecked)
            }
        }

        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            updateUi(user)
        }
        
        userViewModel.twoFactorAuthResult.observe(viewLifecycleOwner) { result ->
            when(result) {
                is Resource.Loading -> {
                    binding.switch2fa.isEnabled = false
                }
                is Resource.Success -> {
                    binding.switch2fa.isEnabled = true
                    binding.tv2faError.visibility = View.GONE
                    Toast.makeText(context, "Настройки 2FA обновлены", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    binding.switch2fa.isEnabled = true
                    binding.tv2faError.text = result.message ?: "Произошла ошибка"
                    binding.tv2faError.visibility = View.VISIBLE
                    val currentUserState = userViewModel.currentUser.value?.isIs2faEnabled() ?: false
                    binding.switch2fa.isChecked = currentUserState
                }
            }
        }
        
        userViewModel.passwordChangeResult.observe(viewLifecycleOwner) { result: Resource<Unit> ->
            when(result) {
                is Resource.Loading -> {
                    binding.btnChangePassword.isEnabled = false
                }
                is Resource.Success -> {
                    binding.btnChangePassword.isEnabled = true
                    Toast.makeText(context, "Инструкция по смене пароля отправлена на вашу почту", Toast.LENGTH_LONG).show()
                }
                is Resource.Error -> {
                    binding.btnChangePassword.isEnabled = true
                    binding.tv2faError.text = "сейчас сменить пароль не удается, повторите позже или напишите в поддержку"
                    binding.tv2faError.visibility = View.VISIBLE
                }
            }
        }
        
        userViewModel.getFirstUser()
    }

    private fun updateUi(user: UserEntity?) {
        if (user != null) {
            binding.switch2fa.isChecked = user.isIs2faEnabled()
            binding.switch2fa.isEnabled = true
        } else {
            binding.switch2fa.isEnabled = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AccountSettingsBottomSheet"

        @JvmStatic
        fun newInstance(): AccountSettingsBottomSheetDialogFragment {
            return AccountSettingsBottomSheetDialogFragment()
        }
    }
} 