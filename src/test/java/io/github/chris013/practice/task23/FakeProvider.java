package io.github.chris013.practice.task23;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Hand-written provider fakes. Each factory method describes one behaviour. */
final class FakeProvider implements QuoteProvider {

    @FunctionalInterface
    interface Behaviour {
        List<Quote> respond(TripRequest request) throws Exception;
    }

    private final String name;
    private final Behaviour behaviour;

    private FakeProvider(String name, Behaviour behaviour) {
        this.name = name;
        this.behaviour = behaviour;
    }

    static Quote quote(String provider, String price, String flight) {
        return new Quote(provider, new BigDecimal(price), flight);
    }

    static QuoteProvider answering(String name, Quote... quotes) {
        List<Quote> answer = List.of(quotes);
        return new FakeProvider(name, request -> answer);
    }

    static QuoteProvider failing(String name) {
        return failingWith(name, new IOException("503"));
    }

    static QuoteProvider failingWith(String name, Exception failure) {
        return new FakeProvider(name, request -> {
            throw failure;
        });
    }

    static QuoteProvider throwingError(String name) {
        return new FakeProvider(name, request -> {
            throw new StackOverflowError("test");
        });
    }

    /** Answers after a short delay. Keep the delay well below the budget used in the test. */
    static QuoteProvider slowAnswering(String name, Duration delay, Quote... quotes) {
        List<Quote> answer = List.of(quotes);
        return new FakeProvider(name, request -> {
            Thread.sleep(delay.toMillis());
            return answer;
        });
    }

    /** Waits on a barrier, so it only answers if all barrier parties are running at the same time. */
    static QuoteProvider answeringAfter(String name, CyclicBarrier barrier, Quote quote) {
        return new FakeProvider(name, request -> {
            barrier.await(1, TimeUnit.SECONDS);
            return List.of(quote);
        });
    }

    static Hanging hanging(String name) {
        return new Hanging(name);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<Quote> quotesFor(TripRequest request) throws Exception {
        return behaviour.respond(request);
    }

    /** Never answers. Records whether it was interrupted, so a test can check the cancellation. */
    static final class Hanging implements QuoteProvider {

        private final String name;
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch neverReleased = new CountDownLatch(1);
        private final CountDownLatch interruptedSignal = new CountDownLatch(1);
        private final AtomicBoolean interrupted = new AtomicBoolean();

        private Hanging(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<Quote> quotesFor(TripRequest request) throws InterruptedException {
            started.countDown();
            try {
                neverReleased.await();
                return List.of();
            } catch (InterruptedException e) {
                interrupted.set(true);
                interruptedSignal.countDown();
                throw e;
            }
        }

        /** Waits until this provider has started running, so the test does not race the start. */
        boolean awaitStarted(Duration timeout) throws InterruptedException {
            return started.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        /** Waits until the cancellation has reached this provider. The interrupt is asynchronous. */
        boolean awaitInterrupted(Duration timeout) throws InterruptedException {
            return interruptedSignal.await(timeout.toMillis(), TimeUnit.MILLISECONDS) && interrupted.get();
        }
    }
}
