package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class ProviderEventCodeTest {
    @Test public void mapsOnlyKnownProviderPackagesToSafeCodes() {
        assertEquals("WEWARD", ProviderEventCode.code("com.weward"));
        assertEquals("MACADAM", ProviderEventCode.code("com.macadamapp"));
        assertEquals("WINWALK", ProviderEventCode.code("com.winwalk.android"));
        assertEquals("SWEATCOIN", ProviderEventCode.code("in.sweatco.app"));
    }

    @Test public void doesNotLogOtherPackagesOrPotentiallySensitiveText() {
        assertEquals("", ProviderEventCode.code(null));
        assertEquals("", ProviderEventCode.code("com.bank.app"));
        assertEquals("", ProviderEventCode.code("com.weward / password"));
    }
}
