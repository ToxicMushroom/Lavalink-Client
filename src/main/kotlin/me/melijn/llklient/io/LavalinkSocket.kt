package me.melijn.llklient.io


import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import me.melijn.llklient.player.LavalinkPlayer
import me.melijn.llklient.player.event.*
import me.melijn.llklient.utils.LavalinkUtil
import net.dv8tion.jda.api.utils.data.DataObject
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI


class LavalinkSocket internal constructor(
    val name: String,
    private val lavalink: Lavalink<*>,
    private val serverUri: URI,
    private val headerMap: Map<String, String>
) {

    lateinit var client: ClientWebSocketSession

    var connected = false
    var connecting = false


    suspend fun open() {
        connecting = true
        try {
            client = httpClient.webSocketSession(HttpMethod.Companion.Get, serverUri.host, serverUri.port) {
                for ((key, value) in headerMap) {
                    header(key, value)
                }
            }
        } catch (t: Throwable) {
            connecting = false
            connected = false
            available = false
            return
        }
        onOpen()

        // Configure resuming (kinda buggy idk why)
//        client.send(
//            DataObject.empty()
//            .put("op", "configureResuming")
//            .put("key", headerMap["Resume-Key"])
//            .put("timeout", 4)
//            .toString()
//        )

        CoroutineScope(Dispatchers.Default).launch {
            try {
                client.incoming.consumeEach { frame ->
                    if (frame is Frame.Text)
                        onMessage(frame.readText())
                }

            } catch (e: ClosedReceiveChannelException) {
                onClose(0, e.message ?: "unknown")

            } catch (io: IOException) {

                onClose(CloseReason.Codes.INTERNAL_ERROR.code.toInt(), io.message ?: "unknown")
                client.close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "IO Error, timeout perhaps"))

            } catch (e: Throwable) {
                log.error("Error on client side, reconnecting nodes", e)
                onClose(1000, "error on client side")
                client.close(CloseReason(CloseReason.Codes.NORMAL, "encountered an error on client side"))
            }

            if (connected) {
                onClose(1000, "Remote closed")
                client.close(CloseReason(CloseReason.Codes.NORMAL, "Remote closed"))
            }
        }
    }

    val password: String = headerMap["Authorization"].toString()

    var stats: RemoteStats? = null
        private set

    var lastReconnectAttempt: Long = 0
    private var reconnectsAttempted = 0
    val restClient: LavalinkRestClient = LavalinkRestClient(this)

    val remoteUri: URI = serverUri
    var available = false

    private suspend fun onOpen() {
        log.info("Received handshake from server")
        connected = true
        connecting = false
        available = true
        lavalink.loadBalancer.onNodeConnect(this)
        reconnectsAttempted = 0
    }

    private fun onMessage(message: String) {
        val json = DataObject.fromJson(message)
        if (json.getString("op") != "playerUpdate") {
            log.debug(message)
        }
        when (json.getString("op")) {
            "playerUpdate" -> lavalink.getLink(json.getString("guildId").toLong(), lavalink.defaultGroupId)
                .player.provideState(json.getObject("state"))
            "stats" -> stats = RemoteStats(json)
            "event" -> try {
                handleEvent(json)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
            else -> log.warn("Unexpected operation: " + json.getString("op"))
        }
    }

    /**
     * Implementation details:
     * The only events extending [lavalink.client.player.event.PlayerEvent] produced by the remote server are these:
     * 1. TrackEndEvent
     * 2. TrackExceptionEvent
     * 3. TrackStuckEvent
     * 4. WebSocketClosedEvent
     */
    private fun handleEvent(json: DataObject) {
        val link: Link = lavalink.getLink(json.getString("guildId").toLong(), lavalink.defaultGroupId)
        val player: LavalinkPlayer = link.player
        var event: PlayerEvent? = null
        when (json.getString("type")) {
            "TrackEndEvent" -> event = TrackEndEvent(
                player,
                LavalinkUtil.toAudioTrackWithData(player, json.getString("track")),
                AudioTrackEndReason.valueOf(json.getString("reason"))
            )
            "TrackExceptionEvent" -> {
                val ex: Exception = if (json.hasKey("exception")) {

                    val jsonEx = json.getObject("exception")
                    FriendlyException(
                        jsonEx.getString("message", "null"),
                        FriendlyException.Severity.valueOf(jsonEx.getString("severity", "null")),
                        RuntimeException(jsonEx.getString("cause", "null"))
                    )

                } else {
                    RemoteTrackException(json.getString("error", "null"))
                }
                event = TrackExceptionEvent(
                    player,
                    LavalinkUtil.toAudioTrackWithData(player, json.getString("track")), ex
                )
            }
            "TrackStuckEvent" -> event = TrackStuckEvent(
                player,
                LavalinkUtil.toAudioTrackWithData(player, json.getString("track")),
                json.getLong("thresholdMs")
            )
            "WebSocketClosedEvent" -> { // THX FOR THE AWESOME NAMING? THIS IS FOR VOICE!!!

                // Unlike the other events, this is handled by the Link instead of the LavalinkPlayer,
                // as this event is more relevant to the implementation of Link.
                link.onVoiceWebSocketClosed(
                    json.getInt("code"),
                    json.getString("reason"),
                    json.getBoolean("byRemote")
                )
            }
            "TrackStartEvent" -> event =
                TrackStartEvent(player, LavalinkUtil.toAudioTrackWithData(player, json.getString("track")))
            else -> log.warn("Unexpected event type: " + json.getString("type"))
        }
        if (event != null) {
            player.emitEvent(event)
        }
    }

    private suspend fun onClose(code: Int, reason: String) {
        available = false
        connected = false
        connecting = false

        if (code == 1000) {
            log.warn("Connection to $remoteUri closed gracefully with reason: $reason")
        } else {
            log.warn("Connection to $remoteUri closed unexpectedly with reason $code: $reason")
        }

        try {
            lavalink.loadBalancer.onNodeDisconnect(this)
        } catch (ex: IllegalStateException) {
            log.warn(ex.message ?: "")
        }
    }

    suspend fun send(text: String) {
        if (connected && available) {
            client.send(text)
        } else {
            log.warn("Tried to send $text when node was not connected")
        }
    }

    suspend fun attemptReconnect() {
        lastReconnectAttempt = System.currentTimeMillis()
        reconnectsAttempted++
        open()
    }

    fun getReconnectInterval(): Long {
        return (reconnectsAttempted * 2000 - 2000).toLong()
    }

    override fun toString(): String {
        return "LavalinkSocket{" +
                "name=" + name +
                ",remoteUri=" + remoteUri +
                '}'
    }

    suspend fun close() {
        client.close(CloseReason(CloseReason.Codes.NORMAL, "Normal"))
        onClose(CloseReason.Codes.NORMAL.code.toInt(), "Normal")
    }

    companion object {
        private val log = LoggerFactory.getLogger(LavalinkSocket::class.java)
        private const val TIMEOUT_MS = 5000
        private val httpClient = HttpClient(OkHttp) {
            install(WebSockets)
        }
    }
}
