package org.example.operators;

import org.example.core.Observable;
import org.example.core.Observer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class FlatMapOperator<T, R> implements Observable.OnSubscribe<R> {
    private final Observable<T> source;
    private final Function<? super T, ? extends Observable<R>> mapper;

    public FlatMapOperator(Observable<T> source, Function<? super T, ? extends Observable<R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public void call(Observer<? super R> observer) {
        AtomicInteger activeSubscriptions = new AtomicInteger(1);

        source.subscribe(new Observer<T>() {
            @Override
            public void onNext(T item) {
                try {
                    activeSubscriptions.incrementAndGet();
                    Observable<R> observable = mapper.apply(item);
                    observable.subscribe(new Observer<R>() {
                        @Override
                        public void onNext(R item) {
                            observer.onNext(item);
                        }

                        @Override
                        public void onError(Throwable t) {
                            observer.onError(t);
                        }

                        @Override
                        public void onComplete() {
                            if (activeSubscriptions.decrementAndGet() == 0) {
                                observer.onComplete();
                            }
                        }
                    });
                } catch (Exception e) {
                    observer.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                observer.onError(t);
            }

            @Override
            public void onComplete() {
                if (activeSubscriptions.decrementAndGet() == 0) {
                    observer.onComplete();
                }
            }
        });
    }
}