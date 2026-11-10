package io.github.chris013.practice.task27;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;

/**
 * Opens the input for {@link CatalogImporter#importFrom(Path)}. Production code opens UTF-8 files; the tests
 * pass their own opener, which is a seam: a place where behaviour can change without editing the importer.
 */
@FunctionalInterface
interface FileOpener {

    Reader open(Path file) throws IOException;
}
