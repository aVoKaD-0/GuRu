package com.ruege.mobile.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ruege.mobile.R
import dagger.hilt.android.AndroidEntryPoint
import com.ruege.mobile.ui.fragment.EmailConfirmationFragment

@AndroidEntryPoint
class EmailConfirmationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_confirmation)

        if (savedInstanceState == null) {
            val fragment = EmailConfirmationFragment().apply {
                arguments = intent.extras
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
} 