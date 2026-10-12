package com.leninasto.ruleapp

import com.leninasto.ruleapp.domain.AppVersion
import org.junit.Assert.*
import org.junit.Test

class AppVersionTest {
    @Test fun comparisonIsNumericAndStableFollowsBeta() {
        assertTrue(AppVersion.parse("0.10.0_beta")!! > AppVersion.parse("0.9.1_beta")!!)
        assertTrue(AppVersion.parse("0.9.1")!! > AppVersion.parse("0.9.1_beta")!!)
        assertEquals(AppVersion.parse("0.9.0_beta"), AppVersion.parse("0.9_beta"))
    }
    @Test fun rejectsUntrustedPayloadInsteadOfCallingItAnUpdate() {
        listOf("", "<html>403</html>", "0.9.1_beta\n1.2.0", "../../keystore").forEach { assertNull(AppVersion.parse(it)) }
    }
}
