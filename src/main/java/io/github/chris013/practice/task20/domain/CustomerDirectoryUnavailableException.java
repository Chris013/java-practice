package io.github.chris013.practice.task20.domain;

/** An infrastructure problem: the directory could not answer. The customer may well exist; try again later. */
public final class CustomerDirectoryUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CustomerDirectoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
