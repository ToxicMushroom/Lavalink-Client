package me.melijn.llklient.player

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.llklient.io.LavalinkSocket
import me.melijn.llklient.io.Link
import me.melijn.llklient.io.filters.*
import me.melijn.llklient.player.event.IPlayerEventListener
import me.melijn.llklient.player.event.PlayerEvent
import me.melijn.llklient.player.event.PlayerPauseEvent
import me.melijn.llklient.player.event.PlayerResumeEvent
import me.melijn.llklient.utils.LavalinkUtil
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min


class LavalinkPlayer(private val link: Link) : IPlayer {

    override var playingTrack: AudioTrack? = null
        private set
    override val filters: Filters by lazy { Filters(this, this::onCommit) }

    private var updateTime: Long = -1
    private var position: Long = -1

    private val listeners: MutableList<IPlayerEventListener> = CopyOnWriteArrayList()

    /**
     * Invoked by [Link] to make sure we keep playing music on the new node
     *
     *
     * Used when we are moved to a new socket
     */
    suspend fun onNodeChange() {
        val track = playingTrack
        if (track != null) {
            track.position = trackPosition
            playTrack(track)
        }
    }

    override suspend fun playTrack(track: AudioTrack) {
        try {
            position = track.position
            val trackData: TrackData? = track.getUserData(TrackData::class.java)
            val json = DataObject.empty()
            json.put("op", "play")
            json.put("guildId", link.guildId.toString())
            json.put("track", LavalinkUtil.toMessage(track))
            json.put("startTime", position)
            if (trackData != null) {
                json.put("startTime", trackData.startPos)
                json.put("endTime", trackData.endPos)
            }
            json.put("pause", this.paused)
            link.getNode(true)?.send(json.toString())
            updateTime = System.currentTimeMillis()
            playingTrack = track
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    override suspend fun stopTrack() {
        playingTrack = null
        val node: LavalinkSocket = link.getNode(false) ?: return
        val json = DataObject.empty()
        json.put("op", "stop")
        json.put("guildId", link.guildId.toString())
        node.send(json.toString())
    }

    override var paused: Boolean = false

    override suspend fun setPaused(value: Boolean) {
        if (value == this.paused) return

        val node: LavalinkSocket? = link.getNode(false)
        if (node != null) {
            val json = DataObject.empty()
            json.put("op", "pause")
            json.put("guildId", link.guildId.toString())
            json.put("pause", value)
            node.send(json.toString())
        }
        this.paused = value

        if (value) {
            emitEvent(PlayerPauseEvent(this))
        } else {
            emitEvent(PlayerResumeEvent(this))
        }
    }


    // Account for the time since our last update
    override val trackPosition: Long
        get() {
            val playing = playingTrack
            checkNotNull(playing) { "Not currently playing anything" }
            return if (!this.paused) {
                // Account for the time since our last update
                val timeDiff = System.currentTimeMillis() - updateTime
                (position + timeDiff).coerceAtMost(playing.duration)
            } else {
                position.coerceAtMost(playing.duration)
            }
        }

    override suspend fun seekTo(position: Long) {
        val playing = playingTrack
        checkNotNull(playing) { "Not currently playing anything" }
        check(playing.isSeekable) { "Track cannot be seeked" }
        val json = DataObject.empty()
        json.put("op", "seek")
        json.put("guildId", link.guildId.toString())
        json.put("position", position)
        link.getNode(true)?.send(json.toString())
    }

    fun provideState(json: DataObject) {
        updateTime = json.getLong("time")
        position = json.getLong("position", 0)
    }

    override fun addListener(listener: IPlayerEventListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: IPlayerEventListener) {
        listeners.remove(listener)
    }

    fun emitEvent(event: PlayerEvent) {
        listeners.forEach { listener: IPlayerEventListener ->
            listener.onEvent(
                    event
            )
        }
    }

    fun clearTrack() {
        playingTrack = null
    }

    fun getLink(): Link {
        return link
    }

    var pausedBecauseNoNodes = false
    suspend fun noNodes() {
        if (!paused) {
            pausedBecauseNoNodes = true
            setPaused(true)
        }
    }

    suspend fun yesNodes() {
        if (paused && pausedBecauseNoNodes) {
            pausedBecauseNoNodes = false
            setPaused(false)
        }
    }

    private suspend fun onCommit() {
        val node = link.getNode(false) ?: return

        val json = DataObject.empty()
        json.put("op", "filters")
        json.put("guildId", link.guildId.toString())

        // Volume
        json.put("volume", filters.volume / 100.0f)

        // Equalizer
        val bands = DataArray.empty()
        var i = -1
        for (f in filters.bands) {
            i++
            if (f == 0.0f) continue
            val obj = DataObject.empty()
            obj.put("band", i)
            obj.put("gain", f)
            bands.add(obj)
        }
        if (bands.length() > 0) json.put("equalizer", bands)

        val timescale: Timescale? = filters.timescale
        if (timescale != null) {
            val obj = DataObject.empty()
            obj.put("speed", timescale.speed)
            obj.put("pitch", timescale.pitch)
            obj.put("rate", timescale.rate)
            json.put("timescale", obj)
        }

        val karaoke: Karaoke? = filters.karaoke
        if (karaoke != null) {
            val obj = DataObject.empty()
            obj.put("level", karaoke.level)
            obj.put("monoLevel", karaoke.monoLevel)
            obj.put("filterBand", karaoke.filterBand)
            obj.put("filterWidth", karaoke.filterWidth)
            json.put("karaoke", obj)
        }

        val tremolo: Tremolo? = filters.tremolo
        if (tremolo != null) {
            val obj = DataObject.empty()
            obj.put("frequency", tremolo.frequency)
            obj.put("depth", tremolo.depth)
            json.put("tremolo", obj)
        }

        val vibrato: Vibrato? = filters.vibrato
        if (vibrato != null) {
            val obj = DataObject.empty()
            obj.put("frequency", vibrato.frequency)
            obj.put("depth", vibrato.depth)
            json.put("vibrato", obj)
        }

        node.send(json.toString())
    }

    /**
     * Constructor only for internal use
     *
     * @param link the parent link
     */
    init {
        addListener(LavalinkInternalPlayerEventHandler())
    }
}

