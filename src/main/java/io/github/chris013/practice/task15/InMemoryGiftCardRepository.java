package io.github.chris013.practice.task15;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Keeps cards in a map. Good enough for tests; the production repository is a database table. */
public final class InMemoryGiftCardRepository implements GiftCardRepository {

    private final Map<GiftCardCode, GiftCard> cards = new HashMap<>();

    @Override
    public Optional<GiftCard> findByCode(GiftCardCode code) {
        Objects.requireNonNull(code, "code");
        return Optional.ofNullable(cards.get(code));
    }

    @Override
    public void save(GiftCard card) {
        Objects.requireNonNull(card, "card");
        cards.put(card.code(), card);
    }
}
