package io.github.chris013.practice.task22;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A fake token store that keeps tokens in a map keyed by hash, like the real table would. */
final class InMemoryResetTokens implements ResetTokenStore {

    private final Map<String, ResetToken> byHash = new LinkedHashMap<>();

    @Override
    public void store(ResetToken token) {
        byHash.put(token.tokenHash(), token);
    }

    @Override
    public Optional<ResetToken> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(byHash.get(tokenHash));
    }

    @Override
    public void delete(String tokenHash) {
        byHash.remove(tokenHash);
    }

    @Override
    public List<ResetToken> findByUser(UserId userId) {
        return byHash.values().stream().filter(token -> token.userId().equals(userId)).toList();
    }

    int count() {
        return byHash.size();
    }
}
