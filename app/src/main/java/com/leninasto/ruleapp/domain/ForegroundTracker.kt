package com.leninasto.ruleapp.domain

/** Internal activity transitions and rotation are not exits from the app. */
class ForegroundTracker(private val enter: () -> Unit, private val leave: () -> Unit) {
    private var started = 0
    fun started() { if (started++ == 0) enter() }
    fun stopped(changingConfiguration: Boolean) {
        started = (started - 1).coerceAtLeast(0)
        if (started == 0 && !changingConfiguration) leave()
    }
}
