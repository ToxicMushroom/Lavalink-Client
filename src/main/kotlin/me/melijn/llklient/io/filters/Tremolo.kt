package me.melijn.llklient.io.filters

class Tremolo {
    var frequency: Float = 2.0f
        set(value) {
            if (frequency <= 0) throw IllegalArgumentException("Frequency must be >0")
            field = value
        }
    var depth: Float = 0.5f
        set(value) {
            if (depth <= 0 || depth >= 1) throw IllegalArgumentException("Frequency must be >0 and <=1")
            field = value
        }
}
