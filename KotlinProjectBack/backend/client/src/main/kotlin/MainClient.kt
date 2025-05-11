@file:Suppress("kt lint:standard:no-wildcard-imports", "ktlint:standard:no-wildcard-imports")

package org.example
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.SocketException

class MainClient<T : IGame.InfoForSending>(
    private val currentGame: IGame<T>,
    private val port: Int,
) {
    private val ip = "10.0.2.2"

    suspend fun startClient() {
        startClient(port).also { socket ->
            if (socket != null) {
                startCommunicate(socket)
            }
        }
    }

    data class ServerInfo(
        val serverName: String,
        val port: Int,
    )

    suspend fun selectServer(): Int {
        val serversInfo = discoverServers()
        val possiblePorts = mutableSetOf<String>()
        serversInfo.forEach { it ->
            println("Server Ip: ${it.serverName}, port: ${it.port}")
            possiblePorts.add(it.port.toString())
        }

        var possiblePort = ""
        while (!possiblePorts.contains(possiblePort)) {
            possiblePort = readln().trim()
            if (!possiblePorts.contains(possiblePort)) {
                println("incorrect port")
            }
        }

        return possiblePort.toInt()
    }

    suspend fun discoverServers(timeout: Long = 3000): List<ServerInfo> {
        val ans = mutableListOf<ServerInfo>()
        val selector = ActorSelectorManager(Dispatchers.IO)
        runBlocking {
            NetworkConfig.PORT_RANGE.forEach { port ->
                launch {
                    withTimeoutOrNull(timeout) {
                        try {
                            val socket =
                                aSocket(selector).tcp().connect(
                                    InetSocketAddress(ip, port),
                                ) {
                                    socketTimeout = timeout
                                }

                            val input = socket.openReadChannel()
                            val response = input.readUTF8Line()

                            if (response?.startsWith("SERVER_ID:") == true) {
                                ans.add(ServerInfo(serverName = ip, port = port))
                            }
                            socket.close()
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        }
        return ans
    }

    fun startCommunicate(curSocket: Socket) {
        val output = curSocket.openWriteChannel(autoFlush = true)
        val input = curSocket.openReadChannel()

        suspend fun checkConnection() =
            try {
                output.writeStringUtf8("\u0001")
                true
            } catch (e: Exception) {
                false
            }

        var currentGameState = IGame.GameState.ONGOING
        runBlocking {
            val checkingDelayJob =
                launch {
                    while (true) {
                        delay(5000)
                        if (!checkConnection()) {
                            println("Connection lost!")
                            curSocket.close()
                            break
                        }
                    }
                }

            while (currentGameState == IGame.GameState.ONGOING) {
                val clientJSon =
                    try {
                        input.readUTF8Line() ?: throw SocketException("Client disconnected")
                    } catch (e: Exception) {
                        println("Connection error: ${e.message}")
                        break
                    }
                if (clientJSon.startsWith("Game Over:")) {
                    break
                }
                try {
                    println("Earned move from other player")
                    val clientMove = currentGame.decerializeJsonFromStringToInfoSending(clientJSon)
                    currentGameState = currentGame.makeMove(clientMove)
                } catch (e: Exception) {
                    println("Json parsing error ${e.message}")
                }

                if (currentGameState != IGame.GameState.ONGOING) {
                    output.writeStringUtf8("Game Over: ${currentGameState.name}")
                    break
                }

                val clentMove = currentGame.returnClassWithCorrectInput("client")
                output.writeStringUtf8(Json.encodeToString(clentMove) + "\n")
                currentGameState = currentGame.makeMove(clentMove)
            }
            checkingDelayJob.cancel()
        }

        when (currentGameState) {
            IGame.GameState.DRAW -> println("Draw")
            IGame.GameState.SERVER_WINS -> println("Server Wins")
            IGame.GameState.CLIENT_WINS -> println("Client Wins")
            else -> println("Incorrect state or other player disconnected")
        }
    }

    suspend fun startClient(port: Int): Socket? {
        val selector = ActorSelectorManager(Dispatchers.IO)
        return try {
            aSocket(selector)
                .tcp()
                .connect(
                    InetSocketAddress(ip, port),
                ) {
                    socketTimeout = 10000
                }.also {
                    println("Успешное подключение к $ip:$port")
                }
        } catch (e: Exception) {
            println("Ошибка подключения: ${e.message}")
            selector.close()
            null
        } finally {
            selector.close()
        }
    }
}

fun main() {
    print("Введите порт для подключения")
    val port = readln().toInt()
    MainClient(TicTacToeGame(), port)
}
