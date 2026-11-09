package io.github.chris013.practice.task21;

/** A whole-number constant. Integers only, on purpose: arithmetic stays exact. */
public record Num(long value) implements Expr {}
