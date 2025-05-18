@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-wildcard-imports")

package org.example
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.*
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.println

class MainServer<T : IGame.InfoForSending>(
    private val currentGame: IGame<T>,
    private val port: Int,
) {
    private val ip = getLocalIpAddress()

    fun getLocalIpAddress(): String? {
        try {
            val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in networkInterfaces) {
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    suspend fun startServer() {
        val customScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val job =
            customScope.launch {
                startServer(port).also { socket ->
                    startCommunicate(socket)
                }
            }

        job.join()
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
        if (ip == null) {
            throw Exception("IP finding problem")
        }

        val selector = ActorSelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selector).tcp().bind(InetSocketAddress(ip, port))

        return suspendCancellableCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO + SupervisorJob())
                .launch {
                    try {
                        while (true) {
                            val clientSocket = serverSocket.accept()
                            withTimeoutOrNull(5000) {
                                try {
                                    val input = clientSocket.openReadChannel()
                                    val output = clientSocket.openWriteChannel(autoFlush = true)

                                    val message: String? = input.readUTF8Line() ?: throw Exception("input failure")
                                    if (message == "connection") {
                                        continuation.resume(clientSocket)
                                        return@withTimeoutOrNull
                                    } else {
                                        output.writeStringUtf8("ok\n")
                                        output.writeStringUtf8(
                                            Json.encodeToString(
                                                ServerInfo(
                                                    serverName = ip,
                                                    port = port,
                                                ),
                                            ) + "\n",
                                        )
                                        output.close()
                                        input.cancel()
                                        clientSocket.close()
                                    }
                                } catch (e: Exception) {
                                    clientSocket.close()
                                }
                            } ?: run {
                                clientSocket.close()
                            }
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }.invokeOnCompletion {
                    serverSocket.close()
                }
        }
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
        runBlocking {
            MainServer(TicTacToeGame(), port).startServer()
        }
    } catch (e: Exception) {
        println("Exception handled: ${e.message}")
    }
}
