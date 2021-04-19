package me.melijn.llklient.player.event

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.llklient.player.IPlayer


class TrackStuckEvent(player: IPlayer, val track: AudioTrack, val thresholdMs: Long) : PlayerEvent(player) {

    val strackTraceElements: Array<StackTraceElement> = arrayOf()

}
