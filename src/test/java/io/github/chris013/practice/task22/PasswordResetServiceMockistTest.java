package io.github.chris013.practice.task22;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Every collaborator is a mock and every interaction is verified. It passes today, and it is also
 * the kind of test that breaks the moment someone refactors without changing behaviour.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceMockistTest {

    private static final EmailAddress EMAIL = new EmailAddress("anna@example.com");
    private static final UserId ANNA = new UserId("anna");
    private static final User USER = new User(ANNA, EMAIL, "old-hash");
    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");

    @Mock
    private UserRepository users;
    @Mock
    private ResetTokenStore tokens;
    @Mock
    private TokenGenerator generator;
    @Mock
    private PasswordHasher hasher;
    @Mock
    private Mailer mailer;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private PasswordResetService service;

    @BeforeEach
    void createService() {
        service = new PasswordResetService(users, tokens, generator, hasher, mailer, clock);
    }

    @Test
    void unknown_email_does_nothing() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.requestReset(EMAIL);

        verify(users).findByEmail(EMAIL);
        verifyNoInteractions(tokens, generator, mailer);
        verifyNoMoreInteractions(users);
    }

    @Test
    void stores_a_token_and_mails_the_link_in_this_order() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(USER));
        when(tokens.findByUser(ANNA)).thenReturn(List.of());
        when(generator.newToken()).thenReturn("raw-token");

        service.requestReset(EMAIL);

        ArgumentCaptor<ResetToken> stored = ArgumentCaptor.forClass(ResetToken.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        InOrder inOrder = inOrder(users, tokens, generator, mailer);
        inOrder.verify(users).findByEmail(EMAIL);
        inOrder.verify(tokens).findByUser(ANNA);
        inOrder.verify(generator).newToken();
        inOrder.verify(tokens).store(stored.capture());
        inOrder.verify(mailer).send(eq(EMAIL), eq("Reset your password"), body.capture());
        verifyNoMoreInteractions(users, tokens, generator, mailer);

        assertThat(stored.getValue().tokenHash()).isNotEqualTo("raw-token");
        assertThat(stored.getValue().expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        assertThat(body.getValue()).contains("https://shop.example/reset?token=raw-token");
    }

    @Test
    void a_user_with_three_outstanding_tokens_gets_no_new_token_and_no_mail() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(USER));
        when(tokens.findByUser(ANNA)).thenReturn(List.of(
                tokenExpiringIn(Duration.ofMinutes(5)),
                tokenExpiringIn(Duration.ofMinutes(10)),
                tokenExpiringIn(Duration.ofMinutes(20))));

        service.requestReset(EMAIL);

        InOrder inOrder = inOrder(users, tokens);
        inOrder.verify(users).findByEmail(EMAIL);
        inOrder.verify(tokens).findByUser(ANNA);
        verify(generator, never()).newToken();
        verify(tokens, never()).store(any());
        verifyNoInteractions(mailer, hasher);
        verifyNoMoreInteractions(users, tokens, generator);
    }

    @Test
    void a_valid_token_changes_the_password_and_deletes_all_tokens_of_the_user() {
        ResetToken used = new ResetToken("used-hash", ANNA, NOW.plusSeconds(60));
        ResetToken sibling = new ResetToken("sibling-hash", ANNA, NOW.plusSeconds(120));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(used));
        when(users.findById(ANNA)).thenReturn(Optional.of(USER));
        when(hasher.hash("correct horse battery")).thenReturn("new-hash");
        when(tokens.findByUser(ANNA)).thenReturn(List.of(used, sibling));

        ResetResult result = service.resetPassword("raw-token", "correct horse battery");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        InOrder inOrder = inOrder(tokens, users, hasher);
        inOrder.verify(tokens).findByTokenHash(anyString());
        inOrder.verify(users).findById(ANNA);
        inOrder.verify(hasher).hash("correct horse battery");
        inOrder.verify(users).save(saved.capture());
        inOrder.verify(tokens).findByUser(ANNA);
        inOrder.verify(tokens).delete("used-hash");
        inOrder.verify(tokens).delete("sibling-hash");
        verifyNoMoreInteractions(users, tokens, hasher);

        assertThat(result).isEqualTo(new PasswordChanged());
        assertThat(saved.getValue().passwordHash()).isEqualTo("new-hash");
    }

    @Test
    void an_expired_token_is_deleted_and_rejected() {
        ResetToken expired = new ResetToken("expired-hash", ANNA, NOW);
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        ResetResult result = service.resetPassword("raw-token", "correct horse battery");

        // The service hashes the raw token itself, so the test captures the hash it looked up.
        ArgumentCaptor<String> lookedUp = ArgumentCaptor.forClass(String.class);
        verify(tokens).findByTokenHash(lookedUp.capture());
        assertThat(result).isEqualTo(new ExpiredToken());
        verify(tokens).delete(lookedUp.getValue());
        verifyNoMoreInteractions(tokens);
        verifyNoInteractions(users, hasher, mailer, generator);
    }

    @Test
    void an_unknown_token_is_rejected_without_touching_users() {
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

        ResetResult result = service.resetPassword("guessed-token", "correct horse battery");

        assertThat(result).isEqualTo(new InvalidToken());
        verify(tokens).findByTokenHash(anyString());
        verifyNoMoreInteractions(tokens);
        verifyNoInteractions(users, hasher);
    }

    @Test
    void a_weak_password_is_refused_and_the_token_is_kept() {
        ResetToken valid = new ResetToken("valid-hash", ANNA, NOW.plusSeconds(60));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(valid));

        ResetResult result = service.resetPassword("raw-token", "short");

        assertThat(result).isEqualTo(new PasswordTooWeak("at least 12 characters"));
        verify(tokens).findByTokenHash(anyString());
        verifyNoMoreInteractions(tokens);
        verifyNoInteractions(users, hasher);
    }

    private static ResetToken tokenExpiringIn(Duration lifetime) {
        return new ResetToken("hash-" + lifetime.toMinutes(), ANNA, NOW.plus(lifetime));
    }
}
