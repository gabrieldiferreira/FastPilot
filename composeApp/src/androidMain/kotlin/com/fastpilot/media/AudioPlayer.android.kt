package com.fastpilot.media

import android.media.AudioAttributes
import android.media.MediaPlayer

class AndroidAudioPlayer : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingUrl: String? = null

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
                }
                setOnErrorListener { _, _, _ ->
                    currentPlayingUrl = null
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            currentPlayingUrl = null
        }
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
