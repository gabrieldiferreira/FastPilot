package com.fastpilot.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * What the radio list shows about playback: the message playing and the one that failed to
 * play. Follows [player]'s events, so a recording that ends or fails stops showing as playing.
 */
class RadioPlayback(private val player: AudioPlayer) {
    var playingUrl by mutableStateOf<String?>(null)
        private set
    var failedUrl by mutableStateOf<String?>(null)
        private set

    init {
        player.onEvent = { event ->
            if (playingUrl == event.url) playingUrl = null
            if (event is AudioEvent.Failed) failedUrl = event.url
        }
    }

    /** Play or pause [url]; trying again clears its failure. */
    fun toggle(url: String) {
        if (playingUrl == url) {
            player.pause()
            playingUrl = null
        } else {
            if (failedUrl == url) failedUrl = null
            // Before play(): a player may report a failure from inside it (Android without network)
            playingUrl = url
            player.play(url)
        }
    }

    fun release() {
        player.onEvent = null
        player.stop()
    }
}
