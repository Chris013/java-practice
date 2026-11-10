package io.github.chris013.practice.task27;

import java.util.ArrayList;
import java.util.List;

/** A hand-written fake: records every batch it is given, and can be told to fail. */
final class RecordingRepository implements ProductRepository {

    private final List<List<Product>> batches = new ArrayList<>();
    private RuntimeException failure;

    void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    @Override
    public void saveAll(List<Product> batch) {
        if (failure != null) {
            throw failure;
        }
        batches.add(List.copyOf(batch));
    }

    List<Integer> batchSizes() {
        return batches.stream().map(List::size).toList();
    }

    List<Product> allProducts() {
        return batches.stream().flatMap(List::stream).toList();
    }
}
