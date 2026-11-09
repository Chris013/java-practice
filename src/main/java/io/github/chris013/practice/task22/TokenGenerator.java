package io.github.chris013.practice.task22;

/** Creates the random secrets that go into reset links. */
public interface TokenGenerator {

    /** A new, unguessable token. Must differ from every token this generator produced before. */
    String newToken();
}
