package io.github.chris013.practice.task22;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.chris013.practice.task22.RecordingMailer.SentMail;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** State-based: we check what the users, the token store and the mailbox look like afterwards. */
class PasswordResetServiceTest {

    private static final EmailAddress EMAIL = new EmailAddress("anna@example.com");
    private static final UserId ANNA = new UserId("anna");
    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");
    private static final Duration LIFETIME = Duration.ofMinutes(30);
    private static final String OLD_PASSWORD = "old-password-123";
    private static final String NEW_PASSWORD = "new-password-456";

    private final InMemoryUsers users = new InMemoryUsers();
    private final InMemoryResetTokens tokens = new InMemoryResetTokens();
    private final RecordingMailer mailer = new RecordingMailer();
    private final MutableClock clock = new MutableClock(NOW);
    private final Sha256PasswordHasher hasher = new Sha256PasswordHasher();
    private final AtomicInteger issued = new AtomicInteger();
    private final PasswordResetService service = serviceWith(mailer);

    @BeforeEach
    void anna_has_an_account() {
        users.save(new User(ANNA, EMAIL, hasher.hash(OLD_PASSWORD)));
    }

    /** A stub: every call hands out the next predictable token, so tests can name them. */
    private String nextToken() {
        return "token-" + issued.incrementAndGet();
    }

    private PasswordResetService serviceWith(Mailer mailerToUse) {
        return new PasswordResetService(users, tokens, this::nextToken, hasher, mailerToUse, clock);
    }

    private void requestReset() {
        service.requestReset(EMAIL);
    }

    private boolean passwordIs(String raw) {
        return hasher.matches(raw, users.findById(ANNA).orElseThrow().passwordHash());
    }

    @Nested
    class RequestingAReset {

        @Test
        void unknown_email_sends_nothing_and_stores_nothing() {
            // Mockito is the clearer choice here, not the only one: verifyNoInteractions states "nothing was sent"
            // in one line and keeps failing if Mailer later gains a second method. An empty recording-mailer
            // list would prove the same today.
            Mailer outbox = mock(Mailer.class);

            serviceWith(outbox).requestReset(new EmailAddress("nobody@example.com"));

            verifyNoInteractions(outbox);
            assertThat(tokens.count()).isZero();
        }

        @Test
        void mails_a_link_that_contains_the_token() {
            requestReset();

            assertThat(mailer.sent()).hasSize(1);
            SentMail mail = mailer.sent().getFirst();
            assertThat(mail.to()).isEqualTo(EMAIL);
            assertThat(mail.subject()).isEqualTo("Reset your password");
            assertThat(mail.body()).contains("https://shop.example/reset?token=token-1");
        }

        @Test
        void stores_only_the_hash_of_the_token_with_a_30_minute_lifetime() {
            requestReset();

            ResetToken stored = tokens.findByUser(ANNA).getFirst();
            // SHA-256 in lower-case hex: 64 characters, and the raw token is nowhere to be seen.
            assertThat(stored.tokenHash()).matches("[0-9a-f]{64}").doesNotContain("token-1");
            assertThat(stored.userId()).isEqualTo(ANNA);
            assertThat(stored.expiresAt()).isEqualTo(NOW.plus(LIFETIME));
        }

        @Test
        void a_fourth_request_is_ignored_while_three_tokens_are_outstanding() {
            requestReset();
            requestReset();
            requestReset();

            requestReset();

            assertThat(mailer.sent()).hasSize(3);
            assertThat(tokens.findByUser(ANNA)).hasSize(3);
        }

        @Test
        void the_limit_still_holds_one_second_before_the_oldest_token_expires() {
            requestReset();
            requestReset();
            requestReset();

            clock.advanceBy(LIFETIME.minusSeconds(1));
            requestReset();

            assertThat(mailer.sent()).hasSize(3);
        }

        @Test
        void a_new_token_is_allowed_again_once_the_old_ones_have_expired() {
            requestReset();
            requestReset();
            requestReset();
            clock.advanceBy(LIFETIME);

            requestReset();

            assertThat(mailer.sent()).hasSize(4);
        }
    }

    @Nested
    class Construction {

        @Test
        void refuses_null_collaborators() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new PasswordResetService(
                            null, tokens, PasswordResetServiceTest.this::nextToken, hasher, mailer, clock))
                    .withMessage("users");
        }
    }

    @Nested
    class ResettingThePassword {

        @BeforeEach
        void request_a_token() {
            requestReset();
        }

        @Test
        void a_valid_token_changes_the_password() {
            ResetResult result = service.resetPassword("token-1", NEW_PASSWORD);

            assertThat(result).isEqualTo(new PasswordChanged());
            assertThat(passwordIs(NEW_PASSWORD)).isTrue();
            assertThat(passwordIs(OLD_PASSWORD)).isFalse();
        }

        @Test
        void a_token_works_only_once() {
            service.resetPassword("token-1", NEW_PASSWORD);

            ResetResult second = service.resetPassword("token-1", "another-password-789");

            assertThat(second).isEqualTo(new InvalidToken());
            assertThat(passwordIs(NEW_PASSWORD)).isTrue();
        }

        @Test
        void using_one_token_invalidates_the_other_outstanding_tokens() {
            requestReset();

            service.resetPassword("token-1", NEW_PASSWORD);

            assertThat(service.resetPassword("token-2", "another-password-789")).isEqualTo(new InvalidToken());
            assertThat(tokens.findByUser(ANNA)).isEmpty();
        }

        @Test
        void a_token_is_still_valid_one_second_before_it_expires() {
            clock.advanceBy(LIFETIME.minusSeconds(1));

            assertThat(service.resetPassword("token-1", NEW_PASSWORD)).isEqualTo(new PasswordChanged());
        }

        @Test
        void a_token_is_rejected_exactly_when_it_expires_and_is_removed() {
            clock.advanceBy(LIFETIME);

            assertThat(service.resetPassword("token-1", NEW_PASSWORD)).isEqualTo(new ExpiredToken());
            assertThat(tokens.findByUser(ANNA)).isEmpty();
            assertThat(passwordIs(OLD_PASSWORD)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "a", "elevenchars"})
        void a_password_under_12_characters_is_refused_and_the_token_stays_valid(String weak) {
            ResetResult result = service.resetPassword("token-1", weak);

            assertThat(result).isEqualTo(new PasswordTooWeak("at least 12 characters"));
            assertThat(service.resetPassword("token-1", NEW_PASSWORD)).isEqualTo(new PasswordChanged());
        }

        @Test
        void exactly_12_characters_is_enough() {
            assertThat(service.resetPassword("token-1", "twelve-chars")).isEqualTo(new PasswordChanged());
        }

        @Test
        void an_unknown_token_is_rejected_and_changes_nothing() {
            ResetResult result = service.resetPassword("guessed-token", NEW_PASSWORD);

            assertThat(result).isEqualTo(new InvalidToken());
            assertThat(passwordIs(OLD_PASSWORD)).isTrue();
            assertThat(tokens.findByUser(ANNA)).hasSize(1);
        }
    }
}
