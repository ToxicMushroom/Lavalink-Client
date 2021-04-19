package me.melijn.llklient.io

class ReconnectTask(val lavalink: Lavalink<*>) {

    val task = suspend {
        val tempList = mutableListOf<LavalinkSocket>()
        lavalink.nodeMap.values.forEach { tempList.addAll(it) }
        val nodes: List<LavalinkSocket> = tempList
        nodes.forEach { lavalinkSocket: LavalinkSocket ->
            if (!lavalinkSocket.connected
                && !lavalinkSocket.connecting
                && System.currentTimeMillis() - lavalinkSocket.lastReconnectAttempt > lavalinkSocket.getReconnectInterval()
            ) {
                lavalinkSocket.attemptReconnect()
            }
        }
    }
}
