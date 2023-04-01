package com.fmsys.snapdrop;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fmsys.snapdrop.databinding.FragmentOnboardingPermissionBinding;

public class OnboardingFragmentPermission extends Fragment {

    OnboardingViewModel viewModel;
    private final ActivityResultLauncher<String> permissionResult = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) {
            viewModel.launchFragment(OnboardingFragment2.class);
        }
    });

    public OnboardingFragmentPermission() {
        super(R.layout.fragment_onboarding_permission);
    }

    @Override
    public void onViewCreated(final @NonNull View view, final Bundle savedInstanceState) {
        final FragmentOnboardingPermissionBinding binding = FragmentOnboardingPermissionBinding.bind(view);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        binding.continueButton.setOnClickListener(v -> {
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                    && (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                permissionResult.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                viewModel.launchFragment(OnboardingFragment2.class);
            }
        });
        binding.continueButton.requestFocus();
    }
}
