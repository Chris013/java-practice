package io.github.chris013.practice.task10.domain;

import java.util.Objects;

/** Given. A library member. A blocked member (for example after unpaid fees) may not borrow. */
public record Member(MemberId id, boolean blocked) {

    public Member {
        Objects.requireNonNull(id, "id");
    }
}
