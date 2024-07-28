package com.fmsys.snapdrop;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class OnboardingViewModel extends ViewModel {
    private final MutableLiveData<Class<? extends Fragment>> fragment = new MutableLiveData<>();
    private final MutableLiveData<String> url = new MutableLiveData<>();
    private boolean onlyServerSelection;

    public void launchFragment(final Class<? extends Fragment> item) {
        fragment.setValue(item);
    }

    public LiveData<Class<? extends Fragment>> getFragment() {
        return fragment;
    }

    public void url(final String url) {
        this.url.setValue(url);
    }

    public LiveData<String> getUrl() {
        return url;
    }

    public boolean isOnlyServerSelection() {
        return onlyServerSelection;
    }

    public void setOnlyServerSelection(final boolean onlyServerSelection) {
        this.onlyServerSelection = onlyServerSelection;
    }

}
