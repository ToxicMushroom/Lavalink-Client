package me.melijn.llklient.player

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import me.melijn.llklient.player.event.PlayerEventListenerAdapter

internal class LavalinkInternalPlayerEventHandler : PlayerEventListenerAdapter() {
    override fun onTrackEnd(player: IPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason != AudioTrackEndReason.REPLACED && endReason != AudioTrackEndReason.STOPPED) {
            (player as LavalinkPlayer).clearTrack()
        }
    }
}
