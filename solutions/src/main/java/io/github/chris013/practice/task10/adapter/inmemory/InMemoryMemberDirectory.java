package io.github.chris013.practice.task10.adapter.inmemory;

import io.github.chris013.practice.task10.application.MemberDirectory;
import io.github.chris013.practice.task10.domain.Member;
import io.github.chris013.practice.task10.domain.MemberId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps members in memory. Demo adapter and test double. */
public final class InMemoryMemberDirectory implements MemberDirectory {

    private final Map<MemberId, Member> members = new ConcurrentHashMap<>();

    /** Adds a member, or replaces the one with the same id (useful to block someone in a test). */
    public void add(Member member) {
        Objects.requireNonNull(member, "member");
        members.put(member.id(), member);
    }

    @Override
    public Optional<Member> findById(MemberId memberId) {
        Objects.requireNonNull(memberId, "memberId");
        return Optional.ofNullable(members.get(memberId));
    }
}
