package org.example

object NetworkConfig {
    const val BASE_PORT = 8000
    const val MAX_SERVERS = 10
    val PORT_RANGE = BASE_PORT..(BASE_PORT + MAX_SERVERS)
    const val CONNECTION_TIMEOUT = 2000L
}
