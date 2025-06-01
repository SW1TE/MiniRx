package org.example.core;

public interface Emitter<T> {
    void onNext(T value);
    void onError(Throwable error);
    void onComplete();
}