package io.github.chris013.practice.task22;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Lets a customer who forgot the password choose a new one, by email.
 *
 * <p>Rules this class enforces (the tests check each of them):
 * <ul>
 *   <li>an unknown email gets the same silent result as a known one, so nobody can probe which emails have accounts;</li>
 *   <li>at most 3 unexpired tokens per user, so nobody can flood a mailbox;</li>
 *   <li>only the SHA-256 hash of a token is stored, so a leaked store is useless to an attacker;</li>
 *   <li>a token is valid for 30 minutes and works once;</li>
 *   <li>a successful reset invalidates every other token of that user.</li>
 * </ul>
 */
public final class PasswordResetService {

    static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    static final int MAX_OUTSTANDING_TOKENS = 3;
    static final int MINIMUM_PASSWORD_LENGTH = 12;
    static final String MAIL_SUBJECT = "Reset your password";
    static final String RESET_LINK_PREFIX = "https://shop.example/reset?token=";

    private static final long TOKEN_LIFETIME_MINUTES = TOKEN_LIFETIME.toMinutes();

    private final UserRepository users;
    private final ResetTokenStore tokens;
    private final TokenGenerator generator;
    private final PasswordHasher hasher;
    private final Mailer mailer;
    private final Clock clock;

    public PasswordResetService(UserRepository users, ResetTokenStore tokens, TokenGenerator generator,
            PasswordHasher hasher, Mailer mailer, Clock clock) {
        this.users = Objects.requireNonNull(users, "users");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.generator = Objects.requireNonNull(generator, "generator");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
        this.mailer = Objects.requireNonNull(mailer, "mailer");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Sends a reset link if the email belongs to an account and the rate limit allows it.
     * Returns normally in every case except a mail-system failure, which propagates to the caller.
     */
    public void requestReset(EmailAddress email) {
        Objects.requireNonNull(email, "email");
        Optional<User> user = users.findByEmail(email);
        if (user.isEmpty()) {
            return;
        }
        UserId userId = user.get().id();
        Instant now = clock.instant();
        long outstanding = tokens.findByUser(userId).stream()
                .filter(token -> !token.isExpiredAt(now))
                .count();
        if (outstanding >= MAX_OUTSTANDING_TOKENS) {
            return;
        }
        String token = generator.newToken();
        tokens.store(new ResetToken(Sha256.hex(token), userId, now.plus(TOKEN_LIFETIME)));
        mailer.send(email, MAIL_SUBJECT, messageBody(token));
    }

    /**
     * Checks the token and, if it is valid and the password is acceptable, changes the password.
     * The token is the secret from the mail link; we look it up by its hash.
     */
    public ResetResult resetPassword(String token, String newPassword) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(newPassword, "newPassword");
        String tokenHash = Sha256.hex(token);
        Optional<ResetToken> stored = tokens.findByTokenHash(tokenHash);
        if (stored.isEmpty()) {
            return new InvalidToken();
        }
        ResetToken resetToken = stored.get();
        if (resetToken.isExpiredAt(clock.instant())) {
            tokens.delete(tokenHash);
            return new ExpiredToken();
        }
        // The token is not consumed here: a weak password is a typo, not a misuse.
        if (newPassword.codePointCount(0, newPassword.length()) < MINIMUM_PASSWORD_LENGTH) {
            return new PasswordTooWeak("at least " + MINIMUM_PASSWORD_LENGTH + " characters");
        }
        User user = users.findById(resetToken.userId()).orElseThrow(() -> new IllegalStateException(
                "reset token refers to an unknown user " + resetToken.userId().value()));
        users.save(new User(user.id(), user.email(), hasher.hash(newPassword)));
        // Single use, and a changed password makes every other outstanding link useless too.
        List<ResetToken> ownTokens = tokens.findByUser(user.id());
        for (ResetToken own : ownTokens) {
            tokens.delete(own.tokenHash());
        }
        return new PasswordChanged();
    }

    private static String messageBody(String token) {
        return "Open this link to choose a new password: " + RESET_LINK_PREFIX + token
                + "\n\nThe link expires in " + TOKEN_LIFETIME_MINUTES + " minutes.";
    }
}
