package com.fmsys.snapdrop;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        final TextView developer = findViewById(R.id.developer);
        developer.setMovementMethod(LinkMovementMethod.getInstance());

        final TextView version = findViewById(R.id.version);
        version.setText("v" + BuildConfig.VERSION_NAME);

    }
}
