package com.fastpilot.media

class DesktopAudioPlayer : AudioPlayer {
    private var currentPlayingUrl: String? = null

    override fun play(url: String) {
        println("Simulating playing audio on desktop: $url")
        currentPlayingUrl = url
    }

    override fun pause() {
         println("Simulating pausing audio on desktop")
         currentPlayingUrl = null
    }

    override fun stop() {
         currentPlayingUrl = null
    }

    override fun getPlayingUrl(): String? {
        return currentPlayingUrl
    }
}

actual fun createAudioPlayer(): AudioPlayer = DesktopAudioPlayer()
