package me.melijn.llklient.io

import me.melijn.llklient.threading.RunnableTask
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

abstract class Lavalink<T : Link> constructor(private val userId: Long, val shardCount: Int) {
    private val log = LoggerFactory.getLogger(Lavalink::class.java)

    val linkMap: ConcurrentHashMap<Long, T> = ConcurrentHashMap()
    val links: Collection<T>
        get() = linkMap.values

    val nodeMap: MutableMap<String, MutableList<LavalinkSocket>> = mutableMapOf()
    val loadBalancer = LavalinkLoadBalancer(this)
    var defaultGroupId: String = ""


    private var reconnectService: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { r: Runnable ->
            val thread = Thread(r, "lavalink-reconnect-thread")
            thread.isDaemon = true
            thread
        }

    init {
        reconnectService.scheduleWithFixedDelay(RunnableTask(ReconnectTask(this).task), 0, 500, TimeUnit.MILLISECONDS)
    }

    private val nodeCounter = AtomicInteger(0)

    open suspend fun addNode(groupId: String, serverUri: URI, password: String) {
        addNode(groupId, "Lavalink_${groupId}_Node_#" + nodeCounter.getAndIncrement(), serverUri, password)
    }

    fun setMainGroup(groupId: String) {
        defaultGroupId = groupId
    }


    /**
     * @param name
     * A name to identify this node. May show up in metrics and other places.
     * @param serverUri
     * uri of the node to be added
     * @param password
     * password of the node to be added
     */
    open suspend fun addNode(
        groupId: String,
        name: String,
        serverUri: URI,
        password: String
    ) {
        val headers = HashMap<String, String>()
        headers["Authorization"] = password
        headers["Num-Shards"] = shardCount.toString()
        headers["User-Id"] = userId.toString()
        headers["Resume-Key"] = "$userId"
        headers["Client-Name"] = "Lavalink-Klient"
        val socket = LavalinkSocket(name, this, serverUri, headers)

        val mutableNodeList = nodeMap.getOrDefault(groupId, mutableListOf())
        mutableNodeList.add(socket)
        nodeMap[groupId] = mutableNodeList

        socket.open()
    }

    open suspend fun removeNode(groupId: String, key: Int) {
        val mutableNodeList = nodeMap[groupId] ?: return
        val node: LavalinkSocket = mutableNodeList.removeAt(key)
        node.close()
        nodeMap[groupId] = mutableNodeList
    }

    open fun getLink(guildId: Long, groupId: String): T {
        return linkMap.computeIfAbsent(
            guildId
        ) { buildNewLink(it, groupId) }
    }

    open fun getExistingLink(guildId: Long): T? {
        return linkMap[guildId]
    }

    /**
     * Hook to build a new Link.
     * Since the Link class is abstract, you will have to return your own implementation of Link.
     *
     * @param guildId the associated guild's ID
     * @return the new link
     */
    protected abstract fun buildNewLink(guildId: Long, groupId: String): T

    open suspend fun shutdown() {
        reconnectService.shutdown()
        nodeMap.forEach { (_, nodeList) ->
            nodeList.forEach { socket ->
                socket.close()
            }
        }
    }

    open fun removeDestroyedLink(link: Link) {
        log.debug("Destroyed link for guild " + link.guildId)
        linkMap.remove(link.guildId)
    }
}