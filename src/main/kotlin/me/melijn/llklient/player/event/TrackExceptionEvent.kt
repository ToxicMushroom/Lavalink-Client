package me.melijn.llklient.player.event

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.llklient.player.IPlayer

class TrackExceptionEvent(player: IPlayer, val track: AudioTrack, val exception: Exception) :
    PlayerEvent(player)
