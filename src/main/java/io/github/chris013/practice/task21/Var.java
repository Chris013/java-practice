package io.github.chris013.practice.task21;

import java.util.Objects;
import java.util.regex.Pattern;

/** A named variable such as {@code x} or {@code vatRate2}. */
public record Var(String name) implements Expr {

    private static final Pattern NAME = Pattern.compile("[a-z][a-zA-Z0-9]*");

    public Var {
        Objects.requireNonNull(name, "name");
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid variable name: " + name);
        }
    }
}
