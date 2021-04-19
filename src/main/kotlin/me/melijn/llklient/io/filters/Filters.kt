package me.melijn.llklient.io.filters

import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer.BAND_COUNT
import me.melijn.llklient.player.IPlayer
import kotlin.reflect.KSuspendFunction0


class Filters(
        private val player: IPlayer,
        private val onCommit: KSuspendFunction0<Unit>
) {

    /**
     * @param volume where 1.0f is regular volume. Values greater than 1.0f are allowed, but may cause clipping.
     */
    var volume = DEFAULT_VOLUME
        set(volume) {
            require(volume >= 0) { "Volume must be greater than 0" }
            field = volume
        }

    var karaoke: Karaoke? = null
    var tremolo: Tremolo? = null
    var timescale: Timescale? = null
    var vibrato: Vibrato? = null

    var bands = FloatArray(BAND_COUNT)

    /**
     * Configures the equalizer.
     *
     * @param band the band to change, values 0-14
     * @param gain the gain in volume for the given band, range -0.25 (mute) to 1.0 (quadruple).
     */
    fun setBand(band: Int, gain: Float): Filters {
        require(gain >= -0.25 && gain <= 1) { "Gain must be -0.25 to 1.0" }
        bands[band] = gain
        return this
    }

    fun clear(): Filters {
        volume = DEFAULT_VOLUME
        bands = FloatArray(BAND_COUNT)
        timescale = null
        karaoke = null
        tremolo = null
        vibrato = null
        return this
    }

    /**
     * Commits these filters to the Lavalink server.
     *
     *
     * The client may choose to commit changes at any time, even if this method is never invoked.
     */
    suspend fun commit() {
        onCommit()
    }

    companion object {
        var DEFAULT_VOLUME = 100
    }
}