package com.fmsys.snapdrop.utils;

public class StateHandler {
    private boolean currentlyOffline = false;
    private boolean currentlyLoading = true;
    private boolean currentlyStarting = true;

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
}
