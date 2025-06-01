package org.example.core;

import org.example.operators.FilterOperator;
import org.example.operators.FlatMapOperator;
import org.example.operators.MapOperator;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Observable<T> {
    private final OnSubscribe<T> onSubscribe;

    public interface OnSubscribe<T> {
        void call(Observer<? super T> observer);
    }

    private Observable(OnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    public static <T> Observable<T> create(Consumer<Emitter<T>> emitterConsumer) {
        return new Observable<>(observer -> {
            emitterConsumer.accept(new Emitter<T>() {
                @Override
                public void onNext(T value) {
                    observer.onNext(value);
                }

                @Override
                public void onError(Throwable error) {
                    observer.onError(error);
                }

                @Override
                public void onComplete() {
                    observer.onComplete();
                }
            });
        });
    }

    // Остальные методы остаются без изменений
    public Disposable subscribe(Observer<? super T> observer) {
        onSubscribe.call(observer);
        return new Disposable() {
            private volatile boolean disposed = false;

            @Override
            public void dispose() {
                disposed = true;
            }

            @Override
            public boolean isDisposed() {
                return disposed;
            }
        };
    }

    public Observable<T> subscribeOn(Scheduler scheduler) {
        return new Observable<>(observer ->
                scheduler.execute(() -> onSubscribe.call(observer))
        );
    }

    public Observable<T> observeOn(Scheduler scheduler) {
        return new Observable<>(observer ->
                onSubscribe.call(new Observer<T>() {
                    @Override
                    public void onNext(T item) {
                        scheduler.execute(() -> observer.onNext(item));
                    }

                    @Override
                    public void onError(Throwable t) {
                        scheduler.execute(() -> observer.onError(t));
                    }

                    @Override
                    public void onComplete() {
                        scheduler.execute(() -> observer.onComplete());
                    }
                })
        );
    }

    public <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        return new Observable<>(new MapOperator<>(this, mapper));
    }

    public Observable<T> filter(Predicate<? super T> predicate) {
        return new Observable<>(new FilterOperator<>(this, predicate));
    }

    public <R> Observable<R> flatMap(Function<? super T, ? extends Observable<R>> mapper) {
        return new Observable<>(new FlatMapOperator<>(this, mapper));
    }

    @SafeVarargs
    public static <T> Observable<T> just(T... items) {
        return create(emitter -> {
            for (T item : items) {
                emitter.onNext(item);
            }
            emitter.onComplete();
        });
    }
}