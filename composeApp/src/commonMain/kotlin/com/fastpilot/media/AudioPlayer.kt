package com.fastpilot.media

interface AudioPlayer {
    fun play(url: String)
    fun pause()
    fun stop()
    fun getPlayingUrl(): String?
}

expect fun createAudioPlayer(): AudioPlayer
