package io.github.chris013.practice.task03;

import java.math.BigDecimal;

/** One line of an order: a book, how many copies, and the price per copy at the time of ordering. */
public record OrderLine(Book book, int quantity, BigDecimal unitPrice) {}
