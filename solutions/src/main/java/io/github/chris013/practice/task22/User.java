package io.github.chris013.practice.task22;

import java.util.Objects;

/** A customer account. The password is never stored in clear text, only its hash. */
public record User(UserId id, EmailAddress email, String passwordHash) {

    public User {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(passwordHash, "passwordHash");
    }
}
