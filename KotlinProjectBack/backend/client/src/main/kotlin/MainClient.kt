@file:Suppress("kt lint:standard:no-wildcard-imports", "ktlint:standard:no-wildcard-imports")

package org.example
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.NetworkInterface
import java.net.SocketException

class MainClient<T : IGame.InfoForSending>(
    private val currentGame: IGame<T>,
    private val port: Int,
) {
    suspend fun startClient() {
        val customScope = CoroutineScope(Dispatchers.IO)
        val job =
            customScope.launch {
                val ip = selectGoodServer()

                startClient(port, ip).also { socket ->
                    if (socket != null) {
                        startCommunicate(socket)
                    }
                }
            }
        job.join()
    }

    @Serializable
    data class ServerInfo(
        val serverName: String,
        val port: Int,
    )

    suspend fun selectGoodServer(): String {
        val listOfPossibeIP = mutableListOf<String>()
        val x = scanNetwork()
        val customScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val job =
            customScope.launch {
                x
                    .map { posIP ->
                        launch {
                            try {
                                val selector = ActorSelectorManager(Dispatchers.IO)
                                val socket =
                                    aSocket(selector).tcp().connect(InetSocketAddress(posIP, port)) {
                                        socketTimeout = 10000
                                    }
                                val output = socket.openWriteChannel()
                                output.writeStringUtf8("took-took")
                                val input = socket.openReadChannel()

                                val answer = input.readUTF8Line()
                                if (answer != "ok") {
                                    throw Exception("some problem")
                                }
                                val serverInfoSerializable = input.readUTF8Line() ?: throw Exception()
                                val serverInfo = Json.decodeFromString<ServerInfo>(serverInfoSerializable)
                                listOfPossibeIP.add(serverInfo.serverName)
                                socket.close()
                            } catch (e: Exception) {
                            }
                        }
                    }.joinAll()
            }
        job.join()
        listOfPossibeIP.forEach {
            println("Possible IP: $it")
        }
        print("Введите нужный IP: ")
        return readln()
    }

    fun scanNetwork(): List<String> {
        val addresses = mutableListOf<String>()

        NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { networkInterface ->
            networkInterface.inetAddresses?.toList()?.forEach { inetAddress ->
                if (!inetAddress.isLoopbackAddress && inetAddress.hostAddress?.contains(":") == false) {
                    addresses.add(inetAddress.hostAddress)
                }
            }
        }

        return addresses
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

    private suspend fun startClient(
        port: Int,
        ip: String,
    ): Socket? {
        val selector = ActorSelectorManager(Dispatchers.IO)
        return try {
            aSocket(selector)
                .tcp()
                .connect(
                    InetSocketAddress(ip, port),
                ) {
                    socketTimeout = 3000
                }.also {
                    println("Успешное подключение к $ip:$port")
                    val output = it.openWriteChannel()
                    output.writeStringUtf8("connection")
                    output.close()
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
    print("Введите порт для подключения: ")
    val port = readln().toInt()
    runBlocking {
        MainClient(TicTacToeGame(), port).startClient()
    }
}
