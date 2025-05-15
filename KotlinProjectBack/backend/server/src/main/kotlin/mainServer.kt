@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-wildcard-imports")

package org.example
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.println

class StartServer<T : IGame.InfoForSending>(
    private val currentGame: IGame<T>,
    private val port: Int,
) {
    private val ip = "0.0.0.0"

    fun startServer() {
        val customScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        customScope.launch {
            startServer(port).also { socket ->
                startCommunicate(socket)
            }
        }
    }

    @Serializable
    data class ServerInfo(
        val serverName: String,
        val port: Int,
    )

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
                val serverMove = currentGame.returnClassWithCorrectInput("server")
                output.writeStringUtf8(Json.encodeToString(serverMove) + "\n")
                currentGameState = currentGame.makeMove(serverMove)
                if (currentGameState != IGame.GameState.ONGOING) {
                    output.writeStringUtf8("Game Over: ${currentGameState.name}")
                    break
                }

                val clientJSon = input.readUTF8Line() ?: break
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

    suspend fun startServer(port: Int): Socket {
        val selector = ActorSelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selector).tcp().bind(InetSocketAddress(ip, port))
        while (true) {
            val clientSocket = serverSocket.accept()

            val input = clientSocket.openReadChannel()
            val output = clientSocket.openWriteChannel()

            val message: String? = input.readUTF8Line() ?: break
            if (message == "connection") {
                input.cancel()
                output.close()
                println("Сервер успешно запущен")
                return clientSocket
            } else {
                output.writeStringUtf8("ok")
                output.writeStringUtf8(
                    Json.encodeToString<ServerInfo>(
                        ServerInfo(
                            serverName = ip,
                            port = port,
                        ),
                    ),
                )
                clientSocket.close()
            }
        }
        return TODO("Provide the return value")
    }
}

fun main() {
    println(
        "Введите порт на котором хотите запустить сервер. Возможные порты от ${NetworkConfig.PORT_RANGE.first} до ${NetworkConfig.PORT_RANGE.last}: ",
    )
    try {
        val port = readln().toInt()
        if (port !in NetworkConfig.PORT_RANGE) {
            throw IllegalArgumentException("incorrect port")
        }
        StartServer(TicTacToeGame(), port)
    } catch (e: Exception) {
        println("Exception handled: ${e.message}")
    }
}
