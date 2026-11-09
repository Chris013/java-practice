package io.github.chris013.practice.task22;

import java.util.Optional;

/** Where user accounts live. Our own port: the service depends on this, not on a database driver. */
public interface UserRepository {

    Optional<User> findByEmail(EmailAddress email);

    Optional<User> findById(UserId id);

    /** Inserts the user, or replaces the stored version with the same id. */
    void save(User user);
}
