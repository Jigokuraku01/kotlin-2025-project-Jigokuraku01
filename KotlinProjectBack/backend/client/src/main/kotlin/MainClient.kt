@file:Suppress("kt lint:standard:no-wildcard-imports", "ktlint:standard:no-wildcard-imports")

package org.example
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.SocketException

class MainClient<T : IGame.InfoForSending>(
    private val currentGame: IGame<T>,
    private val ip: String,
) {
    init {
        var curSocket: Socket
        runBlocking {
            curSocket = startClient()
        }
        startCommunicate(curSocket)
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

    suspend fun startClient(): Socket {
        val selector = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selector).tcp().connect(InetSocketAddress(ip, 12345))
        return socket
    }
}

fun main() {
    MainClient(TicTacToeGame(), "127.0.0.1")
}
