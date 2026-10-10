package com.leninasto.ruleapp

import android.content.Context
import android.graphics.drawable.Animatable
import androidx.lifecycle.*
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import coil3.DrawableImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.leninasto.ruleapp.ui.components.VideoPlayback
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MediaTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun fixture(name: String) = context.cacheDir.resolve(name).apply {
        instrumentation.context.assets.open(name).use { input -> outputStream().use { input.copyTo(it) } }
    }
    @Test fun gifUsesAnimatedDecoder() = runBlocking {
        val result = SingletonImageLoader.get(context).execute(ImageRequest.Builder(context).data(fixture("colors.gif")).build())
        assertTrue(result is SuccessResult)
        assertTrue((result as SuccessResult).image is DrawableImage)
        assertTrue((result.image as DrawableImage).drawable is Animatable)
    }
    @Test fun mp4AndWebmStartPausedPauseInBackgroundAndRelease() {
        listOf("colors.mp4", "colors.webm").forEach { name ->
            val file = fixture(name)
            lateinit var playback: VideoPlayback
            val owner = object : LifecycleOwner {
                val registry = LifecycleRegistry(this)
                override val lifecycle: Lifecycle get() = registry
            }
            instrumentation.runOnMainSync {
                owner.registry.currentState = Lifecycle.State.RESUMED
                playback = VideoPlayback(context, owner, android.net.Uri.fromFile(file).toString())
                assertFalse(playback.player.playWhenReady)
            }
            val deadline = System.currentTimeMillis() + 15_000
            var ready = false
            while (!ready && System.currentTimeMillis() < deadline) {
                instrumentation.runOnMainSync { ready = playback.player.playbackState == Player.STATE_READY }
                Thread.sleep(50)
            }
            try {
                assertTrue("Decoder did not become ready: $name", ready)
                instrumentation.runOnMainSync {
                    playback.player.play(); owner.registry.currentState = Lifecycle.State.CREATED
                    assertFalse(playback.player.playWhenReady)
                }
            } finally { instrumentation.runOnMainSync { playback.close(); assertTrue(playback.released) } }
        }
    }
}
