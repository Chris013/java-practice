package io.github.chris013.practice.task25.application;

import io.github.chris013.practice.task25.domain.InvoiceId;

/** Given. Port: hands out new invoice ids. */
public interface InvoiceIds {

    InvoiceId next();
}
