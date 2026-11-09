package io.github.chris013.practice.task22;

import java.util.List;
import java.util.Optional;

/** Stores reset tokens by their hash. Our own port, like {@link UserRepository}. */
public interface ResetTokenStore {

    void store(ResetToken token);

    Optional<ResetToken> findByTokenHash(String tokenHash);

    /** Removes the token with this hash. Removing a missing token does nothing. */
    void delete(String tokenHash);

    /** All tokens of the user, expired ones included. */
    List<ResetToken> findByUser(UserId userId);
}
