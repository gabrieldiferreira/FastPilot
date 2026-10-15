package com.fastpilot.media

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.koin.core.context.GlobalContext

class AndroidAudioPlayer : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingUrl: String? = null
    override var onEvent: ((AudioEvent) -> Unit)? = null

    override fun play(url: String) {
        // Toggle pause if clicking the same url while playing
        if (currentPlayingUrl == url && mediaPlayer?.isPlaying == true) {
            pause()
            return
        }
        // Resume if clicking the same url while paused
        if (currentPlayingUrl == url && mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
            return
        }

        stop()

        // Without network MediaPlayer retries for ~25 s before failing: say so right away
        if (!hasNetwork()) {
            onEvent?.invoke(AudioEvent.Failed(url))
            return
        }
        currentPlayingUrl = url

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener {
                    it.start()
                }
                setOnCompletionListener {
                    currentPlayingUrl = null
                    onEvent?.invoke(AudioEvent.Finished(url))
                }
                // Without network prepareAsync fails here (MEDIA_ERROR_IO)
                setOnErrorListener { _, _, _ ->
                    currentPlayingUrl = null
                    onEvent?.invoke(AudioEvent.Failed(url))
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            currentPlayingUrl = null
            onEvent?.invoke(AudioEvent.Failed(url))
        }
    }

    private fun hasNetwork(): Boolean = try {
        val connectivity = GlobalContext.get().get<Context>().getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity?.getNetworkCapabilities(connectivity.activeNetwork)
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } catch (_: Exception) {
        true // unknown: let MediaPlayer try
    }

    override fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    override fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        currentPlayingUrl = null
    }

    override fun getPlayingUrl(): String? {
        return if (mediaPlayer?.isPlaying == true) currentPlayingUrl else null
    }
}

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()
