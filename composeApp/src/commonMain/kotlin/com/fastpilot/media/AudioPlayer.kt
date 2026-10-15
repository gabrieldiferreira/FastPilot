package com.fastpilot.media

interface AudioPlayer {
    fun play(url: String)
    fun pause()
    fun stop()
    fun getPlayingUrl(): String?

    /**
     * Called on the main thread when a recording ends or cannot be played (no network,
     * unreachable or invalid file), so the screen does not keep showing it as playing.
     */
    var onEvent: ((AudioEvent) -> Unit)?
}

sealed interface AudioEvent {
    val url: String

    data class Finished(override val url: String) : AudioEvent
    data class Failed(override val url: String) : AudioEvent
}

expect fun createAudioPlayer(): AudioPlayer
