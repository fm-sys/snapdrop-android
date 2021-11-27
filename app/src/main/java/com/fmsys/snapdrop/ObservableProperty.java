package com.fmsys.snapdrop;

import androidx.core.util.Consumer;

import java.util.Objects;

public class ObservableProperty<T> {
    private T value;
    private Consumer<T> listener;

    public ObservableProperty(final T value) {
        this.value = value;
    }

    public void set(final T value) {
        if (Objects.equals(this.value, value)) {
            return;
        }
        this.value = value;
        if (listener != null) {
            listener.accept(value);
        }
    }

    public T get() {
        return value;
    }

    public void setOnChangedListener(final Consumer<T> listener) {
        this.listener = listener;
    }

}
