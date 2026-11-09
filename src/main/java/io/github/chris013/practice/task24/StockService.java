package io.github.chris013.practice.task24;

/** Port: the warehouse system. Owned by the warehouse team. */
public interface StockService {

    int available(String sku);

    void reserve(String sku, int qty);

    void release(String sku, int qty);
}
