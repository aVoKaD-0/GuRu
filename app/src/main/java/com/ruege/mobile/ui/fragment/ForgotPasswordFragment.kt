package com.ruege.mobile.ui.fragment

import android.os.Bundle
import timber.log.Timber
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.recaptcha.RecaptchaAction
import com.google.android.recaptcha.RecaptchaClient
import com.ruege.mobile.R
import com.ruege.mobile.databinding.FragmentForgotPasswordBinding
import com.ruege.mobile.ui.viewmodel.LoginViewModel
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var recaptchaClient: RecaptchaClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnResetPassword.setOnClickListener {
            triggerRecaptcha()
        }
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun triggerRecaptcha() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val email = binding.etEmail.text.toString().trim()
                if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    val token = recaptchaClient.execute(RecaptchaAction.custom("password_recovery")).getOrThrow()
                    viewModel.requestPasswordRecovery(email, token)
                } else {
                    showError("Введите корректный email")
                }
            } catch (e: Exception) {
                showError("Не удалось пройти проверку reCAPTCHA. Попробуйте снова.")
                Timber.e("ForgotPasswordFragment", "reCAPTCHA error", e)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.passwordRecoveryState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    Resource.Success("Если аккаунт с таким email существует, на него будет отправлена ссылка для сброса пароля.")
                    findNavController().navigateUp()
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(resource.message ?: "Не удалось сбросить пароль")
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnResetPassword.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Resource.Error(message, binding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearRegistrationStates()
        _binding = null
    }
} 