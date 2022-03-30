package com.fmsys.snapdrop;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class OpenUrlActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getIntent().getStringExtra(Intent.EXTRA_TEXT))));
        finish();
    }
}
