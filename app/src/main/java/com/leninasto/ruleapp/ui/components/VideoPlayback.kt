package com.leninasto.ruleapp.ui.components

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/** Owned by the active viewer page only, with no credential-bearing HTTP client. */
class VideoPlayback(context: Context, private val owner: LifecycleOwner, url: String) : AutoCloseable {
    val player = ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(url)); playWhenReady = false; prepare()
    }
    var released = false; private set
    private val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) player.pause() }
    init { owner.lifecycle.addObserver(observer) }
    override fun close() {
        if (released) return
        owner.lifecycle.removeObserver(observer); player.release(); released = true
    }
}
