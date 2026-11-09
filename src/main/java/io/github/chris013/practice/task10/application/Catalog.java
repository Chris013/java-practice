package io.github.chris013.practice.task10.application;

import io.github.chris013.practice.task10.domain.CopyId;

/** Given. Outbound port: knows which physical copies the library owns. */
public interface Catalog {

    boolean exists(CopyId copyId);
}
