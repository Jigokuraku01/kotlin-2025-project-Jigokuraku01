@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-wildcard-imports")

package org.example
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.Collections
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MainServer<T : IGame.InfoForSending>(
    private val currentGame: IGame<T>,
    private val port: Int,
    private val onStatusUpdate: (String) -> Unit = {},
    private val setGameResult: (IGame.GameState) -> Unit = {},
) {
    var input: BufferedReader? = null
    var output: PrintWriter? = null
    private val ip = getLocalIpAddress() ?: "0.0.0.0"
    val customScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        if (!isPortAvailable(port)) {
            onStatusUpdate("🔴 Port $port is not available")
            return
        }
        val job =
            customScope.launch {
                startServer(port)
                    .also { socket ->
                        startCommunicate()
                    }
            }
        job.join()
    }

    suspend fun startCommunicate() {
        currentGame.printField()
        var currentGameState = IGame.GameState.ONGOING
        customScope
            .launch {
                while (currentGameState == IGame.GameState.ONGOING) {
                    val serverMove = currentGame.returnClassWithCorrectInput("server", onStatusUpdate)
                    output?.println(Json.encodeToString(serverMove))
                    currentGameState = currentGame.makeMove(serverMove)
                    if (currentGameState != IGame.GameState.ONGOING) {
                        output?.println("Game Over: ${currentGameState.name}")
                        break
                    }

                    val clientJSon = input?.readLine() ?: break
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
            IGame.GameState.DRAW -> setGameResult(IGame.GameState.DRAW)
            IGame.GameState.SERVER_WINS -> setGameResult(IGame.GameState.SERVER_WINS)
            IGame.GameState.CLIENT_WINS -> setGameResult(IGame.GameState.CLIENT_WINS)
            else -> onStatusUpdate("Incorrect state or other player disconnected")
        }
    }

    suspend fun startServer(port: Int) {
        onStatusUpdate("🔵 Сервер начал ожидать запросы по ${getLocalIpAddress() ?: ip}:$port")
        var isServerStarted = false
        val serverSocket = ServerSocket(port)
        return suspendCancellableCoroutine { continuation ->
            CoroutineScope(Dispatchers.IO + SupervisorJob())
                .launch {
                    try {
                        while (!isServerStarted) {
                            val clientSocket = serverSocket.accept()
                            withTimeoutOrNull(5000) {
                                try {
                                    val tmpInput = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
                                    val tmpOutput = PrintWriter(clientSocket.getOutputStream(), true)

                                    val message = tmpInput.readLine() ?: throw Exception("input failure")
                                    if (message == "connection") {
                                        input = tmpInput
                                        output = tmpOutput
                                        isServerStarted = true
                                        onStatusUpdate("🟢 Сервер запущен на ${getLocalIpAddress() ?: ip}:$port")
                                        continuation.resume(Unit)
                                        return@withTimeoutOrNull
                                    } else {
                                        tmpOutput.println("ok")
                                        val actualIp = getLocalIpAddress() ?: ip
                                        tmpOutput.println(
                                            Json.encodeToString(
                                                ServerInfo(
                                                    serverName = actualIp,
                                                    port = port,
                                                ),
                                            ),
                                        )
                                        if (!isServerStarted) {
                                            tmpOutput.flush()
                                            tmpInput.close()
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
            MainServer(TicTacToeGame(), port, { a -> println(a) }).startServer()
        }
    } catch (e: Exception) {
        println("Exception handled: ${e.message}")
    }
}
