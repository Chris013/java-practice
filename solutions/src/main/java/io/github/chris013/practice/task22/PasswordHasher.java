package io.github.chris013.practice.task22;

/** Turns a password into a stored hash and checks a password against one. */
public interface PasswordHasher {

    String hash(String raw);

    boolean matches(String raw, String hash);
}
