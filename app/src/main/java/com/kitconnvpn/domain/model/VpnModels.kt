package com.kitconnvpn.domain.model

data class NetworkStats(
    val downloadSpeedMb: String = "0.0",
    val uploadSpeedMb: String = "0.0",
    val pingMs: Int = 0,
    val totalTrafficMb: Double = 0.0,
    val durationSeconds: Long = 0L
)
