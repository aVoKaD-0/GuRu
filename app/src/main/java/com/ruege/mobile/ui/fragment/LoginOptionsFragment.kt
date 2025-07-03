package com.ruege.mobile.ui.fragment

import android.content.Intent
import android.os.Bundle
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.ruege.mobile.MainActivity
import com.ruege.mobile.R
import com.ruege.mobile.auth.GoogleAuthManager
import com.ruege.mobile.auth.SignInResult
import com.ruege.mobile.auth.TokenManager
import com.ruege.mobile.databinding.FragmentLoginOptionsBinding
import com.ruege.mobile.ui.viewmodel.LoginViewModel
import com.ruege.mobile.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginOptionsFragment : Fragment() {

    private var _binding: FragmentLoginOptionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by activityViewModels()

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var googleAuthManager: GoogleAuthManager

    private lateinit var classicSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        classicSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account.idToken
                    if (idToken != null) {
                        viewModel.performGoogleLogin(idToken)
                    } else {
                        showError("Не удалось получить Google ID токен.")
                    }
                } catch (e: ApiException) {
                    showError("Ошибка входа через Google: ${e.statusCode}")
                    Timber.d("Sign-in failed with status code: ${e.statusCode}", e)
                }
            } else {
                Timber.d("Google sign-in was cancelled by user.")
                showError("Вход через Google был отменен.")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnEmailPasswordSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_loginOptionsFragment_to_loginRegisterFragment)
        }
        binding.btnGoogleSignIn.setOnClickListener {
            lifecycleScope.launch { signInWithGoogle() }
        }
        binding.btnGoogleSignIn2.setOnClickListener {
             viewModel.performGoogleLoginWithId("testnoneakeytotpopkok")
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> showLoading(true)
                is Resource.Success -> {
                    showLoading(false)
                    resource.data?.let {
                        tokenManager.saveAccessToken(it.accessToken)
                        tokenManager.saveRefreshToken(it.refreshToken)
                        navigateToMain()
                    } ?: showError("Ошибка: получены пустые данные")
                }
                is Resource.Error -> {
                    showLoading(false)
                    showError(resource.message ?: "Неизвестная ошибка")
                }
                else -> showLoading(false)
            }
        }
    }

    private suspend fun signInWithGoogle() {
        showLoading(true)
        when (val result = googleAuthManager.signIn(requireActivity(), classicSignInLauncher)) {
            is SignInResult.Success -> viewModel.performGoogleLogin(result.idToken)
            is SignInResult.Error -> {
                showLoading(false)
                if (result.message != "Fallback to classic sign-in") {
                    showError("Ошибка входа через Google: ${result.message}")
                }
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            Toast.makeText(context, "Загрузка...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
    
    private fun navigateToMain() {
        activity?.let {
            val intent = Intent(it, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            it.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "LoginOptionsFragment"
    }
} 