package com.fastpilot.media

import platform.AVFoundation.*
import platform.Foundation.NSURL

class IosAudioPlayer : AudioPlayer {
    private var avPlayer: AVPlayer? = null
    private var currentPlayingUrl: String? = null

    override fun play(url: String) {
        val nsUrl = NSURL(string = url)

        // Toggle pause/play
        if (currentPlayingUrl == url && avPlayer?.timeControlStatus == AVPlayerTimeControlStatusPlaying) {
            pause()
            return
        }
        if (currentPlayingUrl == url && avPlayer?.timeControlStatus == AVPlayerTimeControlStatusPaused) {
            avPlayer?.play()
            return
        }

        stop()
        currentPlayingUrl = url
        avPlayer = AVPlayer(uRL = nsUrl)
        avPlayer?.play()
    }

    override fun pause() {
        avPlayer?.pause()
    }

    override fun stop() {
        avPlayer?.pause()
        avPlayer = null
        currentPlayingUrl = null
    }

    override fun getPlayingUrl(): String? {
        return if (avPlayer?.timeControlStatus == AVPlayerTimeControlStatusPlaying) currentPlayingUrl else null
    }
}

actual fun createAudioPlayer(): AudioPlayer = IosAudioPlayer()
