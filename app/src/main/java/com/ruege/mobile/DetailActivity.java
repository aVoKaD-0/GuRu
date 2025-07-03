package com.ruege.mobile;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; 

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); 

        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        
        String title = getIntent().getStringExtra("content_title");
        String description = getIntent().getStringExtra("content_description");
        

        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title != null ? title : "Детали");
        }

        
        TextView descriptionTextView = findViewById(R.id.detail_description);
        if (descriptionTextView != null) {
            descriptionTextView.setText(description != null ? description : "Описание отсутствует.");
        }

        
        
    }

    
    @Override
    public boolean onSupportNavigateUp() {
        
        getOnBackPressedDispatcher().onBackPressed(); 
        return true;
    }
} 