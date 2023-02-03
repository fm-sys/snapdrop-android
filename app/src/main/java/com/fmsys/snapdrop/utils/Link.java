package com.fmsys.snapdrop.utils;

import androidx.annotation.StringRes;

public class Link {

    public String url;

    public @StringRes
    int description;

    private Link(final String url, final @StringRes int description) {
        this.url = url;
        this.description = description;
    }

    public static Link bind(final String url, final @StringRes int description) {
        return new Link(url, description);
    }
}
