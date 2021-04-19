package me.melijn.llklient.io

import me.melijn.llklient.player.LavalinkPlayer
import net.dv8tion.jda.api.utils.data.DataObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class Link(open var lavalink: Lavalink<*>, var guildId: Long, var groupId: String) {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)
    private var lastVoiceServerUpdate: DataObject? = null
    private var lastSessionId: String? = null

    var player: LavalinkPlayer = LavalinkPlayer(this)

    var lastChannel: Long? = null

    private var node: LavalinkSocket? = null

    /* May only be set by setState() */
    var state = State.NOT_CONNECTED
        @Synchronized
        set(value) {
            check(!(this.state == State.DESTROYED && value != State.DESTROYED)) { "Cannot change state to " + value + " when state is " + State.DESTROYED }
            check(!(this.state == State.DESTROYING && value != State.DESTROYED)) { "Cannot change state to " + value + " when state is " + State.DESTROYING }
            log.debug("Link {} changed state from {} to {}", this, this.state, value)
            field = value
        }


    open suspend fun getRestClient(): LavalinkRestClient {
        val node: LavalinkSocket = getNode(true) ?: throw IllegalStateException("No available nodes!")
        return node.restClient
    }

    open fun disconnect() {
        state = State.DISCONNECTING
        queueAudioDisconnect()
    }

    open suspend fun changeNode(newNode: LavalinkSocket) {
        node = newNode
        if (lastVoiceServerUpdate != null) {
            onVoiceServerUpdate(getLastVoiceServerUpdate(), lastSessionId)
            player.onNodeChange()
        }
    }

    open suspend fun changeGroup(groupId: String) {
        this.groupId = groupId
        changeNode(lavalink.loadBalancer.determineBestSocket(guildId, groupId))
    }

    /**
     * Invoked when we get a voice state update telling us that we have disconnected.
     */
    open suspend fun onDisconnected() {
        state = State.NOT_CONNECTED
        val socket: LavalinkSocket? = getNode(false)
        if (socket != null && state != State.DESTROYING && state != State.DESTROYED) {
            socket.send(
                DataObject.empty()
                    .put("op", "destroy")
                    .put("guildId", guildId.toString())
                    .toString()
            )
            node = null
        }
    }

    /**
     * Disconnects the voice connection (if any) and internally dereferences this [Link].
     *
     *
     * You should invoke this method when your bot leaves a guild.
     */
    open suspend fun destroy() {
        val shouldDisconnect = state != State.DISCONNECTING && state != State.NOT_CONNECTED
        state = State.DESTROYING
        if (shouldDisconnect) {
            try {
                queueAudioDisconnect()
            } catch (ignored: RuntimeException) {
                // This could fail in case we are not in a guild.
                // In that case, we are already disconnected
            }
        }
        state = State.DESTROYED
        lavalink.removeDestroyedLink(this)
        getNode(false)?.send(
            DataObject.empty()
                .put("op", "destroy")
                .put("guildId", guildId.toString())
                .toString()
        )
    }

    /**
     * Disconnects the voice connection (if any) and internally dereferences this [Link].
     *
     *
     * You should invoke this method when your bot is retarded.
     */
    open suspend fun forceDestroy() {
        queueAudioDisconnect()
        lavalink.removeDestroyedLink(this)
        getNode(false)?.send(
            DataObject.empty()
                .put("op", "destroy")
                .put("guildId", guildId.toString())
                .toString()
        )
    }

    protected abstract fun removeConnection()
    protected abstract fun queueAudioDisconnect()
    protected abstract fun queueAudioConnect(channelId: Long)

    /**
     * @return The current node
     */
    open suspend fun getNode(): LavalinkSocket? {
        return getNode(false)
    }

    /**
     * @param selectIfAbsent If true determines a new socket if there isn't one yet
     * @return The current node
     */
    open suspend fun getNode(selectIfAbsent: Boolean): LavalinkSocket? {
        if (selectIfAbsent && node == null) {
            node = lavalink.loadBalancer.determineBestSocket(guildId)
            player.onNodeChange()
        }
        return node
    }

    /**
     * @return The channel we are currently connect to
     */
    open fun getCurrentChannel(): Long? {
        return if (lastChannel == null || state == State.DESTROYED || state == State.NOT_CONNECTED) {
            null
        } else {
            lastChannel
        }
    }

    override fun toString(): String {
        return "Link{" +
                "guild='" + guildId + '\'' +
                ", channel='" + lastChannel + '\'' +
                ", state=" + state +
                '}'
    }

    open suspend fun onVoiceServerUpdate(json: DataObject?, sessionId: String?) {
        lastVoiceServerUpdate = json
        lastSessionId = sessionId

        // Send WS message
        val out = DataObject.empty()
        out.put("op", "voiceUpdate")
        out.put("sessionId", sessionId)
        out.put("guildId", guildId.toString())
        out.put("event", lastVoiceServerUpdate)
        getNode(true)?.send(out.toString())
        state = State.CONNECTED
    }

    open fun getLastVoiceServerUpdate(): DataObject? {
        return lastVoiceServerUpdate
    }

    /**
     * Invoked when the remote Lavalink server reports that this Link's WebSocket to the voice server was closed.
     * This could be because of an expired voice session, that might have to be renewed.
     *
     * @param code the RFC 6455 close code.
     * @param reason the reason for closure, provided by the closing peer.
     * @param byRemote true if closed by Discord, false if closed by the Lavalink server.
     */
    open fun onVoiceWebSocketClosed(code: Int, reason: String, byRemote: Boolean) {

    }

    enum class State {
        /**
         * Default, means we are not trying to use voice at all
         */
        NOT_CONNECTED,

        /**
         * Waiting for VOICE_SERVER_UPDATE
         */
        CONNECTING,

        /**
         * We have dispatched the voice server info to the server, and it should (soon) be connected.
         */
        CONNECTED,

        /**
         * Waiting for confirmation from Discord that we have connected
         */
        DISCONNECTING,

        /**
         * This [Link] is being destroyed
         */
        DESTROYING,

        /**
         * This [Link] has been destroyed and will soon (if not already) be unmapped from [Lavalink]
         */
        DESTROYED
    }

}