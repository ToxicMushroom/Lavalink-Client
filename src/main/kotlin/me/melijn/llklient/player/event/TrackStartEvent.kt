package me.melijn.llklient.player.event

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.llklient.player.IPlayer


class TrackStartEvent(player: IPlayer, val track: AudioTrack) : PlayerEvent(player)
