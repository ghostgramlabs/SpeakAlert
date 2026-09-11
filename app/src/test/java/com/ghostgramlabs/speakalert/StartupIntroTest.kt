package com.ghostgramlabs.speakalert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartupIntroTest {
    @Test fun `first home launch explains the app instead of showing release notes`() {
        assertEquals(StartupIntro.QUICK_START, startupIntroFor(null, "2.0.34", true))
    }
    @Test fun `upgrading an existing installation shows release notes`() {
        assertEquals(StartupIntro.RELEASE_NOTES, startupIntroFor("2.0.33", "2.0.34", true))
    }
    @Test fun `completed or dismissed intro does not repeat on the same version`() {
        assertNull(startupIntroFor("2.0.34", "2.0.34", true))
    }
    @Test fun `notification and widget launches do not interrupt the requested task`() {
        assertNull(startupIntroFor(null, "2.0.34", false))
        assertNull(startupIntroFor("2.0.33", "2.0.34", false))
    }
    @Test fun `empty legacy version marker is treated as first launch`() {
        assertEquals(StartupIntro.QUICK_START, startupIntroFor("", "2.0.34", true))
    }
}
