package io.github.chris013.practice.task35;

import java.io.Serial;
import java.util.Collection;
import java.util.HashSet;

/**
 * <b>Broken. Kept for comparison only, do not use.</b>
 *
 * <p>This class counts scans by overriding {@code add} and {@code addAll} of {@link HashSet}. It
 * double-counts every batch upload. {@link HashSet} does not override {@code addAll}; it inherits
 * {@link java.util.AbstractCollection#addAll}, which calls {@code add} once per element. So
 * {@code addAll(of 3 barcodes)} runs our {@code addAll}, which adds 3, and then 3 more calls to our
 * {@code add}. The result is 6 attempts for 3 scans. The superclass's internal calls are an
 * implementation detail, and we depend on them without knowing it.
 */
public class InheritedScanCounter extends HashSet<String> {

    @Serial
    private static final long serialVersionUID = 1L;

    private int attempts;

    @Override
    public boolean add(String barcode) {
        attempts++;
        return super.add(barcode);
    }

    @Override
    public boolean addAll(Collection<? extends String> barcodes) {
        attempts += barcodes.size();
        return super.addAll(barcodes);
    }

    public int addAttempts() {
        return attempts;
    }
}
