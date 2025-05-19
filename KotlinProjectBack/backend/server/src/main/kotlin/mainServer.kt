@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-wildcard-imports")

package org.example
import io.ktor.network.selector.*
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.coroutines.withContext
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
    private val onStatusUpdate: (String) -> Unit = {},
) {
    var input: ByteReadChannel? = null
    var output: ByteWriteChannel? = null
    private val ip = getLocalIpAddress()
    val customScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    suspend fun isPortAvailable(port: Int): Boolean =
        withContext(Dispatchers.IO) {
            val selector = ActorSelectorManager(Dispatchers.IO)
            try {
                aSocket(selector).tcp().bind(InetSocketAddress("0.0.0.0", port)).use {
                    true
                }
            } catch (e: Exception) {
                false
            } finally {
                selector.close()
            }
        }

    suspend fun startServer() {
        if (!isPortAvailable(port)) {
            onStatusUpdate("🔴 Port $port is not available")
            return
        }
        val job =
            customScope.launch {
                startServer(port)
                    .also { socket ->
                        startCommunicate(socket)
                    }
            }
        job.join()
    }

    suspend fun startCommunicate(curSocket: Socket) {
        var currentGameState = IGame.GameState.ONGOING
        customScope
            .launch {
                while (currentGameState == IGame.GameState.ONGOING) {
                    val serverMove = currentGame.returnClassWithCorrectInput("server")
                    output?.writeStringUtf8(Json.encodeToString(serverMove) + "\n")
                    currentGameState = currentGame.makeMove(serverMove)
                    if (currentGameState != IGame.GameState.ONGOING) {
                        output?.writeStringUtf8("Game Over: ${currentGameState.name}\n")
                        break
                    }

                    val clientJSon = input?.readUTF8Line() ?: break
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
            }.join()

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
        onStatusUpdate("🔵 Сервер начал ожидать запросы по $ip:$port")
        val selector = ActorSelectorManager(Dispatchers.IO)
        var isServerStarted = false
        val serverSocket = aSocket(selector).tcp().bind(InetSocketAddress(ip, port))
        return suspendCancellableCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO + SupervisorJob())
                .launch {
                    try {
                        while (true) {
                            val clientSocket = serverSocket.accept()
                            withTimeoutOrNull(5000) {
                                try {
                                    val tmpInput = clientSocket.openReadChannel()
                                    val tmpOutput = clientSocket.openWriteChannel(autoFlush = true)

                                    val message = tmpInput.readUTF8Line() ?: throw Exception("input failure")
                                    if (message == "connection") {
                                        input = tmpInput
                                        output = tmpOutput
                                        continuation.resume(clientSocket)
                                        isServerStarted = true
                                        onStatusUpdate("🟢 Сервер запущен на $ip:$port")
                                        return@withTimeoutOrNull
                                    } else {
                                        tmpOutput.writeStringUtf8("ok\n")
                                        tmpOutput.writeStringUtf8(
                                            Json.encodeToString(
                                                ServerInfo(
                                                    serverName = ip,
                                                    port = port,
                                                ),
                                            ) + "\n",
                                        )
                                        if (!isServerStarted) {
                                            tmpOutput.flushAndClose()
                                            tmpInput.cancel()
                                            clientSocket.close()
                                        }
                                    }
                                } catch (e: Exception) {
                                    if (!isServerStarted) {
                                        clientSocket.close()
                                    }
                                    throw e
                                }
                            } ?: run {
                                if (!isServerStarted) {
                                    clientSocket.close()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }.invokeOnCompletion {
                    if (!isServerStarted) {
                        serverSocket.close()
                    }
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
