package com.ruege.mobile.ui.fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.ruege.mobile.auth.TokenManager
import com.ruege.mobile.databinding.FragmentTwoFactorLoginBinding
import com.ruege.mobile.ui.activity.LoginActivity
import com.ruege.mobile.ui.viewmodel.LoginViewModel
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TwoFactorLoginFragment : Fragment() {

    private var _binding: FragmentTwoFactorLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by activityViewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    private var loginSessionToken: String? = null
    private var userEmail: String? = null
    private var timer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTwoFactorLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            loginSessionToken = it.getString("login_session_token")
            userEmail = it.getString("user_email")
            binding.tvUserEmail.text = userEmail
        }

        setupClickListeners()
        observeTfaState()
        startTimer()
    }

    private fun setupClickListeners() {
        binding.btnConfirmTfa.setOnClickListener {
            val code = binding.etTfaCode.text.toString().trim()
            if (code.length == 6 && loginSessionToken != null) {
                viewModel.verifyTfaCode(loginSessionToken!!, code)
            } else {
                showError("Код должен состоять из 6 цифр.")
            }
        }
    }

    private fun observeTfaState() {
        viewModel.tfaVerifyState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    resource.data?.let {
                        tokenManager.saveAccessToken(it.accessToken)
                        tokenManager.saveRefreshToken(it.refreshToken)
                        Toast.makeText(context, "Вход выполнен успешно!", Toast.LENGTH_LONG).show()
                        (activity as? LoginActivity)?.navigateToMain()
                    } ?: showError("Получены пустые данные")
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(resource.message ?: "Неверный код")
                }
                else -> showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBarTfa.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnConfirmTfa.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        binding.tvTfaError.text = message
        binding.tvTfaError.visibility = View.VISIBLE
    }

    private fun startTimer() {
        binding.tvTfaError.visibility = View.GONE
        binding.btnResendTfaCode.isEnabled = false
        timer?.cancel()
        timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvTfaTimer.text = "Отправить код повторно можно через: ${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                binding.tvTfaTimer.text = ""
                binding.btnResendTfaCode.isEnabled = true
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        _binding = null
    }
} 