package com.fastpilot.media

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFoundation.*
import platform.Foundation.NSURL

class IosAudioPlayer : AudioPlayer {
    private var avPlayer: AVPlayer? = null
    private var currentPlayingUrl: String? = null
    override var onEvent: ((AudioEvent) -> Unit)? = null

    private val scope = MainScope()
    private var watch: Job? = null

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
        val player = AVPlayer(uRL = nsUrl)
        avPlayer = player
        player.play()
        watch = scope.launch { watch(player, url) }
    }

    /**
     * AVPlayer reports a failed load (no network, bad file) only through the item status and the
     * end through its position, both observed with key-value observing; checking them twice a
     * second is simpler from Kotlin. A recording that has not started after 15 s counts as failed.
     */
    private suspend fun watch(player: AVPlayer, url: String) {
        var waited = 0
        while (avPlayer === player) {
            val item = player.currentItem
            val failed = item?.status == AVPlayerItemStatusFailed || player.status == AVPlayerStatusFailed
            val started = player.timeControlStatus == AVPlayerTimeControlStatusPlaying
            when {
                failed || (!started && waited >= 15_000 && item?.status != AVPlayerItemStatusReadyToPlay) -> {
                    stop()
                    onEvent?.invoke(AudioEvent.Failed(url))
                    return
                }
                item != null && item.status == AVPlayerItemStatusReadyToPlay && !started &&
                    player.rate == 0.0f && isAtEnd(item) -> {
                    stop()
                    onEvent?.invoke(AudioEvent.Finished(url))
                    return
                }
            }
            delay(500)
            waited += 500
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun isAtEnd(item: AVPlayerItem): Boolean {
        val duration = platform.CoreMedia.CMTimeGetSeconds(item.duration)
        val position = platform.CoreMedia.CMTimeGetSeconds(item.currentTime())
        return duration.isFinite() && duration > 0 && position >= duration - 0.25
    }

    override fun pause() {
        avPlayer?.pause()
    }

    override fun stop() {
        watch?.cancel()
        watch = null
        avPlayer?.pause()
        avPlayer = null
        currentPlayingUrl = null
    }

    override fun getPlayingUrl(): String? {
        return if (avPlayer?.timeControlStatus == AVPlayerTimeControlStatusPlaying) currentPlayingUrl else null
    }
}

actual fun createAudioPlayer(): AudioPlayer = IosAudioPlayer()
