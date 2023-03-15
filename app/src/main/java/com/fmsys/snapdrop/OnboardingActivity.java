package com.fmsys.snapdrop;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;


public class OnboardingActivity extends AppCompatActivity {


    public OnboardingActivity() {
        super(R.layout.activity_onboarding);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final OnboardingViewModel viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);

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
            viewModel.launchFragment(OnboardingFragment1.class);
        }
    }


}
