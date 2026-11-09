package io.github.chris013.practice.task24;

import java.util.List;

/** What the checkout page sends. {@code couponCode} may be {@code null}. */
public record OrderRequest(String customerId, String email, String country, List<Item> items, String couponCode) {}
