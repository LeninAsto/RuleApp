package com.leninasto.ruleapp.macrobenchmark

import androidx.benchmark.macro.*
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryBenchmark {
    @get:Rule val benchmark = MacrobenchmarkRule()
    @Test fun galleryScroll() = benchmark.measureRepeated(
        packageName = "com.leninasto.ruleapp.benchmark",
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(), iterations = 5,
        setupBlock = {
            pressHome()
            // Scroll is the measured operation. Wait on actual UI readiness in setup;
            // old gfxinfo launch markers can be absent on Android 16 emulators.
            device.executeShellCommand("am start -W -n com.leninasto.ruleapp.benchmark/com.leninasto.ruleapp.MainActivity")
            val continueButton = By.text(java.util.regex.Pattern.compile("Continuar a Daparushi|Continue to Daparushi"))
            if (device.wait(Until.hasObject(continueButton), 3_000)) {
                device.findObject(continueButton).click()
            }
            check(device.wait(Until.hasObject(By.desc(java.util.regex.Pattern.compile("Guardar en favoritos|Save to favorites"))), 10_000)) { "Gallery not loaded" }
        }
    ) {
        val width = device.displayWidth
        val height = device.displayHeight
        repeat(8) { device.swipe(width / 2, height * 3 / 4, width / 2, height / 3, 20); device.waitForIdle() }
    }
}
