package io.github.chris013.practice.task15;

import java.util.Optional;

/** Port for loading and saving gift cards. The real implementation talks to the database. */
public interface GiftCardRepository {

    Optional<GiftCard> findByCode(GiftCardCode code);

    void save(GiftCard card);
}
