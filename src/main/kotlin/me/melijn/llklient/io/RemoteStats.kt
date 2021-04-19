package me.melijn.llklient.io

import net.dv8tion.jda.api.utils.data.DataObject

class RemoteStats internal constructor(private val asJson: DataObject) {
    val players: Int = asJson.getInt("players")
    val playingPlayers: Int = asJson.getInt("playingPlayers")

    //in millis
    val uptime //in millis
            : Long = asJson.getLong("uptime")

    // In bytes
    val memFree: Long
    val memUsed: Long
    val memAllocated: Long
    val memReservable: Long
    val cpuCores: Int
    val systemLoad: Double
    val lavalinkLoad: Double
    var avgFramesSentPerMinute = -1
    var avgFramesNulledPerMinute = -1
    var avgFramesDeficitPerMinute = -1

    override fun toString(): String {
        return "RemoteStats{" +
                "players=" + players +
                ", playingPlayers=" + playingPlayers +
                ", uptime=" + uptime +
                ", memFree=" + memFree +
                ", memUsed=" + memUsed +
                ", memAllocated=" + memAllocated +
                ", memReservable=" + memReservable +
                ", cpuCores=" + cpuCores +
                ", systemLoad=" + systemLoad +
                ", lavalinkLoad=" + lavalinkLoad +
                ", avgFramesSentPerMinute=" + avgFramesSentPerMinute +
                ", avgFramesNulledPerMinute=" + avgFramesNulledPerMinute +
                ", avgFramesDeficitPerMinute=" + avgFramesDeficitPerMinute +
                '}'
    }

    init {
        val memory = asJson.getObject("memory")
        memFree = memory.getLong("free")
        memUsed = memory.getLong("used")
        memAllocated = memory.getLong("allocated")
        memReservable = memory.getLong("reservable")
        val cpu = asJson.getObject("cpu")
        cpuCores = cpu.getInt("cores")
        systemLoad = cpu.getInt("systemLoad").toDouble()
        lavalinkLoad = cpu.getInt("lavalinkLoad").toDouble()
        val frames = if (asJson.hasKey("frameStats")) {
            asJson.getObject("frameStats")
        } else {
            null
        }
        if (frames != null) {
            avgFramesSentPerMinute = frames.getInt("sent")
            avgFramesNulledPerMinute = frames.getInt("nulled")
            avgFramesDeficitPerMinute = frames.getInt("deficit")
        }
    }
}
