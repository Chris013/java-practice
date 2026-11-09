package io.github.chris013.practice.task12;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentWorkflowTest {

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
    private static final Instant PUBLISHED = Instant.parse("2026-03-04T08:00:00Z");

    private static Draft draft(Content content) {
        return Draft.create(ID, ANNA, content);
    }

    private static InReview submitted(Content content) {
        return draft(content).submit(SUBMITTED);
    }

    private static Approved approvedPublic() {
        return (Approved) submitted(PUBLIC_POLICY).approve(BEN, REVIEWED);
    }

    private static InReview confidentialWithBensApproval() {
        return (InReview) submitted(CONFIDENTIAL_POLICY).approve(BEN, REVIEWED);
    }

    @Nested
    class Drafting {

        @Test
        void create_keeps_the_id_author_and_content() {
            Draft draft = draft(PUBLIC_POLICY);

            assertThat(draft.id()).isEqualTo(ID);
            assertThat(draft.author()).isEqualTo(ANNA);
            assertThat(draft.content()).isEqualTo(PUBLIC_POLICY);
        }

        @Test
        void edit_replaces_the_content_and_keeps_the_id_and_author() {
            Draft edited = draft(PUBLIC_POLICY).edit(CONFIDENTIAL_POLICY);

            assertThat(edited.content()).isEqualTo(CONFIDENTIAL_POLICY);
            assertThat(edited.id()).isEqualTo(ID);
            assertThat(edited.author()).isEqualTo(ANNA);
        }

        @Test
        void rejects_a_missing_author() {
            assertThatThrownBy(() -> Draft.create(ID, null, PUBLIC_POLICY))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("author");
        }
    }

    @Nested
    class Content_rules {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void reject_a_blank_title(String title) {
            assertThatThrownBy(() -> new Content(title, "Some body", Classification.PUBLIC))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void reject_a_blank_body(String body) {
            assertThatThrownBy(() -> new Content("Title", body, Classification.PUBLIC))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Submitting {

        @Test
        void moves_the_draft_to_review_with_no_approvals() {
            InReview review = draft(PUBLIC_POLICY).submit(SUBMITTED);

            assertThat(review.submittedAt()).isEqualTo(SUBMITTED);
            assertThat(review.approvals()).isEmpty();
            assertThat(review.content()).isEqualTo(PUBLIC_POLICY);
        }
    }

    @Nested
    class Approving_a_public_document {

        @Test
        void one_approval_is_enough_and_the_document_becomes_approved() {
            ReviewOutcome outcome = submitted(PUBLIC_POLICY).approve(BEN, REVIEWED);

            assertThat(outcome).isInstanceOf(Approved.class);
            Approved approved = (Approved) outcome;
            assertThat(approved.approvers()).containsExactly(BEN);
            assertThat(approved.approvedAt()).isEqualTo(REVIEWED);
        }
    }

    @Nested
    class Approving_a_confidential_document {

        @Test
        void one_approval_keeps_it_in_review() {
            ReviewOutcome outcome = submitted(CONFIDENTIAL_POLICY).approve(BEN, REVIEWED);

            assertThat(outcome).isInstanceOf(InReview.class);
            assertThat(((InReview) outcome).approvals()).containsExactly(BEN);
        }

        @Test
        void a_second_approval_from_another_reviewer_approves_it() {
            ReviewOutcome outcome = confidentialWithBensApproval().approve(CARA, REVIEWED);

            assertThat(outcome).isInstanceOf(Approved.class);
            assertThat(((Approved) outcome).approvers()).containsExactlyInAnyOrder(BEN, CARA);
        }
    }

    @Nested
    class Four_eyes_principle {

        @Test
        void the_author_cannot_approve_their_own_document() {
            assertThatThrownBy(() -> submitted(PUBLIC_POLICY).approve(ANNA, REVIEWED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("four-eyes");
        }

        @Test
        void the_author_cannot_approve_a_confidential_document_after_another_approval() {
            assertThatThrownBy(() -> confidentialWithBensApproval().approve(ANNA, REVIEWED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("four-eyes");
        }

        @Test
        void the_author_cannot_reject_their_own_document() {
            assertThatThrownBy(() -> submitted(PUBLIC_POLICY).reject(ANNA, "Wrong year", REVIEWED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("four-eyes");
        }
    }

    @Nested
    class Duplicate_approvals {

        @Test
        void the_same_reviewer_cannot_approve_a_confidential_document_twice() {
            assertThatThrownBy(() -> confidentialWithBensApproval().approve(BEN, REVIEWED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ben");
        }
    }

    @Nested
    class Rejecting {

        @Test
        void records_the_reviewer_the_reason_and_the_time() {
            Rejected rejected = submitted(PUBLIC_POLICY).reject(BEN, "Missing the effective date", REVIEWED);

            assertThat(rejected.reviewer()).isEqualTo(BEN);
            assertThat(rejected.reason()).isEqualTo("Missing the effective date");
            assertThat(rejected.rejectedAt()).isEqualTo(REVIEWED);
            assertThat(rejected.content()).isEqualTo(PUBLIC_POLICY);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void a_blank_reason_is_rejected(String reason) {
            assertThatThrownBy(() -> submitted(PUBLIC_POLICY).reject(BEN, reason, REVIEWED))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Revising {

        @Test
        void returns_a_draft_with_the_new_content_and_the_same_id_and_author() {
            Rejected rejected = submitted(CONFIDENTIAL_POLICY).reject(BEN, "Too vague", REVIEWED);

            Draft revised = rejected.revise(PUBLIC_POLICY);

            assertThat(revised.id()).isEqualTo(ID);
            assertThat(revised.author()).isEqualTo(ANNA);
            assertThat(revised.content()).isEqualTo(PUBLIC_POLICY);
        }

        @Test
        void a_revised_document_starts_its_next_review_with_no_old_approvals() {
            Draft revised = submitted(PUBLIC_POLICY).reject(BEN, "Too vague", REVIEWED).revise(PUBLIC_POLICY);

            assertThat(revised.submit(SUBMITTED).approvals()).isEmpty();
        }
    }

    @Nested
    class Publishing {

        @Test
        void an_approved_document_can_be_published_with_its_approvers() {
            Published published = approvedPublic().publish(PUBLISHED);

            assertThat(published.approvers()).containsExactly(BEN);
            assertThat(published.publishedAt()).isEqualTo(PUBLISHED);
            assertThat(published.content()).isEqualTo(PUBLIC_POLICY);
        }
    }

    @Nested
    class Invariants {

        @Test
        void an_approved_document_needs_at_least_one_approver() {
            assertThatThrownBy(() -> new Approved(ID, ANNA, PUBLIC_POLICY, Set.of(), REVIEWED))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void a_published_document_needs_at_least_one_approver() {
            assertThatThrownBy(() -> new Published(ID, ANNA, PUBLIC_POLICY, Set.of(), PUBLISHED))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void a_review_needs_its_approvals_set() {
            assertThatThrownBy(() -> new InReview(ID, ANNA, PUBLIC_POLICY, SUBMITTED, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("approvals");
        }

        @Test
        void an_approved_document_cannot_be_approved_by_its_author() {
            assertThatThrownBy(() -> new Approved(ID, ANNA, PUBLIC_POLICY, Set.of(ANNA), REVIEWED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("four-eyes");
        }

        @Test
        void a_published_document_cannot_be_approved_by_its_author() {
            assertThatThrownBy(() -> new Published(ID, ANNA, PUBLIC_POLICY, Set.of(ANNA), PUBLISHED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("four-eyes");
        }

        @Test
        void a_review_cannot_list_its_author_as_an_approver() {
            assertThatThrownBy(() -> new InReview(ID, ANNA, CONFIDENTIAL_POLICY, SUBMITTED, Set.of(ANNA)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("four-eyes");
        }

        @Test
        void a_confidential_approved_document_needs_two_approvers() {
            assertThatThrownBy(() -> new Approved(ID, ANNA, CONFIDENTIAL_POLICY, Set.of(BEN), REVIEWED))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void a_confidential_published_document_needs_two_approvers() {
            assertThatThrownBy(() -> new Published(ID, ANNA, CONFIDENTIAL_POLICY, Set.of(BEN), PUBLISHED))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void a_review_that_already_has_enough_approvals_cannot_be_built() {
            assertThatThrownBy(() -> new InReview(ID, ANNA, PUBLIC_POLICY, SUBMITTED, Set.of(BEN)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void a_rejection_by_the_author_cannot_be_built_directly() {
            assertThatThrownBy(() -> new Rejected(ID, ANNA, PUBLIC_POLICY, ANNA, "Wrong year", REVIEWED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("four-eyes");
        }

        @Test
        void a_rejection_without_a_reason_cannot_be_built_directly() {
            assertThatThrownBy(() -> new Rejected(ID, ANNA, PUBLIC_POLICY, BEN, " ", REVIEWED))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Immutability {

        @Test
        void approving_does_not_change_the_review_it_started_from() {
            InReview review = submitted(CONFIDENTIAL_POLICY);

            review.approve(BEN, REVIEWED);

            assertThat(review.approvals()).isEmpty();
        }

        @Test
        void editing_does_not_change_the_original_draft() {
            Draft draft = draft(PUBLIC_POLICY);

            draft.edit(CONFIDENTIAL_POLICY);

            assertThat(draft.content()).isEqualTo(PUBLIC_POLICY);
        }

        @Test
        void publishing_does_not_change_the_approved_document() {
            Approved approved = approvedPublic();

            approved.publish(PUBLISHED);

            assertThat(approved.approvers()).containsExactly(BEN);
        }

        @Test
        void the_approvers_of_a_review_cannot_be_changed_from_outside() {
            Set<UserId> approvers = new HashSet<>(Set.of(BEN));
            InReview review = new InReview(ID, ANNA, CONFIDENTIAL_POLICY, SUBMITTED, approvers);

            approvers.add(CARA);

            assertThat(review.approvals()).containsExactly(BEN);
        }

        @Test
        void the_approvers_of_an_approved_document_are_unmodifiable() {
            Set<UserId> approvers = approvedPublic().approvers();

            assertThatThrownBy(() -> approvers.add(CARA)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void the_approvers_of_a_published_document_are_unmodifiable() {
            Set<UserId> approvers = approvedPublic().publish(PUBLISHED).approvers();

            assertThatThrownBy(() -> approvers.remove(BEN)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void the_approvals_of_a_review_are_unmodifiable() {
            Set<UserId> approvals = confidentialWithBensApproval().approvals();

            assertThatThrownBy(() -> approvals.add(CARA)).isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
