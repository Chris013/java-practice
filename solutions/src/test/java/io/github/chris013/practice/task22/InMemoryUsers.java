package io.github.chris013.practice.task22;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** A fake with the real semantics of the port: save replaces, lookups return what was saved. */
final class InMemoryUsers implements UserRepository {

    private final Map<UserId, User> byId = new HashMap<>();

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return byId.values().stream().filter(user -> user.email().equals(email)).findFirst();
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public void save(User user) {
        byId.put(user.id(), user);
    }
}
