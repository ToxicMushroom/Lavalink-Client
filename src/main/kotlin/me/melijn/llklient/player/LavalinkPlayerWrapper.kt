package me.melijn.llklient.player

import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer
import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import me.melijn.llklient.io.filters.Filters
import me.melijn.llklient.player.event.IPlayerEventListener

class LavaplayerPlayerWrapper(private val player: AudioPlayer) : IPlayer {

    private val eqFac = EqualizerFactory()
    val bands = FloatArray(Equalizer.BAND_COUNT)

    override val playingTrack: AudioTrack
        get() = player.playingTrack
    override val filters: Filters by lazy { Filters(this, this::onCommit) }

    override suspend fun playTrack(track: AudioTrack) {
        player.playTrack(track)
    }

    override suspend fun stopTrack() {
        player.stopTrack()
    }

    override var paused: Boolean
        get() = player.isPaused
        set(b) {
            player.isPaused = b
        }

    override suspend fun setPaused(value: Boolean) {
        this.paused = value
    }

    override val trackPosition: Long
        get() {
            checkNotNull(player.playingTrack) { "Not playing anything" }
            return player.playingTrack.position
        }

    override suspend fun seekTo(position: Long) {
        checkNotNull(player.playingTrack) { "Not playing anything" }
        player.playingTrack.position = position
    }

    override fun addListener(listener: IPlayerEventListener) {
        player.addListener(listener as AudioEventListener)
    }

    override fun removeListener(listener: IPlayerEventListener) {
        player.removeListener(listener as AudioEventListener)
    }

    fun provide(): AudioFrame {
        return player.provide()
    }

    fun provide(targetFrame: MutableAudioFrame): Boolean {
        return player.provide(targetFrame)
    }

    private suspend fun onCommit() {
        player.volume = filters.volume
    }
}
