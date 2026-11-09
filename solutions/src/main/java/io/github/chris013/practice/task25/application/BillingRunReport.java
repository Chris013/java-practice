package io.github.chris013.practice.task25.application;

/**
 * Given. What one run of the nightly job did.
 *
 * @param renewed               periods started, including FREE ones that bill nothing
 * @param ended                 subscriptions that ended because they were cancelled
 * @param invoiced              renewal invoices created in this run
 * @param paymentsFailed        charges that were declined in this run
 * @param skippedAlreadyInvoiced renewals whose invoice existed already, so nothing was created or charged
 */
public record BillingRunReport(int renewed, int ended, int invoiced, int paymentsFailed, int skippedAlreadyInvoiced) {}
