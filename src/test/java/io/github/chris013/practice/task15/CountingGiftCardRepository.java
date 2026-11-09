package io.github.chris013.practice.task15;

import java.util.Optional;

/** Wraps the in-memory repository and counts the saves, so tests can check when the service persists a card. */
final class CountingGiftCardRepository implements GiftCardRepository {

    private final InMemoryGiftCardRepository delegate = new InMemoryGiftCardRepository();
    private int saves;

    /** Puts a card in place without counting it, as if it had been stored before the test started. */
    void seed(GiftCard card) {
        delegate.save(card);
    }

    @Override
    public Optional<GiftCard> findByCode(GiftCardCode code) {
        return delegate.findByCode(code);
    }

    @Override
    public void save(GiftCard card) {
        saves++;
        delegate.save(card);
    }

    int saveCount() {
        return saves;
    }
}
