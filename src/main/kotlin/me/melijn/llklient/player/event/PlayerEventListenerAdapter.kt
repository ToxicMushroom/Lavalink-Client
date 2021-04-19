package me.melijn.llklient.player.event

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import me.melijn.llklient.player.IPlayer

abstract class PlayerEventListenerAdapter : IPlayerEventListener {

    /**
     * @param player Audio player
     */
    open fun onPlayerPause(player: IPlayer) {
        // Dummy method
    }

    /**
     * @param player Audio player
     */
    open fun onPlayerResume(player: IPlayer) {
        // Dummy method
    }

    /**
     * @param player Audio player
     * @param track Audio track that started
     */
    open fun onTrackStart(player: IPlayer, track: AudioTrack) {
        // Dummy method
    }

    /**
     * @param player Audio player
     * @param track Audio track that ended
     * @param endReason The reason why the track stopped playing
     */
    open fun onTrackEnd(player: IPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        // Dummy method
    }

    /**
     * @param player Audio player
     * @param track Audio track where the exception occurred
     * @param exception The exception that occurred
     */
    open fun onTrackException(player: IPlayer, track: AudioTrack, exception: Exception) {
        // Dummy method
    }

    /**
     * @param player Audio player
     * @param track Audio track where the exception occurred
     * @param thresholdMs The wait threshold that was exceeded for this event to trigger
     */
    open fun onTrackStuck(player: IPlayer, track: AudioTrack, thresholdMs: Long) {
        // Dummy method
    }

    override fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerPauseEvent -> {
                onPlayerPause(event.player)
            }
            is PlayerResumeEvent -> {
                onPlayerResume(event.player)
            }
            is TrackStartEvent -> {
                onTrackStart(event.player, event.track)
            }
            is TrackEndEvent -> {
                onTrackEnd(event.player, event.track, event.reason)
            }
            is TrackExceptionEvent -> {
                onTrackException(
                    event.player,
                    event.track,
                    event.exception
                )
            }
            is TrackStuckEvent -> {
                onTrackStuck(
                    event.player,
                    event.track,
                    event.thresholdMs
                )
            }
        }
    }
}
