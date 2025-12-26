package org.example

import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    val serverName: String,
    val port: Int,
)
