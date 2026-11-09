package io.github.chris013.practice.task12;

import java.util.Objects;

/** What the document says: the text itself and how sensitive it is. */
public record Content(String title, String body, Classification classification) {

    public Content {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(classification, "classification");
        if (title.isBlank()) {
            throw new IllegalArgumentException("A document needs a title");
        }
        if (body.isBlank()) {
            throw new IllegalArgumentException("A document needs a body");
        }
    }
}
