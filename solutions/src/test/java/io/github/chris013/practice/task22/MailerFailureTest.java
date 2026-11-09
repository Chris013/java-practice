package io.github.chris013.practice.task22;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pins what happens when the mail system is down. The current behaviour is deliberate to discuss:
 * the exception reaches the caller, and the token that was already stored stays in the store.
 */
@ExtendWith(MockitoExtension.class)
class MailerFailureTest {

    private static final EmailAddress EMAIL = new EmailAddress("anna@example.com");
    private static final UserId ANNA = new UserId("anna");

    @Mock
    private Mailer mailer;

    @Test
    void a_failing_mailer_propagates_and_leaves_the_stored_token_behind() {
        InMemoryUsers users = new InMemoryUsers();
        InMemoryResetTokens tokens = new InMemoryResetTokens();
        users.save(new User(ANNA, EMAIL, "some-hash"));
        Clock clock = Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC);
        PasswordResetService service = new PasswordResetService(
                users, tokens, () -> "token-1", new Sha256PasswordHasher(), mailer, clock);
        doThrow(new IllegalStateException("SMTP server is down")).when(mailer).send(any(), any(), any());

        assertThatThrownBy(() -> service.requestReset(EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("SMTP server is down");

        // The user never got the mail, but the token exists and counts against the limit of 3 for 30 minutes.
        assertThat(tokens.findByUser(ANNA)).hasSize(1);
    }
}
