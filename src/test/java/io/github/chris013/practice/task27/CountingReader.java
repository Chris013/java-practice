package io.github.chris013.practice.task27;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * A reader that counts the characters it hands out and records whether it was closed. It can also fail on
 * close, to test how the importer handles a failing close.
 */
final class CountingReader extends Reader {

    private final StringReader delegate;
    private final IOException closeFailure;
    private int charsRead;
    private boolean closed;

    CountingReader(String text) {
        this(text, null);
    }

    CountingReader(String text, IOException closeFailure) {
        this.delegate = new StringReader(text);
        this.closeFailure = closeFailure;
    }

    @Override
    public int read(char[] buffer, int offset, int length) throws IOException {
        int count = delegate.read(buffer, offset, length);
        if (count > 0) {
            charsRead += count;
        }
        return count;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        delegate.close();
        if (closeFailure != null) {
            throw closeFailure;
        }
    }

    int charsRead() {
        return charsRead;
    }

    boolean isClosed() {
        return closed;
    }
}
