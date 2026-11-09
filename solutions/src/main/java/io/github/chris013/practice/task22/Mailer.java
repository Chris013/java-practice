package io.github.chris013.practice.task22;

/**
 * Sends email to customers. Our own port: the real implementation talks to an SMTP server or a
 * mail API. Nothing in the domain depends on how the message gets there.
 */
public interface Mailer {

    /** @throws IllegalStateException if the message could not be handed to the mail system */
    void send(EmailAddress to, String subject, String body);
}
