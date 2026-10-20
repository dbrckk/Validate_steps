package fr.validatesteps;

import java.text.Normalizer;
import java.util.Locale;

/** Canonical, size-capped handling of untrusted provider UI and notification labels. */
public final class StepLabelNormalizer {
    public static final int MAX_CHARS = 100;

    private StepLabelNormalizer() {}

    public static String normalize(String value) {
        if (value == null || value.length() > MAX_CHARS) return "";
        String plain = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return plain.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static boolean reasonableLength(CharSequence label) {
        return label != null && label.length() <= MAX_CHARS;
    }
}
