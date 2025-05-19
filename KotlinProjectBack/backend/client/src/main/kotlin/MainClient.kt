@file:Suppress("kt lint:standard:no-wildcard-imports", "ktlint:standard:no-wildcard-imports")

package org.example
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.*
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.util.pipeline.InvalidPhaseException
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
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
    var input: ByteReadChannel? = null
    var output: ByteWriteChannel? = null
    val customScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun startClient() {
        val customScope = CoroutineScope(Dispatchers.IO)
        val job =
            customScope.launch {
                val ip = selectGoodServer()
                if (ip != null) {
                    startClient(port, ip).also { socket ->
                        if (socket != null) {
                            startCommunicate(socket)
                        }
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

    suspend fun selectGoodServer(): String? {
        val listOfPossibeIP = mutableListOf<String>()
        val x = scanNetwork()
        val job =
            customScope.launch {
                x
                    .map { posIP ->
                        launch {
                            try {
                                val selector = ActorSelectorManager(Dispatchers.IO)
                                val socket =
                                    aSocket(selector).tcp().connect(InetSocketAddress(posIP, port)) {
                                        socketTimeout = 5000
                                    }
                                val tmpOutput = socket.openWriteChannel(autoFlush = true)
                                tmpOutput.writeStringUtf8("took-took\n")
                                val tmpInput = socket.openReadChannel()

                                val answer = tmpInput.readUTF8Line()
                                if (answer != "ok") {
                                    throw Exception("some problem")
                                }
                                val serverInfoSerializable = tmpInput.readUTF8Line() ?: throw Exception("input failure with ip $posIP")
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
        val ansIP = readLine()
        if (!listOfPossibeIP.contains(ansIP)) {
            throw InvalidPhaseException("invalid ip")
        }
        return ansIP
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

    private suspend fun startCommunicate(curSocket: Socket) {
        suspend fun checkConnection() =
            try {
                output?.writeStringUtf8("\n")
                true
            } catch (e: Exception) {
                false
            }

        var currentGameState = IGame.GameState.ONGOING
        customScope
            .launch {
                while (currentGameState == IGame.GameState.ONGOING) {
                    val clientJSon =
                        try {
                            input?.readUTF8Line() ?: throw SocketException("Server connection error")
                        } catch (e: Exception) {
                            println("Connection error: ${e.message} $input")
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
                        output?.writeStringUtf8("Game Over: ${currentGameState.name}\n")
                        break
                    }

                    val clentMove = currentGame.returnClassWithCorrectInput("client")
                    output?.writeStringUtf8(Json.encodeToString(clentMove) + "\n")
                    currentGameState = currentGame.makeMove(clentMove)
                }
            }.join()

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
                ).also {
                    println("Успешное подключение к $ip:$port")
                    output = it.openWriteChannel(autoFlush = true)
                    output?.writeStringUtf8("connection\n")
                    input = it.openReadChannel()
                }
        } catch (e: Exception) {
            println("Ошибка подключения: ${e.message}")
            selector.close()
            null
        }
    }
}

fun main() {
    try {
        print("Введите порт для подключения: ")
        val port = readln().toInt()
        runBlocking {
            MainClient(TicTacToeGame(), port).startClient()
        }
    } catch (e: Exception) {
        println("Exception handled: ${e.message}")
    }
}
