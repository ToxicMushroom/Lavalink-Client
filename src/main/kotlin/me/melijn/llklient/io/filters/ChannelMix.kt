package me.melijn.llklient.io.filters

data class ChannelMix(
    var leftToLeft: Float = 1.0f,
    var leftToRight: Float = 0.0f,
    var rightToLeft: Float = 0.0f,
    var rightToRight: Float = 1.0f,
)