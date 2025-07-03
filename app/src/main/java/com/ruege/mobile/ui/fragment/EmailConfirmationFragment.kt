package com.ruege.mobile.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.ruege.mobile.MainActivity
import com.ruege.mobile.auth.TokenManager
import com.ruege.mobile.databinding.FragmentEmailConfirmationBinding
import com.ruege.mobile.ui.viewmodel.LoginViewModel
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class EmailConfirmationFragment : Fragment() {

    private var _binding: FragmentEmailConfirmationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by activityViewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    private var timer: CountDownTimer? = null

    companion object {
        const val EXTRA_SESSION_TOKEN = "session_token"
        const val EXTRA_USER_EMAIL = "user_email"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmailConfirmationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        arguments?.let {
            viewModel.sessionToken = it.getString(EXTRA_SESSION_TOKEN)
            binding.tvUserEmail.text = it.getString(EXTRA_USER_EMAIL) ?: "вашу почту"
        }

        setupClickListeners()
        observeConfirmationState()
        observe2faState()
        observeResendState()
        startTimer()
    }

    private fun setupClickListeners() {
        binding.btnConfirm.setOnClickListener {
            val code = binding.etConfirmationCode.text.toString().trim()
            if (code.length == 6) {
                viewModel.verifyCode(code)
            } else {
                showError("Код должен состоять из 6 цифр")
            }
        }
        binding.btnResendCode.setOnClickListener {
            viewModel.resendConfirmationCode()
        }
    }

    private fun observeResendState() {
        viewModel.resendCodeState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    Toast.makeText(context, "Код отправлен повторно", Toast.LENGTH_SHORT).show()
                    startTimer()
                }
                is Resource.Error -> {
                    showLoading(false)
                    binding.tvResendError.text = "Невозможно повторно отправить код подтверждения почты"
                    binding.tvResendError.visibility = View.VISIBLE
                    binding.btnResendCode.isEnabled = true
                }
                else -> showLoading(false)
            }
        }
    }

    private fun observeConfirmationState() {
        viewModel.verifyCodeState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    resource.data?.let {
                        tokenManager.saveAccessToken(it.accessToken)
                        tokenManager.saveRefreshToken(it.refreshToken)
                        Toast.makeText(context, "Аккаунт успешно подтвержден!", Toast.LENGTH_LONG).show()
                        show2faDialog(it.user.userId)
                    } ?: showError("Получены пустые данные")
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError("Неверный код подтверждения")
                }
                else -> showLoading(false)
            }
        }
    }

    private fun startTimer() {
        binding.tvResendError.visibility = View.GONE
        binding.btnResendCode.isEnabled = false
        binding.btnConfirm.isEnabled = true
        timer?.cancel()

        val codeLifetimeMillis = 10 * 60 * 1000L
        val resendCooldownMillis = 1 * 60 * 1000L

        timer = object : CountDownTimer(codeLifetimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (millisUntilFinished > codeLifetimeMillis - resendCooldownMillis) {
                    val cooldownSecondsLeft = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished - (codeLifetimeMillis - resendCooldownMillis))
                    binding.tvTimer.text = "Отправить код повторно можно через: ${cooldownSecondsLeft} с"
                } else {
                    if (!binding.btnResendCode.isEnabled) {
                        binding.btnResendCode.isEnabled = true
                    }
                    val minutesLeft = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                    val secondsLeft = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                    binding.tvTimer.text = String.format("Код истекает через: %02d:%02d", minutesLeft, secondsLeft)
                }
            }

            override fun onFinish() {
                binding.tvTimer.text = "Код истек. Отправьте новый."
                binding.btnConfirm.isEnabled = false
                binding.btnResendCode.isEnabled = true
            }
        }.start()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnConfirm.isEnabled = !isLoading
        if (isLoading) {
            binding.btnResendCode.isEnabled = false
        }
    }

    private fun show2faDialog(userId: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Двухфакторная аутентификация")
            .setMessage("Хотите усилить защиту вашего аккаунта и включить двухфакторную аутентификацию?")
            .setPositiveButton("Да, включить") { dialog, _ ->
                viewModel.enable2faOnRegistration(userId)
                dialog.dismiss()
            }
            .setNegativeButton("Позже") { dialog, _ ->
                dialog.dismiss()
                navigateToMain()
            }
            .setCancelable(false)
            .show()
    }

    private fun observe2faState() {
        viewModel.enable2faState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    Toast.makeText(context, "2FA успешно включена! Настройка будет доступна в профиле.", Toast.LENGTH_LONG).show()
                    navigateToMain()
                }
                is Resource.Error -> {
                    showLoading(false)
                    val errorMessage = resource.message ?: "Не удалось включить 2FA"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    navigateToMain()
                }
                else -> {}
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToMain() {
        activity?.let {
            val intent = Intent(it, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            it.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        _binding = null
    }
}
