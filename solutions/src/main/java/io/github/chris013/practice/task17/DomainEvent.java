package io.github.chris013.practice.task17;

/** Given. The events that modules of the webshop publish to each other. */
public sealed interface DomainEvent permits CustomerRegistered, OrderPlaced {}
