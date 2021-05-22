package me.melijn.llklient.io.filters


data class Distortion(
    var sinOffset: Float = 0.0f,
    var sinScale: Float = 1.0f,
    var cosOffset: Float = 0.0f,
    var cosScale: Float = 1.0f,
    var tanOffset: Float = 0.0f,
    var tanScale: Float = 1.0f,
    var offset: Float = 0.0f,
    var scale: Float = 1.0f
)