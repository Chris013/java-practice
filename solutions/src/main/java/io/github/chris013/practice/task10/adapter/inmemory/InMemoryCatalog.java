package io.github.chris013.practice.task10.adapter.inmemory;

import io.github.chris013.practice.task10.application.Catalog;
import io.github.chris013.practice.task10.domain.CopyId;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps the owned copies in memory. Demo adapter and test double. */
public final class InMemoryCatalog implements Catalog {

    private final Set<CopyId> copies = ConcurrentHashMap.newKeySet();

    /** Registers copies the library owns. */
    public void add(CopyId... copyIds) {
        for (CopyId copyId : copyIds) {
            copies.add(Objects.requireNonNull(copyId, "copyId"));
        }
    }

    @Override
    public boolean exists(CopyId copyId) {
        Objects.requireNonNull(copyId, "copyId");
        return copies.contains(copyId);
    }
}
