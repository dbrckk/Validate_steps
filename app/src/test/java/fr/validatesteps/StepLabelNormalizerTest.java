package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class StepLabelNormalizerTest {
    @Test public void canonicalizesAccentedFrenchAndWhitespace() {
        assertEquals("recuperer mes pas",
            StepLabelNormalizer.normalize("  RÉCUPÉRER   MES   PAS  "));
    }

    @Test public void oversizedOrNullThirdPartyTextNeverMatches() {
        assertEquals("", StepLabelNormalizer.normalize("valider mes pas".repeat(1000)));
        assertEquals("", StepLabelNormalizer.normalize(null));
        assertFalse(StepLabelNormalizer.reasonableLength("a".repeat(150)));
        assertFalse(StepLabelNormalizer.reasonableLength(null));
        assertTrue(StepLabelNormalizer.reasonableLength("Valider mes pas"));
    }
}
