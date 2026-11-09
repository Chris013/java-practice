package io.github.chris013.practice.task10.application;

import io.github.chris013.practice.task10.domain.Member;
import io.github.chris013.practice.task10.domain.MemberId;
import java.util.Optional;

/** Given. Outbound port: looks up library members. */
public interface MemberDirectory {

    Optional<Member> findById(MemberId memberId);
}
