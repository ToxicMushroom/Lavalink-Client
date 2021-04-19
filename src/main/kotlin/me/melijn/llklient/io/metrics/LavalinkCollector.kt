package me.melijn.llklient.io.metrics

import io.prometheus.client.Collector
import io.prometheus.client.GaugeMetricFamily
import me.melijn.llklient.io.Lavalink
import me.melijn.llklient.io.LavalinkSocket
import me.melijn.llklient.io.Link
import me.melijn.llklient.io.RemoteStats
import java.util.*

class LavalinkCollector(val lavalink: Lavalink<Link>) {


    fun collect(): List<Collector.MetricFamilySamples>? {
        val mfs: MutableList<Collector.MetricFamilySamples> = ArrayList()
        val labelNames = listOf("node")
        val players = GaugeMetricFamily(
            "lavalink_players_current",
            "Amount of players", labelNames
        )
        mfs.add(players)
        val playingPlayers = GaugeMetricFamily(
            "lavalink_playing_players_current",
            "Amount of playing players", labelNames
        )
        mfs.add(playingPlayers)
        val uptimeSeconds = GaugeMetricFamily(
            "lavalink_uptime_seconds",
            "Uptime of the node", labelNames
        )
        mfs.add(uptimeSeconds)
        val memFree = GaugeMetricFamily(
            "lavalink_mem_free_bytes",
            "Amount of free memory", labelNames
        )
        mfs.add(memFree)
        val memUsed = GaugeMetricFamily(
            "lavalink_mem_used_bytes",
            "Amount of used memory", labelNames
        )
        mfs.add(memUsed)
        val memAllocated = GaugeMetricFamily(
            "lavalink_mem_allocated_bytes",
            "Amount of allocated memory", labelNames
        )
        mfs.add(memAllocated)
        val memReservable = GaugeMetricFamily(
            "lavalink_mem_reservable_bytes",
            "Amount of reservable memory", labelNames
        )
        mfs.add(memReservable)
        val cpuCores = GaugeMetricFamily(
            "lavalink_cpu_cores",
            "Amount of cpu cores", labelNames
        )
        mfs.add(cpuCores)
        val systemLoad = GaugeMetricFamily(
            "lavalink_load_system",
            "Total load of the system", labelNames
        )
        mfs.add(systemLoad)
        val lavalinkLoad = GaugeMetricFamily(
            "lavalink_load_lavalink",
            "Load caused by Lavalink", labelNames
        )
        mfs.add(lavalinkLoad)
        val averageFramesSentPerMinute = GaugeMetricFamily(
            "lavalink_average_frames_sent_per_minute",
            "Average frames sent per minute", labelNames
        )
        mfs.add(averageFramesSentPerMinute)
        val averageFramesNulledPerMinute = GaugeMetricFamily(
            "lavalink_average_frames_nulled_per_minute",
            "Average frames nulled per minute", labelNames
        )
        mfs.add(averageFramesNulledPerMinute)
        val averageFramesDeficitPerMinute = GaugeMetricFamily(
            "lavalink_average_frames_deficit_per_minute",
            "Average frames deficit per minute", labelNames
        )
        mfs.add(averageFramesDeficitPerMinute)
        val ls = mutableListOf<LavalinkSocket>()
        for (entry in lavalink.nodeMap) {
            ls.addAll(entry.value)
        }

        val nodes: List<LavalinkSocket> = ls
        for (node in nodes) {
            val labels: List<String> = listOf(node.name)
            val stats: RemoteStats = node.stats ?: continue
            players.addMetric(labels, stats.players.toDouble())
            playingPlayers.addMetric(labels, stats.playingPlayers.toDouble())
            uptimeSeconds.addMetric(labels, (stats.uptime / 1000).toDouble())
            memFree.addMetric(labels, stats.memFree.toDouble())
            memUsed.addMetric(labels, stats.memUsed.toDouble())
            memAllocated.addMetric(labels, stats.memAllocated.toDouble())
            memReservable.addMetric(labels, stats.memReservable.toDouble())
            cpuCores.addMetric(labels, stats.cpuCores.toDouble())
            systemLoad.addMetric(labels, stats.systemLoad)
            lavalinkLoad.addMetric(labels, stats.lavalinkLoad)
            averageFramesSentPerMinute.addMetric(labels, stats.avgFramesSentPerMinute.toDouble())
            averageFramesNulledPerMinute.addMetric(labels, stats.avgFramesNulledPerMinute.toDouble())
            averageFramesDeficitPerMinute.addMetric(labels, stats.avgFramesDeficitPerMinute.toDouble())
        }
        return mfs
    }
}