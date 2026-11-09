package io.github.chris013.practice.task10.adapter.inmemory;

import io.github.chris013.practice.task10.application.MemberDirectory;
import io.github.chris013.practice.task10.domain.Member;
import io.github.chris013.practice.task10.domain.MemberId;
import java.util.Optional;

/**
 * Keeps members in memory. Demo adapter and test double.
 *
 * <p>Task 10: read {@code tasks/10-library-lending-hexagonal.md} first.
 */
public final class InMemoryMemberDirectory implements MemberDirectory {

    /** Adds a member, or replaces the one with the same id (useful to block someone in a test). */
    public void add(Member member) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }

    @Override
    public Optional<Member> findById(MemberId memberId) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }
}
