package io.github.chris013.practice.task22;

import java.util.ArrayList;
import java.util.List;

/**
 * A hand-written spy: it records every message, and the test reads the record afterwards.
 * That is usually all a mailer needs. No Mockito required.
 */
final class RecordingMailer implements Mailer {

    record SentMail(EmailAddress to, String subject, String body) {
    }

    private final List<SentMail> sent = new ArrayList<>();

    @Override
    public void send(EmailAddress to, String subject, String body) {
        sent.add(new SentMail(to, subject, body));
    }

    List<SentMail> sent() {
        return List.copyOf(sent);
    }
}
