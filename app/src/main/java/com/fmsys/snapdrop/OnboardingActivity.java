package com.fmsys.snapdrop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;


public class OnboardingActivity extends AppCompatActivity {
    private static final String EXTRA_ONLY_SERVER_SELECTION = "extra_server";

    OnboardingViewModel viewModel;

    public OnboardingActivity() {
        super(R.layout.activity_onboarding);
    }

    public static void launchOnboarding(final Activity context) {
        context.startActivity(new Intent(context, OnboardingActivity.class));
        context.finish();
    }

    public static Intent getServerSelectionIntent(final Activity context) {
        final Intent intent = new Intent(context, OnboardingActivity.class);
        intent.putExtra(EXTRA_ONLY_SERVER_SELECTION, true);
        return intent;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);
        viewModel.setOnlyServerSelection(getIntent().getBooleanExtra(EXTRA_ONLY_SERVER_SELECTION, false));

        viewModel.getFragment().observe(this, fragment -> getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, fragment, null)
                .commit());

        viewModel.getUrl().observe(this, url -> PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean(getString(R.string.pref_first_use), false)
                .putString(getString(R.string.pref_baseurl), url)
                .apply());

        if (savedInstanceState == null) {
            if (viewModel.isOnlyServerSelection()) {
                viewModel.launchFragment(OnboardingFragment2.class);
            } else {
                viewModel.launchFragment(OnboardingFragment1.class);
            }
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(!viewModel.isOnlyServerSelection()) {
            @Override
            public void handleOnBackPressed() {
                // block back press while onboarding
            }
        });
    }
}
