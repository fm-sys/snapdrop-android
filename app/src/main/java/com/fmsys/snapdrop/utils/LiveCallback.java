package com.fmsys.snapdrop.utils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

public class LiveCallback {
    private final MutableLiveData<Void> liveData = new MutableLiveData<>();

    /**
     * Call the registered observers.
     */
    public void call() {
        liveData.setValue(null);
    }

    /**
     * Posts a task to a main thread to call the registered observers.
     */
    public void post() {
        liveData.postValue(null);
    }

    /**
     * Adds the given observer to the observers list within the lifespan of the given owner.
     * The events are dispatched on the main thread.
     * If a call was already triggered, the given action gets called directly.
     */
    public void observe(final @NonNull LifecycleOwner owner, final @NonNull Runnable action) {
        liveData.observe(owner, none -> action.run());
    }
}
