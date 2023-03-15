package com.fmsys.snapdrop;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.fmsys.snapdrop.databinding.FragmentOnboarding1Binding;
import com.fmsys.snapdrop.utils.ViewUtils;

public class OnboardingFragment1 extends Fragment {
    public OnboardingFragment1() {
        super(R.layout.fragment_onboarding_1);
    }

    @Override
    public void onViewCreated(final @NonNull View view, final Bundle savedInstanceState) {
        final FragmentOnboarding1Binding binding = FragmentOnboarding1Binding.bind(view);
        final OnboardingViewModel viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);


        final AnimatedVectorDrawableCompat loadAnimationDrawable = AnimatedVectorDrawableCompat.create(requireContext(), R.drawable.snapdrop_anim);
        loadAnimationDrawable.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
            @Override
            public void onAnimationEnd(final Drawable drawable) {
                binding.appName.animate().alpha(1).translationY(ViewUtils.dpToPixel(20)).setDuration(1000).setInterpolator(new DecelerateInterpolator()).start();
                binding.slogan.animate().setStartDelay(750).alpha(1).setInterpolator(new DecelerateInterpolator()).start();
                binding.continueButton.animate().setStartDelay(750).alpha(1).setInterpolator(new DecelerateInterpolator()).start();

            }
        });
        view.postDelayed(() -> {
            binding.appIcon.setImageDrawable(loadAnimationDrawable);
            loadAnimationDrawable.start();
        }, 500);

        binding.continueButton.setOnClickListener(v -> viewModel.launchFragment(OnboardingFragment2.class));
    }
}
