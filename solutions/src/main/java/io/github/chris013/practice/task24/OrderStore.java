package io.github.chris013.practice.task24;

import java.math.BigDecimal;

/** Port: the order table in the database. */
public interface OrderStore {

    void save(String orderId, String customerId, BigDecimal total, String status);
}
