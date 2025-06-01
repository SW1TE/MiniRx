package org.example;

import org.example.core.Observable;
import org.example.core.Observer;
import org.example.schedulers.IOThreadScheduler;
import org.example.schedulers.SingleThreadScheduler;

public class Main {
    public static void main(String[] args) {
        Observable.<String>create(emitter -> {
                    emitter.onNext("Hello");
                    emitter.onNext("World");
                    emitter.onComplete();
                })
                .subscribeOn(new IOThreadScheduler())
                .filter(s -> s.length() > 3)
                .map(String::toUpperCase)
                .observeOn(new SingleThreadScheduler())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onNext(String s) {
                        System.out.println(s + " on " + Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("Error: " + t.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("Done!");
                    }
                });
    }
}