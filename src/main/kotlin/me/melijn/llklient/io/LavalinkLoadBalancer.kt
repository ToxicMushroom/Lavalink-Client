package me.melijn.llklient.io

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.pow

class LavalinkLoadBalancer internal constructor(private val lavalink: Lavalink<*>) {
    val logger = LoggerFactory.getLogger(this.javaClass)

    //private Map<String, Optional<LavalinkSocket>> socketMap = new ConcurrentHashMap<>();
    private val penaltyProviders: MutableList<PenaltyProvider> = ArrayList()

    suspend fun determineBestSocket(guild: Long, groupId: String? = null): LavalinkSocket {
        val link = lavalink.linkMap[guild] ?: throw IllegalArgumentException("please provide link")
        val cnode = link.getNode(false)
        val bestGroupId: String = groupId ?: if (cnode != null) {
            lavalink.nodeMap.entries.first { (_, sockets) ->
                sockets.contains(cnode)
            }.key
        } else {
            lavalink.defaultGroupId
        }

        return getRandomSocket(bestGroupId)
    }

    fun getRandomSocket(groupId: String): LavalinkSocket {
        var leastPenalty: LavalinkSocket? = null
        var record = Int.MAX_VALUE

        val nodes: List<LavalinkSocket> = lavalink.nodeMap[groupId]
            ?: throw IllegalArgumentException("non valid groupId")

        for (socket in nodes) {
            val total = getPenalties(socket, penaltyProviders).total
            if (total < record) {
                leastPenalty = socket
                record = total
            }
        }
        if (leastPenalty == null || !leastPenalty.available){
            throw IllegalArgumentException("No available nodes for $groupId")
        }
        return leastPenalty
    }

    fun addPenalty(penalty: PenaltyProvider) {
        penaltyProviders.add(penalty)
    }

    fun removePenalty(penalty: PenaltyProvider?) {
        penaltyProviders.remove(penalty)
    }

    suspend fun onNodeDisconnect(disconnected: LavalinkSocket) {
        val links: Collection<Link> = lavalink.links
        links.forEach { link: Link ->
            try {
                if (disconnected == link.getNode(false)) link.changeNode(
                    determineBestSocket(link.guildId, link.groupId)
                )
            } catch (t: IllegalArgumentException) {
                // No available nodes
                logger.warn(t.message ?: "unknown")
                link.player.noNodes()
            }
        }
    }

    suspend fun onNodeConnect(connected: LavalinkSocket) {
        val links: Collection<Link> = lavalink.links
        links.forEach { link: Link ->
            val sockets = lavalink.nodeMap[link.groupId] ?: return@forEach
            val otherAvailableNodes = sockets.stream()
                .filter { node: LavalinkSocket -> node != connected }
                .filter(LavalinkSocket::available)
                .count()

            if (otherAvailableNodes > 0) { //only update links if this is the only connected node
                return@forEach
            }

            link.changeNode(
                connected
            )
            link.player.yesNodes()
        }
    }

    fun getPenalties(
        socket: LavalinkSocket,
        penaltyProviders: List<PenaltyProvider>
    ): Penalties {
        return Penalties(socket, penaltyProviders, lavalink)
    }

    class Penalties(
        private val socket: LavalinkSocket,
        penaltyProviders: List<PenaltyProvider>,
        private val lavalink: Lavalink<*>?
    ) {
        var playerPenalty = 0
        var cpuPenalty = 0
        var deficitFramePenalty = 0
        var nullFramePenalty = 0
        private var customPenalties = 0

        private suspend fun countPlayingPlayers(): Int {
            val links: Collection<Link> = lavalink?.links ?: return 0
            var count = 0
            links.forEach { link ->
                val player = link.player
                val node = link.getNode(false)
                count += if (socket == node && player.playingTrack != null &&
                    !player.paused
                ) {
                    1
                } else 0
            }

            return count
        }

        fun getSocket(): LavalinkSocket {
            return socket
        }

        val total: Int
            get() = if (!socket.available || socket.stats == null) Int.MAX_VALUE - 1 else playerPenalty + cpuPenalty + deficitFramePenalty + nullFramePenalty + customPenalties

        override fun toString(): String {
            return if (!socket.available) "Penalties{" +
                    "unavailable=" + (Int.MAX_VALUE - 1) +
                    '}' else "Penalties{" +
                    "total=" + total +
                    ", playerPenalty=" + playerPenalty +
                    ", cpuPenalty=" + cpuPenalty +
                    ", deficitFramePenalty=" + deficitFramePenalty +
                    ", nullFramePenalty=" + nullFramePenalty +
                    ", custom=" + customPenalties +
                    '}'
        }

        init {
            val stats: RemoteStats? = socket.stats
            if (stats != null) {  // Will return as max penalty anyways
                // This will serve as a rule of thumb. 1 playing player = 1 penalty point
                playerPenalty = if (lavalink != null) {
                    runBlocking { countPlayingPlayers() }
                } else {
                    stats.playingPlayers
                }

                // https://fred.moe/293.png
                cpuPenalty = 1.05.pow(100 * stats.systemLoad).toInt() * 10 - 10

                // -1 Means we don't have any frame stats. This is normal for very young nodes
                if (stats.avgFramesDeficitPerMinute != -1) {
                    // https://fred.moe/rjD.png
                    deficitFramePenalty =
                        (1.03.pow(500f * (stats.avgFramesDeficitPerMinute.toFloat() / 3000f).toDouble()) * 600 - 600).toInt()
                    nullFramePenalty =
                        (1.03.pow(500f * (stats.avgFramesNulledPerMinute.toFloat() / 3000f).toDouble()) * 300 - 300).toInt()
                    nullFramePenalty *= 2
                    // Deficit frames are better than null frames, as deficit frames can be caused by the garbage collector
                }
                penaltyProviders.forEach { pp: PenaltyProvider ->
                    customPenalties += pp.getPenalty(
                        this
                    )
                }
            }
        }
    }

    companion object {
        fun getPenalties(socket: LavalinkSocket): Penalties {
            return Penalties(socket, emptyList(), null)
        }
    }

}
