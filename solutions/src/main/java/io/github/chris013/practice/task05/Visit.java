package io.github.chris013.practice.task05;

import java.time.LocalDate;

/** One check-in at the gym. {@code kind} is one of "GYM", "CLASS", "SAUNA". */
public record Visit(LocalDate date, String kind) {}
