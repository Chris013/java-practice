package io.github.chris013.practice.task24;

import java.util.ArrayList;
import java.util.List;

/** Keeps every mail it was asked to send. */
final class RecordingMailer implements MailSender {

    record Sent(String to, String subject, String body) {}

    private final List<Sent> sent = new ArrayList<>();

    List<Sent> sent() {
        return List.copyOf(sent);
    }

    @Override
    public void send(String to, String subject, String body) {
        sent.add(new Sent(to, subject, body));
    }
}
