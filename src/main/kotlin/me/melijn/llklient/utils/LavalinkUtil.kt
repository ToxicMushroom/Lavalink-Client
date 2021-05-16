package me.melijn.llklient.utils

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.llklient.player.LavalinkPlayer
import org.apache.commons.codec.binary.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutput
import java.io.IOException

class LavalinkUtil {
    companion object {
        private var PLAYER_MANAGER: AudioPlayerManager = DefaultAudioPlayerManager()

        /**
         *
         * @param player the lavalink player that holds the track with data
         * @param message the Base64 audio track
         * @return the AudioTrack with the user data stored in the player
         */
        fun toAudioTrackWithData(player: LavalinkPlayer, message: String): AudioTrack {
            val storedTrack: AudioTrack? = player.playingTrack
            val messageTrack: AudioTrack = toAudioTrack(message)
            storedTrack?.userData?.let { messageTrack.userData = it }
            return messageTrack
        }

        /**
         *
         * @param message the Base64 audio track
         * @return the AudioTrack
         * @throws IOException if there is an IO problem
         */
        fun toAudioTrack(message: String): AudioTrack {
            return toAudioTrack(Base64.decodeBase64(message))
        }

        /**
         * @param message the unencoded audio track
         * @return the AudioTrack
         * @throws IOException if there is an IO problem
         */
        private fun toAudioTrack(message: ByteArray): AudioTrack {
            return PLAYER_MANAGER.decodeTrack(MessageInput(ByteArrayInputStream(message))).decodedTrack
        }

        /**
         * @param track the track to serialize
         * @return the serialized track a Base64 string
         * @throws IOException if there is an IO problem
         */
        fun toMessage(track: AudioTrack): String {
            return Base64.encodeBase64String(toBinary(track))
        }

        /**
         * @param track the track to serialize
         * @return the serialized track as binary
         * @throws IOException if there is an IO problem
         */
        fun toBinary(track: AudioTrack): ByteArray {
            ByteArrayOutputStream().use { baos ->
                PLAYER_MANAGER.encodeTrack(MessageOutput(baos), track)
                return baos.toByteArray()
            }
        }

        fun getShardFromSnowflake(snowflake: Long, numShards: Int): Int {
            return ((snowflake shr 22) % numShards).toInt()
        }

        init {

            /* These are only to encode/decode messages */
            PLAYER_MANAGER.registerSourceManager(
                YoutubeAudioSourceManager()
            )
            PLAYER_MANAGER.registerSourceManager(BandcampAudioSourceManager())
            PLAYER_MANAGER.registerSourceManager(SoundCloudAudioSourceManager.createDefault())
            PLAYER_MANAGER.registerSourceManager(TwitchStreamAudioSourceManager())
            PLAYER_MANAGER.registerSourceManager(VimeoAudioSourceManager())
            PLAYER_MANAGER.registerSourceManager(HttpAudioSourceManager())
        }
    }


    private fun encodeTrackDetails(track: AudioTrack, output: DataOutput) {
        val sourceManager: AudioSourceManager = track.sourceManager
        output.writeUTF(sourceManager.sourceName)
        sourceManager.encodeTrack(track, output)
    }
}
