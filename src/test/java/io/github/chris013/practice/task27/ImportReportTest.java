package io.github.chris013.practice.task27;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImportReportTest {

    @Test
    void is_clean_only_without_errors_and_without_an_abort() {
        RowError error = new RowError(4, "sku", "must be 3 to 20 characters A-Z, 0-9 or -");

        assertThat(new ImportReport(3, 3, List.of(), false).isClean()).isTrue();
        assertThat(new ImportReport(3, 2, List.of(error), false).isClean()).isFalse();
        assertThat(new ImportReport(3, 0, List.of(), true).isClean()).isFalse();
    }

    @Test
    void copies_the_error_list_so_the_report_cannot_change() {
        List<RowError> errors = new ArrayList<>(List.of(new RowError(2, "sku", "bad")));
        ImportReport report = new ImportReport(1, 0, errors, false);

        errors.clear();

        assertThat(report.errors()).hasSize(1);
        assertThatThrownBy(() -> report.errors().add(new RowError(3, "sku", "more")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
