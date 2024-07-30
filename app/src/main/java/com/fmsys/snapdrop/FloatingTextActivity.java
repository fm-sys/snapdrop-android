package com.fmsys.snapdrop;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentSanitizer;

public class FloatingTextActivity extends AppCompatActivity {

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = new  IntentSanitizer.Builder()
                .allowComponent(new ComponentName(getApplicationContext(), MainActivity.class))
                .allowAction(Intent.ACTION_PROCESS_TEXT)
                .allowType("text/plain")
                .allowExtra(Intent.EXTRA_PROCESS_TEXT, String.class)
                .allowExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, Boolean.class)
                .build()
                .sanitizeByFiltering(getIntent().setClass(this, MainActivity.class));
        if (!isTaskRoot()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
        finish();
    }
}
