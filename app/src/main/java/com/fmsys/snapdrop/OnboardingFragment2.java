package com.fmsys.snapdrop;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.fmsys.snapdrop.databinding.FragmentOnboarding2Binding;
import com.fmsys.snapdrop.utils.Link;
import com.fmsys.snapdrop.utils.NetworkUtils;
import com.fmsys.snapdrop.utils.ViewUtils;

public class OnboardingFragment2 extends Fragment {

    final MutableLiveData<String> tempUrl = new MutableLiveData<>();

    FragmentOnboarding2Binding binding;

    public OnboardingFragment2() {
        super(R.layout.fragment_onboarding_2);
    }

    @Override
    public void onViewCreated(final @NonNull View view, final Bundle savedInstanceState) {

        final OnboardingViewModel viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        binding = FragmentOnboarding2Binding.bind(view);

        tempUrl.setValue(PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(getString(R.string.pref_baseurl), "https://pairdrop.net"));

        binding.card1.setOnClickListener(v -> tempUrl.setValue("https://pairdrop.net"));
        binding.card2.setOnClickListener(v -> tempUrl.setValue("https://snapdrop.net"));
        binding.card3.setOnClickListener(v -> ViewUtils.showEditTextWithResetPossibility(this, "Custom URL", null, isCustomUrl() ? tempUrl.getValue() : null, Link.bind("https://github.com/RobinLinus/snapdrop/blob/master/docs/faq.md#inofficial-instances", R.string.baseurl_unofficial_instances), url -> {
            if (url == null) {
                binding.customUrl.setText(R.string.onboarding_server_custom);
                if (binding.card3.isChecked()) {
                    binding.card1.callOnClick();
                }
                return;
            }

            if (url.startsWith("!!")) { // hidden feature to force a different url
                tempUrl.setValue(url.substring("!!".length()));
            } else if (url.startsWith("http")) {
                NetworkUtils.checkInstance(this, url, result -> {
                    if (result) {
                        tempUrl.setValue(url);
                    }
                });
            } else {

                // do some magic in case user forgot to specify the protocol

                String mightBeHttpsUrl = "https://" + url;
                NetworkUtils.checkInstance(this, mightBeHttpsUrl, resultHttps -> {
                    if (resultHttps) {
                        tempUrl.setValue(mightBeHttpsUrl);
                    } else {
                        String mightBeHttpUrl = "http://" + url;
                        NetworkUtils.checkInstance(this, mightBeHttpUrl, resultHttp -> {
                            if (resultHttp) {
                                tempUrl.setValue(mightBeHttpUrl);
                            }
                        });
                    }
                });
            }
        }));

        tempUrl.observe(requireActivity(), url -> {
            binding.card1.setChecked("https://pairdrop.net".equals(url));
            binding.card2.setChecked("https://snapdrop.net".equals(url));

            binding.card3.setChecked(isCustomUrl());
            binding.customUrl.setText(isCustomUrl() ? url : getString(R.string.onboarding_server_custom));
        });

        binding.continueButton.setOnClickListener(v -> {
            viewModel.url(tempUrl.getValue());
            if (viewModel.isOnlyServerSelection()) {
                viewModel.finishActivity();
            } else {
                viewModel.launchFragment(OnboardingFragment3.class);
            }
        });
        binding.continueButton.requestFocus();
    }

    private boolean isCustomUrl() {
        return !binding.card1.isChecked() && !binding.card2.isChecked();
    }
}
