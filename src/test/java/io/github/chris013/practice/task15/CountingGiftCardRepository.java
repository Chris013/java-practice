package io.github.chris013.practice.task15;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Wraps the in-memory repository and counts the saves, so tests can check when the service persists a card.
 * It also remembers the balance at the last save, so a test can check that the debit was saved.
 */
final class CountingGiftCardRepository implements GiftCardRepository {

    private final InMemoryGiftCardRepository delegate = new InMemoryGiftCardRepository();
    private int saves;
    private BigDecimal lastSavedBalance;

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
        lastSavedBalance = card.balance();
        delegate.save(card);
    }

    int saveCount() {
        return saves;
    }

    /** The balance that was written by the most recent save, or {@code null} if nothing was saved yet. */
    BigDecimal lastSavedBalance() {
        return lastSavedBalance;
    }
}
