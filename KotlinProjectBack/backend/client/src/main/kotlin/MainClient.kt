@file:Suppress("kt lint:standard:no-wildcard-imports", "ktlint:standard:no-wildcard-imports")

package org.example
import io.ktor.util.pipeline.InvalidPhaseException
import io.ktor.utils.io.close
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketException

open class MainClient<T : IGame.InfoForSending>(
    private val currentGame: IGame<T>,
    private val port: Int,
    private val onStatusUpdate: (String) -> Unit = {},
    private val setGameResult: (IGame.GameState) -> Unit = {},
) {
    var input: BufferedReader? = null
    var output: PrintWriter? = null
    val customScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun startClient(ip: String = "") {
        val job =
            customScope.launch {
                startClient(port, ip).also { socket ->
                    if (socket != null) {
                        startCommunicate()
                    }
                }
            }
        job.join()
    }

    open suspend fun selectIpFromList(list: List<Pair<String, String>>): String? {
        list.forEach {
            println("Possible IP: ${it.second}")
        }
        print("Введите нужный IP: ")
        val ansIP = readLine()?.trim()
        if (list.none { it.second == ansIP }) {
            throw InvalidPhaseException("invalid ip")
        }
        return ansIP
    }

    suspend fun selectGoodServer(): String? {
        val listOfPossibeIP = mutableListOf<Pair<String, String>>()
        val x = scanNetwork()
        val job =
            customScope.launch {
                x
                    .map { posIP ->
                        launch {
                            try {
                                onStatusUpdate("🔵 Пытаюсь подключиться к $posIP:$port")
                                val socket = Socket()
                                socket.connect(InetSocketAddress(posIP, port), 300)
                                val tmpOutput = PrintWriter(socket.getOutputStream(), true)
                                tmpOutput.println("took-took")
                                val tmpInput = BufferedReader(InputStreamReader(socket.getInputStream()))

                                val answer = tmpInput.readLine()
                                if (answer != "ok") {
                                    onStatusUpdate("🔴 some network input problems 🔴")
                                    throw Exception("error occurred")
                                }
                                val serverInfoSerializable =
                                    tmpInput.readLine() ?: throw Exception("input failure with ip $posIP")
                                val serverInfo = Json.decodeFromString<ServerInfo>(serverInfoSerializable)
                                listOfPossibeIP.add(Pair(serverInfo.serverName, posIP))
                                socket.close()
                            } catch (e: Exception) {
                                println(
                                    "$posIP Exception handled ${e.message}\n" +
                                        "-------STACK TRACE-------",
                                )
                                e.printStackTrace()
                            }
                        }
                    }.joinAll()
                    .also { println("🔵 End Of Log of selecting network 🔵") }
            }
        job.join()
        println(listOfPossibeIP)
        return selectIpFromList(listOfPossibeIP.distinct())
    }

    private fun scanNetwork(): List<String> {
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

    suspend fun startCommunicate() {
        currentGame.printField()
        var currentGameState = IGame.GameState.ONGOING
        customScope
            .launch {
                while (currentGameState == IGame.GameState.ONGOING) {
                    val clientJSon =
                        try {
                            input?.readLine() ?: throw SocketException("Server connection error")
                        } catch (e: Exception) {
                            println("Connection error: ${e.message} $input")
                            onStatusUpdate("🔴 Connection error: ${e.message}")
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
                        output?.println("Game Over: ${currentGameState.name}")
                        break
                    }

                    val clentMove = currentGame.returnClassWithCorrectInput("client", onStatusUpdate)
                    output?.println(Json.encodeToString(clentMove))
                    currentGameState = currentGame.makeMove(clentMove)
                }
            }.join()

        when (currentGameState) {
            IGame.GameState.DRAW -> setGameResult(IGame.GameState.DRAW)
            IGame.GameState.SERVER_WINS -> setGameResult(IGame.GameState.SERVER_WINS)
            IGame.GameState.CLIENT_WINS -> setGameResult(IGame.GameState.CLIENT_WINS)
            else -> onStatusUpdate("Incorrect state or other player disconnected")
        }
    }

    private fun startClient(
        port: Int,
        ip: String,
    ): Socket? =
        try {
            onStatusUpdate("🔵 Пытаюсь подключиться к $ip:$port")
            Socket(ip, port).also {
                onStatusUpdate("\uD83D\uDFE2 Успешное подключение к $ip:$port")
                output = PrintWriter(it.getOutputStream(), true)
                output?.println("connection")
                input = BufferedReader(InputStreamReader(it.getInputStream()))
            }
        } catch (e: Exception) {
            onStatusUpdate("🔴 Connection error: ${e.message}")
            println(e.message)
            null
        }
}

fun main() {
    try {
        print("Введите порт для подключения: ")
        val port = readln().toInt()
        println("Введите IP для подключения: ")
        val ip = readln()
        runBlocking {
            MainClient(TicTacToeGame(), port, { a -> println(a) }).startClient(ip)
        }
    } catch (e: Exception) {
        println("Exception handled: ${e.message}")
    }
}
