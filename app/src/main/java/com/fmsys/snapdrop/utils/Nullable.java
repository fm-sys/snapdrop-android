package com.fmsys.snapdrop.utils;

import androidx.core.util.Consumer;

public class Nullable<T> {

    private final T object;

    private Nullable(T object) {
        this.object = object;
    }

    public static <T> Nullable<T> of(T object) {
        return new Nullable<>(object);
    }

    public Nullable<T> ifNotNull(Consumer<T> consumer) {
        if (object != null) {
            consumer.accept(object);
        }
        return this;
    }

    public Nullable<T> ifNull(Runnable runnable) {
        if (object == null) {
            runnable.run();
        }
        return this;
    }
}
