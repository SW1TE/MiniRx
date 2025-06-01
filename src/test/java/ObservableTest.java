import org.example.core.*;
import org.example.schedulers.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class ObservableTest {

    @Test
    public void testBasicFlow() throws InterruptedException {
        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable.<String>create(emitter -> {
            emitter.onNext("test");
            emitter.onComplete();
        }).subscribe(new Observer<String>() {
            @Override
            public void onNext(String item) {
                results.add(item);
            }

            @Override
            public void onError(Throwable t) {
                fail("Unexpected error: " + t.getMessage());
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(List.of("test"), results);
    }

    @Test
    public void testMapOperator() throws InterruptedException {
        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable.just("a", "b", "c")
                .map(String::toUpperCase)
                .subscribe(new Observer<>() {
                    @Override
                    public void onNext(String item) {
                        results.add(item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("Unexpected error: " + t.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(List.of("A", "B", "C"), results);
    }

    @Test
    public void testErrorHandling() {
        Observer<String> observer = mock(Observer.class);

        Observable.<String>create(emitter -> {
            emitter.onError(new RuntimeException("test error"));
        }).subscribe(observer);

        verify(observer).onError(any(RuntimeException.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onComplete();
    }

    @Test
    public void testSubscribeOn() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        String mainThread = Thread.currentThread().getName();
        String[] workerThread = new String[1];

        Observable.just(1)
                .subscribeOn(new IOThreadScheduler())
                .subscribe(new Observer<>() {
                    @Override
                    public void onNext(Integer item) {
                        workerThread[0] = Thread.currentThread().getName();
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("Unexpected error: " + t.getMessage());
                    }

                    @Override
                    public void onComplete() {}
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(workerThread[0]);
        assertNotEquals(mainThread, workerThread[0]);
    }

    @Test
    public void testObserveOn() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        String mainThread = Thread.currentThread().getName();
        String[] workerThread = new String[1];

        Observable.just(1)
                .observeOn(new SingleThreadScheduler())
                .subscribe(new Observer<>() {
                    @Override
                    public void onNext(Integer item) {
                        workerThread[0] = Thread.currentThread().getName();
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("Unexpected error: " + t.getMessage());
                    }

                    @Override
                    public void onComplete() {}
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(workerThread[0]);
        assertNotEquals(mainThread, workerThread[0]);
    }

    @Test
    public void testDisposable() {
        Disposable disposable = Observable.just(1)
                .subscribe(new Observer<>() {
                    @Override
                    public void onNext(Integer item) {}

                    @Override
                    public void onError(Throwable t) {
                        fail("Unexpected error: " + t.getMessage());
                    }

                    @Override
                    public void onComplete() {}
                });

        assertFalse(disposable.isDisposed());
        disposable.dispose();
        assertTrue(disposable.isDisposed());
    }

    @Test
    public void testFilterOperator() throws InterruptedException {
        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable.just("apple", "banana", "cherry")
                .filter(s -> s.length() > 5)
                .subscribe(new Observer<>() {
                    @Override
                    public void onNext(String item) {
                        results.add(item);
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("Unexpected error: " + t.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(List.of("banana", "cherry"), results);
    }

    @Test
    public void testFlatMapOperator() throws InterruptedException {
        List<String> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        Observable.just(1, 2, 3)
                .flatMap(i -> Observable.just(i * 10, i * 100))
                .subscribe(new Observer<>() {
                    @Override
                    public void onNext(Integer item) {
                        results.add(item.toString());
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("Unexpected error: " + t.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(results.containsAll(List.of("10", "100", "20", "200", "30", "300")));
    }

    @Test
    public void testThreadSwitching() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        String mainThread = Thread.currentThread().getName();
        String[] observeThread = new String[1];

        Observable.just(1)
                .subscribeOn(new IOThreadScheduler())
                .observeOn(new SingleThreadScheduler())
                .subscribe(new Observer<>() {
                    @Override
                    public void onNext(Integer item) {
                        observeThread[0] = Thread.currentThread().getName();
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("Unexpected error: " + t.getMessage());
                    }

                    @Override
                    public void onComplete() {}
                });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotNull(observeThread[0]);
        assertNotEquals(mainThread, observeThread[0]);
    }
}