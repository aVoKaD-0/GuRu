package com.ruege.mobile;

import com.ruege.mobile.databinding.ActivityMainBinding;
import androidx.appcompat.app.AppCompatActivity;
import com.ruege.mobile.utils.FlowJavaHelper;
import dagger.hilt.android.AndroidEntryPoint;
import com.ruege.mobile.auth.AuthEventBus;
import androidx.lifecycle.LifecycleOwnerKt;
import com.ruege.mobile.auth.AuthEvent;
import android.content.Intent;
import android.widget.Toast;
import javax.inject.Inject;
import android.os.Bundle;
import timber.log.Timber;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Inject
    AuthEventBus authEventBus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            Timber.d("MainActivity onCreate");
            observeAuthEvents();
        } catch (Exception e) {
            Timber.e(e, "Error in MainActivity onCreate");
            Toast.makeText(this, "Произошла ошибка при запуске. Пожалуйста, перезапустите приложение.", Toast.LENGTH_LONG).show();
        }
    }

    private void observeAuthEvents() {
        FlowJavaHelper.collectInScope(
            authEventBus.getEvents(),
            LifecycleOwnerKt.getLifecycleScope(this),
            event -> {
                if (event instanceof AuthEvent.SessionExpired) {
                    Timber.w("Session expired event received. Forcing logout.");
                    Toast.makeText(this, "Сессия истекла. Пожалуйста, войдите снова.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, com.ruege.mobile.ui.activity.LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        );
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        Timber.d("MainActivity onDestroy");
    }
}