package me.melijn.llklient.player

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.llklient.io.filters.Filters
import me.melijn.llklient.io.filters.Karaoke
import me.melijn.llklient.io.filters.Timescale
import me.melijn.llklient.player.event.IPlayerEventListener

interface IPlayer {
    val playingTrack: AudioTrack?

    suspend fun playTrack(track: AudioTrack)
    suspend fun stopTrack()

    var paused: Boolean
    suspend fun setPaused(value: Boolean)

    val trackPosition: Long

    suspend fun seekTo(position: Long)

    val filters: Filters

    fun addListener(listener: IPlayerEventListener)
    fun removeListener(listener: IPlayerEventListener)
}
