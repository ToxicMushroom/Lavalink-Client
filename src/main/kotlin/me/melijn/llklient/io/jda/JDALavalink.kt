package me.melijn.llklient.io.jda

import me.melijn.llklient.io.Lavalink
import me.melijn.llklient.utils.LavalinkUtil
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReconnectedEvent
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class JDALavalink(
    userId: Long,
    shardCount: Int,
    val jdaProvider: (Int) -> JDA?
) : Lavalink<JDALink>(userId, shardCount), EventListener {

    private val log: Logger =
        LoggerFactory.getLogger(JDALavalink::class.java)
    var autoReconnect = true
    val voiceInterceptor: JDAVoiceInterceptor = JDAVoiceInterceptor(this)


    fun getJda(shardId: Int): JDA {
        return jdaProvider(shardId) ?: throw IllegalArgumentException("please provide non null jda objects")
    }

    fun getJdaFromGuildId(guildId: Long): JDA {
        return jdaProvider(LavalinkUtil.getShardFromSnowflake(guildId, shardCount))
            ?: throw IllegalArgumentException("please provide correct shardCount and guildId")
    }

    override fun onEvent(event: GenericEvent) {
        when (event) {
            is ReconnectedEvent -> {
                if (autoReconnect) {
                    linkMap.forEach { (guildId, link) ->
                        try {
                            //Note: We also ensure that the link belongs to the JDA object
                            val channel = link.lastChannel
                            if (channel != null && event.getJDA().getGuildById(guildId) != null) {
                                event.jda.getVoiceChannelById(channel)?.let { vc ->
                                    link.connect(vc, false)
                                }
                            }
                        } catch (e: Exception) {
                            log.error("Caught exception while trying to reconnect link $link", e)
                        }
                    }
                }
            }
            is GuildLeaveEvent -> {
                val link: JDALink = linkMap[event.guild.idLong] ?: return
                link.removeConnection()
            }
            is VoiceChannelDeleteEvent -> {
                val link: JDALink = linkMap[event.guild.idLong] ?: return
                if (event.channel.idLong != link.lastChannel) return
                link.removeConnection()
            }
        }
    }

    override fun buildNewLink(guildId: Long, groupId: String): JDALink {
        return JDALink(this, guildId, groupId)
    }
}
