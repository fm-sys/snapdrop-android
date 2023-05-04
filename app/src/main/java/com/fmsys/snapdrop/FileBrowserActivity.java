package com.fmsys.snapdrop;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FileBrowserActivity extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> viewFileResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> openStorageFolder());

    private final ActivityResultLauncher<Intent> fileBrowserResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getData() != null) {
            try {
                viewFileResultLauncher.launch(new Intent(Intent.ACTION_VIEW).setData(result.getData().getData()));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.err_no_app, Toast.LENGTH_SHORT).show();
            }
        } else {
            finish();
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openStorageFolder();
    }

    private void openStorageFolder() {
        if (MainActivity.isCustomSaveLocation() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, MainActivity.getSaveLocation().getUri());
            fileBrowserResultLauncher.launch(i);
        } else {
            startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
            finish();
        }
    }
}
