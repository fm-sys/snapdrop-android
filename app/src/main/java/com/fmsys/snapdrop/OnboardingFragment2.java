package com.fmsys.snapdrop;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fmsys.snapdrop.databinding.FragmentOnboarding2Binding;
import com.fmsys.snapdrop.utils.Link;
import com.fmsys.snapdrop.utils.NetworkUtils;
import com.fmsys.snapdrop.utils.ViewUtils;

public class OnboardingFragment2 extends Fragment {

    String url = "https://pairdrop.net";

    public OnboardingFragment2() {
        super(R.layout.fragment_onboarding_2);
    }

    @Override
    public void onViewCreated(final @NonNull View view, final Bundle savedInstanceState) {
        final FragmentOnboarding2Binding binding = FragmentOnboarding2Binding.bind(view);
        final OnboardingViewModel viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        binding.card1.setChecked(true);
        binding.card1.setOnClickListener(v -> {
            url = "https://pairdrop.net";
            binding.card1.setChecked(true);
            binding.card2.setChecked(false);
            binding.card3.setChecked(false);
        });
        binding.card2.setOnClickListener(v -> {
            url = "https://snapdrop.net";
            binding.card1.setChecked(false);
            binding.card2.setChecked(true);
            binding.card3.setChecked(false);
        });
        binding.card3.setOnClickListener(v -> {
            ViewUtils.showEditTextWithResetPossibility(this, "Custom URL", null, null, Link.bind("https://github.com/RobinLinus/snapdrop/blob/master/docs/faq.md#inofficial-instances", R.string.baseurl_unofficial_instances), url -> {
                if (url == null) {
                    binding.customUrl.setText(R.string.onboarding_server_custom);
                    if (binding.card3.isChecked()) {
                        binding.card1.callOnClick();
                    }
                    return;
                }

                NetworkUtils.checkInstance(this, url, result -> {
                    if (result) {
                        this.url = url;
                        binding.customUrl.setText(url);
                        binding.card1.setChecked(false);
                        binding.card2.setChecked(false);
                        binding.card3.setChecked(true);
                    }
                });
            });
        });

        binding.continueButton.setOnClickListener(v -> {
            viewModel.url(url);
            viewModel.launchFragment(OnboardingFragment3.class);
        });
    }
}
