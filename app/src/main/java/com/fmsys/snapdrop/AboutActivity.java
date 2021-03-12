package com.fmsys.snapdrop;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class AboutActivity extends AppCompatActivity {

    ViewPager2 viewPager2;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        viewPager2 = findViewById(R.id.pager);

        final ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager2.setAdapter(adapter);

        setResult(Activity.RESULT_OK);
    }

    public class ViewPagerAdapter extends FragmentStateAdapter {


        public ViewPagerAdapter(final @NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(final int position) {
            if (position == 1) {
                return new AboutFragment();
            }
            return new SettingsFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
