package com.ruege.mobile.ui.fragment

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.recaptcha.Recaptcha
import com.google.android.recaptcha.RecaptchaAction
import com.google.android.recaptcha.RecaptchaClient
import com.google.android.recaptcha.RecaptchaException
import com.ruege.mobile.BuildConfig
import com.ruege.mobile.R
import com.ruege.mobile.auth.TokenManager
import com.ruege.mobile.databinding.LoginRegisterBinding
import com.ruege.mobile.ui.activity.LoginActivity
import com.ruege.mobile.ui.viewmodel.LoginViewModel
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ruege.mobile.data.repository.LoginResult
import timber.log.Timber

@AndroidEntryPoint
class LoginRegisterFragment : Fragment() {

    private var _binding: LoginRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by activityViewModels()

    @Inject
    lateinit var tokenManager: TokenManager
    
    private val recaptchaSiteKey = BuildConfig.RECAPTCHA_SITE_KEY

    private var isRecaptchaInitialized = false

    private enum class RegistrationStep {
        EMAIL_PASSWORD,
        USERNAME
    }
    private var currentStep = RegistrationStep.EMAIL_PASSWORD

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var recaptchaClient: RecaptchaClient

    private var isRegisterMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LoginRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.clearRegistrationStates()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        
        lifecycleScope.launch {
            try {
                recaptchaClient = Recaptcha.fetchClient(requireActivity().application, recaptchaSiteKey)
                isRecaptchaInitialized = true
                updateContinueButtonState()
            } catch (e: RecaptchaException) {
                isRecaptchaInitialized = false
                showError("Не удалось инициализировать reCAPTCHA.")
                updateContinueButtonState()
                Timber.e(e, "reCAPTCHA initialization failed")
            }
        }

