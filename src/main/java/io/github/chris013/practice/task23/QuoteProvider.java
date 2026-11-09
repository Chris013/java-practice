package io.github.chris013.practice.task23;

import java.util.List;

/**
 * An airline or partner API that answers quote requests.
 *
 * <p>The call is a plain blocking call. That is fine on a virtual thread. Well-behaved implementations
 * respond to {@link Thread#interrupt()} by giving up quickly, because the aggregator uses interruption
 * to cancel a provider that is too slow.
 */
public interface QuoteProvider {

    String name();

    /** @throws Exception anything: network errors, HTTP 503, bad data. The aggregator reports it, it does not crash. */
    List<Quote> quotesFor(TripRequest request) throws Exception;
}
