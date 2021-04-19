package me.melijn.llklient.io.jda

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.melijn.llklient.io.Link
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor.VoiceServerUpdate
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor.VoiceStateUpdate

/**
 * You have to set this class on the JDABuilder or DefaultShardManagerBuilder
 */
class JDAVoiceInterceptor(private val lavalink: JDALavalink) : VoiceDispatchInterceptor {
    override fun onVoiceServerUpdate(update: VoiceServerUpdate) {
        val content = update.toData().getObject("d")

        // Get session
        val guild = update.guild
        val vs = guild.selfMember.voiceState ?: return
        val link = lavalink.getLink(guild.idLong, lavalink.defaultGroupId)

        CoroutineScope(Dispatchers.Default).launch {
            link.onVoiceServerUpdate(content, vs.sessionId)
        }
    }

    override fun onVoiceStateUpdate(update: VoiceStateUpdate): Boolean {
        val channel = update.channel
        val link: JDALink = lavalink.getLink(update.guildIdLong, lavalink.defaultGroupId)
        if (channel == null) {
            // Null channel means disconnected
            if (link.state != Link.State.DESTROYED) {
                CoroutineScope(Dispatchers.Default).launch {
                    link.onDisconnected()
                }

            }
        } else {
            link.lastChannel = channel.idLong // Change expected channel
        }
        return link.state == Link.State.CONNECTED
    }

}
