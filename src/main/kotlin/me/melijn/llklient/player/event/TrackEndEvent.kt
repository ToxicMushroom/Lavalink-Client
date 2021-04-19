package me.melijn.llklient.player.event

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import me.melijn.llklient.player.IPlayer

class TrackEndEvent(player: IPlayer, val track: AudioTrack, val reason: AudioTrackEndReason) :
    PlayerEvent(player)
