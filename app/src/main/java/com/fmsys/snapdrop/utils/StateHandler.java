package com.fmsys.snapdrop.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class StateHandler {
    private boolean currentlyOffline = false;
    private boolean currentlyLoading = true;
    private boolean currentlyStarting = true;

    private final AtomicInteger loadProgress = new AtomicInteger();

    public boolean isCurrentlyOffline() {
        return currentlyOffline;
    }

    public void setCurrentlyOffline(final boolean currentlyOffline) {
        this.currentlyOffline = currentlyOffline;
    }

    public boolean isCurrentlyLoading() {
        return currentlyLoading;
    }

    public void setCurrentlyLoading(final boolean currentlyLoading) {
        this.currentlyLoading = currentlyLoading;

        if (!currentlyLoading) {
            currentlyStarting = false;
        }
    }

    public boolean isCurrentlyStarting() {
        return currentlyStarting;
    }

    public int getLoadProgress() {
        return loadProgress.get();
    }

    public void setLoadProgress(final int newProgress) {
        loadProgress.set(newProgress);
    }
}
