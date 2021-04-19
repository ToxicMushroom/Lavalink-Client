package me.melijn.llklient.io.filters

class Timescale {
    var speed: Float = 1.0f
        set(value) {
            if (speed < 0) throw IllegalArgumentException("Speed must be greater than 0")
            field = value
        }

    var pitch: Float = 1.0f
        set(value) {
            if (pitch < 0) throw IllegalArgumentException("Pitch must be greater than 0")
            field = value
        }

    var rate: Float = 1.0f
        set(value) {
            if (rate < 0) throw IllegalArgumentException("Rate must be greater than 0")
            field = value
        }
}
