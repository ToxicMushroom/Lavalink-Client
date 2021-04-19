package me.melijn.llklient.player.event

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException

abstract class AudioEventAdapterWrapped : AudioEventAdapter(), IPlayerEventListener {
    private val player: AudioPlayer? = null
    override fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerPauseEvent -> {
                onEvent(com.sedmelluq.discord.lavaplayer.player.event.PlayerPauseEvent(player))
            }
            is PlayerResumeEvent -> {
                onEvent(com.sedmelluq.discord.lavaplayer.player.event.PlayerResumeEvent(player))
            }
            is TrackStartEvent -> {
                onEvent(
                    com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent(
                        player,
                        event.track
                    )
                )
            }
            is TrackEndEvent -> {
                onEvent(
                    com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent(
                        player,
                        event.track,
                        event.reason
                    )
                )
            }
            is TrackExceptionEvent -> {
                val e: Exception = event.exception
                val fe = if (e is FriendlyException) e else FriendlyException(
                    "Unexpected exception",
                    FriendlyException.Severity.SUSPICIOUS,
                    e
                )
                onEvent(
                    com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent(
                        player,
                        event.track,
                        fe
                    )
                )
            }
            is TrackStuckEvent -> {
                onEvent(
                    com.sedmelluq.discord.lavaplayer.player.event.TrackStuckEvent(
                        player,
                        event.track,
                        event.thresholdMs,
                        event.strackTraceElements
                    )
                )
            }
        }
    }
}
