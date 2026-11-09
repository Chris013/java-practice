package io.github.chris013.practice.task20.crm;

import java.util.Objects;

/** Something went wrong talking to the CRM. {@link #code()} is the CRM's error code, e.g. TIMEOUT or AUTH. */
public final class CrmException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String code;

    public CrmException(String code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public String code() {
        return code;
    }
}
