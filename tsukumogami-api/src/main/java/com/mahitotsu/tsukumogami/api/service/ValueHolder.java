package com.mahitotsu.tsukumogami.api.service;

public class ValueHolder<T> {

    public ValueHolder(final T initValue) {
        this.value = initValue;
    }
    
    private T value;

    public T get() {
        return this.value;
    }

    public void set(final T value) {
        this.value = value;
    }
}