        setupClickListeners()
        setupObservers()
        binding.toggleButtonGroup.check(R.id.btn_login_toggle)
        updateUiForMode(R.id.btn_login_toggle)
        updateContinueButtonState()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.toggleButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                updateUiForMode(checkedId)
                updateContinueButtonState()
            }
        }
        binding.btnContinue.setOnClickListener { handleContinue() }
        binding.btnForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginRegisterFragment_to_forgotPasswordFragment)
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateContinueButtonState()
            }
        }
        binding.etEmail.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)
        binding.etConfirmPassword.addTextChangedListener(textWatcher)
        binding.etUsername.addTextChangedListener(textWatcher)
    }

    private fun setupObservers() {
        viewModel.emailLoginState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                    clearInputErrors()
                }
                is Resource.Success -> {
                    showLoading(false)
                    when (val loginResult = resource.data) {
                        is LoginResult.Success -> {
                            tokenManager.saveAccessToken(loginResult.authResponse.accessToken)
                            tokenManager.saveRefreshToken(loginResult.authResponse.refreshToken)
                            (activity as? LoginActivity)?.navigateToMain()
                        }
                        is LoginResult.TwoFactorRequired -> {
                            val email = binding.etEmail.text.toString().trim()
                            val action = LoginRegisterFragmentDirections.actionLoginRegisterFragmentToTwoFactorLoginFragment(
                                loginSessionToken = loginResult.tfaResponse.loginSessionToken,
                                userEmail = email
                            )
                            findNavController().navigate(action)
                        }
                        null -> { /* Do nothing on null state */ }
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    if (resource.message?.contains("Неверный email или пароль", ignoreCase = true) == true) {
                        binding.tilEmail.error = " "
                        binding.tilPassword.error = "Неверный email или пароль"
                    } else {
                        showError(resource.message ?: "Неизвестная ошибка")
                    }
                }
                else -> { /* Do nothing on null state */ }
            }
        }
        
        viewModel.registrationStartState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
               is Resource.Loading -> showLoading(true)
               is Resource.Success -> {
                   showLoading(false)
                   currentStep = RegistrationStep.USERNAME
                   updateUiForRegistrationStep()
               }
               is Resource.Error -> {
                   showLoading(false)
                   val errorMessage = resource.message ?: "Ошибка на 1-м шаге регистрации"
                   if (errorMessage.contains("Email already registered", ignoreCase = true)) {
                       showLoginSuggestionDialog()
                   } else {
                       showError(errorMessage)
                   }
               }
                else -> {}
            }
       }

       viewModel.setUsernameState.observe(viewLifecycleOwner) { resource ->
           when (resource) {
               is Resource.Loading -> showLoading(true)
               is Resource.Success -> {
                   showLoading(false)
                   val action = LoginRegisterFragmentDirections.actionLoginRegisterFragmentToEmailConfirmationFragment(
                       sessionToken = viewModel.sessionToken!!,
                       userEmail = binding.etEmail.text.toString().trim()
                   )
                   findNavController().navigate(action)
               }
               is Resource.Error -> {
                   showLoading(false)
                   showError("Имя пользователя уже занято")
               }
               else -> {}
           }
       }
    }
    
    private fun triggerRecaptcha() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = recaptchaClient.execute(RecaptchaAction.custom("register")).getOrThrow()
                handleRecaptchaSuccess(token)
            } catch (e: Exception) {
                showError("Не удалось пройти проверку reCAPTCHA. Попробуйте снова.")
                Timber.e(e, "reCAPTCHA error")
            }
        }
    }

    private fun handleRecaptchaSuccess(recaptchaToken: String) {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (isRegisterMode) {
            viewModel.startRegistration(email, password, recaptchaToken)
        } else {
            viewModel.performEmailLogin(email, password)
        }
    }

    private fun handleContinue() {
        if (isRegisterMode && currentStep == RegistrationStep.USERNAME) {
            val username = binding.etUsername.text.toString().trim()
            if (username.length >= 3) {
                viewModel.setUsername(username)
            } else {
                showError("Имя пользователя должно быть не менее 3 символов")
            }
        } else {
            triggerRecaptcha()
        }
    }

    private fun updateUiForMode(checkedId: Int) {
        isRegisterMode = checkedId == R.id.btn_register_toggle
        binding.groupRegisterFields.visibility = if (isRegisterMode) View.VISIBLE else View.GONE
        binding.btnForgotPassword.visibility = if (isRegisterMode) View.GONE else View.VISIBLE
        
        if (isRegisterMode) {
            currentStep = RegistrationStep.EMAIL_PASSWORD
            updateUiForRegistrationStep()
        } else {
            binding.tilConfirmPassword.error = null
            binding.tvEmailTitle.text = "Введи свои данные"
            binding.tilUsername.visibility = View.GONE
            binding.tilEmail.visibility = View.VISIBLE
            binding.tilPassword.visibility = View.VISIBLE
            binding.tilConfirmPassword.visibility = View.GONE
            binding.layoutPasswordRequirements.visibility = View.GONE
            validateEmail(binding.etEmail.text.toString())
        }
    }

    private fun updateUiForRegistrationStep() {
        val isUsernameStep = currentStep == RegistrationStep.USERNAME

        binding.toggleButtonGroup.visibility = if (isUsernameStep) View.GONE else View.VISIBLE
        binding.tilEmail.visibility = if (isUsernameStep) View.GONE else View.VISIBLE
        binding.tilPassword.visibility = if (isUsernameStep) View.GONE else View.VISIBLE
        binding.tilConfirmPassword.visibility = if (isUsernameStep) View.GONE else View.VISIBLE
        binding.layoutPasswordRequirements.visibility = if (isUsernameStep) View.GONE else View.VISIBLE
        binding.tilUsername.visibility = if (isUsernameStep) View.VISIBLE else View.GONE
        binding.tvEmailTitle.text = if (isUsernameStep) "Шаг 2: Введите имя пользователя" else "Шаг 1: Введите email и пароль"
        binding.btnContinue.text = if (isUsernameStep) "Завершить" else "Продолжить"
        updateContinueButtonState()
    }

    private fun validateEmail(email: String) {
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        updateContinueButtonState()
    }

    private fun updateContinueButtonState() {
        if (!isAdded) return
        clearInputErrors()

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

        if (isRegisterMode) {
            if (currentStep == RegistrationStep.EMAIL_PASSWORD) {
                val confirmPassword = binding.etConfirmPassword.text.toString().trim()

                val isLengthOk = password.length >= 8
                val isUpperOk = password.any { it.isUpperCase() }
                val isNumberOk = password.any { it.isDigit() }
                val isSpecialOk = password.any { !it.isLetterOrDigit() }
                updateRequirementView(binding.reqLength, isLengthOk, "Более 8 символов")
                updateRequirementView(binding.reqUppercase, isUpperOk, "Минимум 1 заглавная буква")
                updateRequirementView(binding.reqNumber, isNumberOk, "Минимум 1 число")
                updateRequirementView(binding.reqSpecialChar, isSpecialOk, "Минимум 1 специальный знак")

                val passwordsMatch = password.isNotEmpty() && password == confirmPassword
                if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                    binding.tilConfirmPassword.error = "Пароли не совпадают"
                }

                binding.btnContinue.isEnabled = isEmailValid && isLengthOk && isUpperOk && isNumberOk && isSpecialOk && passwordsMatch && isRecaptchaInitialized
            } else {
                val username = binding.etUsername.text.toString().trim()
                binding.btnContinue.isEnabled = username.length >= 3
            }
        } else { 
            val isPasswordValid = password.length >= 8
            binding.btnContinue.isEnabled = isEmailValid && isPasswordValid && isRecaptchaInitialized
        }
    }

    private fun showLoading(isLoading: Boolean, message: String = "Загрузка...") {
        if(isLoading) Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        binding.btnContinue.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        showLoading(false)
    }

    private fun showLoginSuggestionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Email уже зарегистрирован")
            .setMessage("Пользователь с таким email уже существует. Хотите войти?")
            .setPositiveButton("Войти") { _, _ ->
                binding.toggleButtonGroup.check(R.id.btn_login_toggle)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun clearInputErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
    }

    private fun updateRequirementView(textView: TextView, isValid: Boolean, text: String) {
        val context = context ?: return
        val color = if (isValid) R.color.correct_answer_green else android.R.color.darker_gray
        val prefix = if (isValid) "✓ " else ""
        textView.text = prefix + text
        textView.setTextColor(ContextCompat.getColor(context, color))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 