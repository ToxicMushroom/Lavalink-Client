package me.melijn.llklient.io.filters

class Vibrato {
    var frequency = 2.0f
        set(value) {
            if (frequency <= 0 || frequency >= 14) throw IllegalArgumentException("Frequency must be >0 and <=14")
            field = value
        }

    var depth = 1.0f
        set(value) {
            if (depth <= 0 || depth >= 1) throw IllegalArgumentException("Depth must be >0 and <=1")
            field = value
        }
}