package com.leninasto.ruleapp

import com.leninasto.ruleapp.domain.ForegroundTracker
import org.junit.Assert.*
import org.junit.Test

class ForegroundTrackerTest {
    @Test fun movingFromMainToSettingsDoesNotLockTheVisibleApp() {
        var leaves = 0
        val tracker = ForegroundTracker({}, { leaves++ })
        tracker.started(); tracker.started(); tracker.stopped(false)
        assertEquals(0, leaves)
        tracker.stopped(false); assertEquals(1, leaves)
    }
    @Test fun languageRecreationAndRotationKeepAccess() {
        var leaves = 0
        val tracker = ForegroundTracker({}, { leaves++ })
        tracker.started(); tracker.stopped(true); tracker.started()
        assertEquals(0, leaves)
        tracker.stopped(false); assertEquals(1, leaves)
    }
}
