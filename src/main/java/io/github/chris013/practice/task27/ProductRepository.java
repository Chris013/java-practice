package io.github.chris013.practice.task27;

import java.util.List;

/**
 * The database port. The importer calls {@link #saveAll} with one batch at a time, never with an empty list.
 * If a call throws, the exception reaches the caller of the importer unchanged.
 */
public interface ProductRepository {

    void saveAll(List<Product> batch);
}
