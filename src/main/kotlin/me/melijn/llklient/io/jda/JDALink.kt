package me.melijn.llklient.io.jda

import me.melijn.llklient.io.Link
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.slf4j.LoggerFactory

class JDALink internal constructor(private val jdaLavalink: JDALavalink, guildId: Long, groupId: String) : Link(jdaLavalink, guildId, groupId) {
    fun connect(voiceChannel: VoiceChannel) {
        connect(voiceChannel, true)
    }

    /**
     * Eventually connect to a channel. Takes care of disconnecting from an existing connection
     *
     * @param channel Channel to connect to
     */
    fun connect(channel: VoiceChannel, checkChannel: Boolean) {
        require(channel.guild == guild) {
            "The provided VoiceChannel is not a part of the Guild that this AudioManager handles." +
                    "Please provide a VoiceChannel from the proper Guild"
        }
        require(
            !channel.jda.unavailableGuilds
                .contains(channel.guild.id)
        ) {
            "Cannot open an Audio Connection with an unavailable guild. " +
                    "Please wait until this Guild is available to open a connection."
        }
        val self = channel.guild.selfMember
        if (!self.hasPermission(channel, Permission.VOICE_CONNECT) && !self.hasPermission(
                channel,
                Permission.VOICE_MOVE_OTHERS
            )
        ) throw InsufficientPermissionException(channel, Permission.VOICE_CONNECT)

        //If we are already connected to this VoiceChannel, then do nothing.
        if (checkChannel && channel == channel.guild.selfMember.voiceState!!.channel) return
        if (channel.guild.selfMember.voiceState!!.inVoiceChannel()) {
            val userLimit = channel.userLimit // userLimit is 0 if no limit is set!
            if (!self.isOwner && !self.hasPermission(Permission.ADMINISTRATOR)) {
                if (userLimit > 0 // If there is a userlimit
                    && userLimit <= channel.members.size // if that userlimit is reached
                    && !self.hasPermission(channel, Permission.VOICE_MOVE_OTHERS)
                ) {
                    throw InsufficientPermissionException(
                        channel, Permission.VOICE_MOVE_OTHERS,  // then throw exception!
                        "Unable to connect to VoiceChannel due to userlimit! Requires permission VOICE_MOVE_OTHERS to bypass"
                    )
                }
            }
        }
        state = State.CONNECTING
        queueAudioConnect(channel.idLong)
    }

    private val jda: JDA
        get() = jdaLavalink.getJdaFromGuildId(guildId)

    public override fun removeConnection() {
        // JDA handles this for us
    }

    override fun queueAudioDisconnect() {
        val guild: Guild? = guild
        if (guild != null) {
            jda.directAudioController.disconnect(guild)
        } else {
            log.warn("Attempted to disconnect, but guild {} was not found", this.guildId)
        }
    }

    override fun queueAudioConnect(channelId: Long) {
        val vc = jda.getVoiceChannelById(channelId)
        if (vc != null) {
            jda.directAudioController.connect(vc)
        } else {
            log.warn("Attempted to connect, but voice channel {} was not found", channelId)
        }
    }

    /**
     * @return the Guild, or null if it doesn't exist
     */
    private val guild: Guild?
        get() = jda.getGuildById(guildId)

    companion object {
        private val log = LoggerFactory.getLogger(JDALink::class.java)
    }

}
