package io.github.chris013.practice.task27;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects valid products and passes them to the repository in batches of exactly {@code batchSize}. Only the
 * last batch may be smaller. It never calls the repository with an empty list.
 */
final class BatchWriter {

    private final ProductRepository repository;
    private final int batchSize;
    private final List<Product> pending = new ArrayList<>();
    private int saved;

    BatchWriter(ProductRepository repository, int batchSize) {
        this.repository = repository;
        this.batchSize = batchSize;
    }

    void add(Product product) {
        pending.add(product);
        if (pending.size() == batchSize) {
            flush();
        }
    }

    /** Saves the pending products, if there are any. The count only grows after the repository has succeeded. */
    void flush() {
        if (pending.isEmpty()) {
            return;
        }
        // A copy, so the repository cannot keep a list that we are about to clear.
        repository.saveAll(List.copyOf(pending));
        saved += pending.size();
        pending.clear();
    }

    int saved() {
        return saved;
    }
}
