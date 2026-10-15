package com.fastpilot.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RadioPlaybackTest {

    private class FakePlayer(private val offline: Boolean = false) : AudioPlayer {
        val calls = mutableListOf<String>()
        override var onEvent: ((AudioEvent) -> Unit)? = null
        override fun play(url: String) {
            calls += "play $url"
            // Android without network reports the failure inside play()
            if (offline) onEvent?.invoke(AudioEvent.Failed(url))
        }
        override fun pause() { calls += "pause" }
        override fun stop() { calls += "stop" }
        override fun getPlayingUrl(): String? = null
    }

    @Test
    fun aRecordingThatCannotPlayStopsShowingAsPlayingAndIsFlagged() {
        val player = FakePlayer()
        val playback = RadioPlayback(player)

        playback.toggle("a.mp3")
        assertEquals("a.mp3", playback.playingUrl)

        // Offline: the platform player reports the failure
        player.onEvent?.invoke(AudioEvent.Failed("a.mp3"))
        assertNull(playback.playingUrl)
        assertEquals("a.mp3", playback.failedUrl)

        // Trying again clears the warning
        playback.toggle("a.mp3")
        assertNull(playback.failedUrl)
        assertEquals("a.mp3", playback.playingUrl)
    }

    @Test
    fun aRecordingThatEndsGoesBackToListen() {
        val player = FakePlayer()
        val playback = RadioPlayback(player)
        playback.toggle("a.mp3")

        player.onEvent?.invoke(AudioEvent.Finished("a.mp3"))

        assertNull(playback.playingUrl)
        assertNull(playback.failedUrl)
    }

    @Test
    fun anEventOfAnotherRecordingDoesNotStopTheCurrentOne() {
        val player = FakePlayer()
        val playback = RadioPlayback(player)
        playback.toggle("a.mp3")
        playback.toggle("b.mp3")

        player.onEvent?.invoke(AudioEvent.Failed("a.mp3"))

        assertEquals("b.mp3", playback.playingUrl)
        assertEquals("a.mp3", playback.failedUrl)
        playback.toggle("b.mp3")
        assertEquals(listOf("play a.mp3", "play b.mp3", "pause"), player.calls)
    }

    @Test
    fun aFailureReportedInsidePlayIsNotOverwritten() {
        val playback = RadioPlayback(FakePlayer(offline = true))

        playback.toggle("a.mp3")

        assertNull(playback.playingUrl)
        assertEquals("a.mp3", playback.failedUrl)
    }
}
