package io.github.chris013.practice.task12;

/**
 * What a review step returns: either the document still needs more approvals, or it is now approved.
 * A caller that reviews a document must handle both cases.
 */
public sealed interface ReviewOutcome permits InReview, Approved {}
