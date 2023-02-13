package com.fmsys.snapdrop;

import android.view.View;

import androidx.annotation.NonNull;

import com.mikepenz.aboutlibraries.LibsConfiguration;

/**
 * Default listener implementing all methods we do not really need
 */
public abstract class AboutLibrariesListener implements LibsConfiguration.LibsListener {
    @Override
    public boolean onLibraryAuthorClicked(final @NonNull View view, final @NonNull com.mikepenz.aboutlibraries.entity.Library library) {
        return false;
    }

    @Override
    public boolean onLibraryContentClicked(final @NonNull View view, final @NonNull com.mikepenz.aboutlibraries.entity.Library library) {
        return false;
    }

    @Override
    public boolean onLibraryBottomClicked(final @NonNull View view, final @NonNull com.mikepenz.aboutlibraries.entity.Library library) {
        return false;
    }

    @Override
    public boolean onLibraryAuthorLongClicked(final @NonNull View view, final @NonNull com.mikepenz.aboutlibraries.entity.Library library) {
        return false;
    }

    @Override
    public boolean onLibraryContentLongClicked(final @NonNull View view, final @NonNull com.mikepenz.aboutlibraries.entity.Library library) {
        return false;
    }

    @Override
    public boolean onLibraryBottomLongClicked(final @NonNull View view, final @NonNull com.mikepenz.aboutlibraries.entity.Library library) {
        return false;
    }
}
