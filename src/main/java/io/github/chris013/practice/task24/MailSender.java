package io.github.chris013.practice.task24;

/** Port: outgoing customer e-mail. */
public interface MailSender {

    void send(String to, String subject, String body);
}
