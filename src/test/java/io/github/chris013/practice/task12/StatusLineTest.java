package io.github.chris013.practice.task12;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StatusLineTest {

    private static final DocumentId ID = new DocumentId("POL-042");
    private static final UserId ANNA = new UserId("anna");
    private static final UserId BEN = new UserId("ben");
    private static final UserId CARA = new UserId("cara");
    private static final Content PUBLIC_POLICY =
            new Content("Travel policy", "Book economy class.", Classification.PUBLIC);
    private static final Content CONFIDENTIAL_POLICY =
            new Content("Severance terms", "For HR only.", Classification.CONFIDENTIAL);
    private static final Instant SUBMITTED = Instant.parse("2026-03-02T09:00:00Z");
    private static final Instant REVIEWED = Instant.parse("2026-03-03T10:00:00Z");

    static Stream<Arguments> one_line_per_state() {
        Draft draft = Draft.create(ID, ANNA, PUBLIC_POLICY);
        InReview emptyReview = draft.submit(SUBMITTED);
        InReview confidentialWithOneApproval = (InReview) draft.edit(CONFIDENTIAL_POLICY)
                .submit(SUBMITTED)
                .approve(BEN, REVIEWED);
        Approved approved = (Approved) emptyReview.approve(BEN, REVIEWED);
        Rejected rejected = emptyReview.reject(BEN, "Wrong year", REVIEWED);
        Published published = approved.publish(Instant.parse("2026-03-04T23:30:00Z"));
        return Stream.of(
                arguments(draft, "Draft: Travel policy"),
                arguments(emptyReview, "In review: Travel policy (0 of 1 approvals)"),
                arguments(confidentialWithOneApproval, "In review: Severance terms (1 of 2 approvals)"),
                arguments(approved, "Approved: Travel policy by ben"),
                arguments(rejected, "Rejected: Travel policy by ben: Wrong year"),
                arguments(published, "Published: Travel policy on 2026-03-04"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource
    void one_line_per_state(Document document, String expected) {
        assertThat(StatusLine.of(document)).isEqualTo(expected);
    }

    @Nested
    class Details {

        @Test
        void approvers_are_listed_in_alphabetical_order_whatever_the_insertion_order() {
            Approved approved = new Approved(ID, ANNA, CONFIDENTIAL_POLICY, Set.of(CARA, BEN), REVIEWED);

            assertThat(StatusLine.of(approved)).isEqualTo("Approved: Severance terms by ben, cara");
        }

        @Test
        void the_publication_date_is_the_utc_date_not_the_local_one() {
            // 23:30 UTC is already the next day in Berlin, but the line must show the UTC date.
            Published published = new Published(ID, ANNA, PUBLIC_POLICY, Set.of(BEN),
                    Instant.parse("2026-03-04T23:30:00Z"));

            assertThat(StatusLine.of(published)).isEqualTo("Published: Travel policy on 2026-03-04");
        }

        @Test
        void a_review_counts_the_approvals_it_has_against_the_classification() {
            InReview review = new InReview(ID, ANNA, CONFIDENTIAL_POLICY, SUBMITTED, Set.of(BEN));

            assertThat(StatusLine.of(review)).isEqualTo("In review: Severance terms (1 of 2 approvals)");
        }
    }
}
