package com.fastpilot.media

import kotlinx.browser.document
import org.w3c.dom.HTMLAudioElement

class WebAudioPlayer : AudioPlayer {
    private var audio: HTMLAudioElement? = null
    private var currentPlayingUrl: String? = null
    override var onEvent: ((AudioEvent) -> Unit)? = null

    override fun play(url: String) {
        // Toggle pause/play
        if (currentPlayingUrl == url && audio != null) {
            if (audio!!.paused) audio!!.play() else pause()
            return
        }

        stop()
        currentPlayingUrl = url
        audio = (document.createElement("audio") as HTMLAudioElement).apply {
            // Without network (or a file the browser cannot load) the element fires "error"
            addEventListener("error") {
                if (currentPlayingUrl == url) currentPlayingUrl = null
                onEvent?.invoke(AudioEvent.Failed(url))
            }
            addEventListener("ended") {
                if (currentPlayingUrl == url) currentPlayingUrl = null
                onEvent?.invoke(AudioEvent.Finished(url))
            }
            src = url
            play()
        }
    }

    override fun pause() {
        audio?.pause()
    }

    override fun stop() {
        audio?.pause()
        audio = null
        currentPlayingUrl = null
    }

    override fun getPlayingUrl(): String? {
        return if (audio?.paused == false) currentPlayingUrl else null
    }
}

actual fun createAudioPlayer(): AudioPlayer = WebAudioPlayer()
