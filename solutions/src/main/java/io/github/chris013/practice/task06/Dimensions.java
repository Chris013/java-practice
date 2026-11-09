package io.github.chris013.practice.task06;

/** Outer dimensions of a parcel in centimetres. */
public record Dimensions(int lengthCm, int widthCm, int heightCm) {

    /** Carriers bill bulky parcels as if 5000 cm³ weighed 1 kg, i.e. 5 cm³ per gram. */
    private static final int CUBIC_CM_PER_VOLUMETRIC_GRAM = 5;

    public Dimensions {
        if (lengthCm < 1 || widthCm < 1 || heightCm < 1) {
            throw new IllegalArgumentException(
                    "Dimensions must be positive, got %d x %d x %d cm".formatted(lengthCm, widthCm, heightCm));
        }
    }

    int volumetricWeightGrams() {
        return Math.ceilDiv(lengthCm * widthCm * heightCm, CUBIC_CM_PER_VOLUMETRIC_GRAM);
    }
}
