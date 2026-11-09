package io.github.chris013.practice.task22;

/** The token existed but its lifetime is over. The service deletes it when this happens. */
public record ExpiredToken() implements ResetResult {
}
