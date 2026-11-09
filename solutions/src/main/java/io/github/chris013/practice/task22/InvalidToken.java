package io.github.chris013.practice.task22;

/** No token matches. The answer is the same for "never existed" and "already used", on purpose. */
public record InvalidToken() implements ResetResult {
}
