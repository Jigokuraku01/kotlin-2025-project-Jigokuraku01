package org.example
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class StartServer<T: IGame.InfoForSending>(private val currentGame: IGame<T>, private val ip:String){
    init{
        var curSocket: Socket
        runBlocking {
            curSocket = startServer(ip)
        }
        startCommunicate(curSocket)
    }

    fun startCommunicate(curSocket: Socket){
        val output = curSocket.openWriteChannel(autoFlush = true)
        val input = curSocket.openReadChannel()
        var currentGameState = IGame.GameState.ONGOING
        runBlocking {
            while (currentGameState == IGame.GameState.ONGOING) {
                val serverMove = currentGame.returnClassWithCorrectInput("server")
                output.writeStringUtf8(Json.encodeToString(serverMove) + "\n")
                currentGameState = currentGame.makeMove(serverMove)
                if (currentGameState != IGame.GameState.ONGOING)
                {
                    output.writeStringUtf8("Game Over: ${currentGameState.name}")
                    break
                }

                val clientJSon = input.readUTF8Line() ?: break
                if(clientJSon.startsWith("Game Over:"))
                    break
                try {
                    println("Earned move from other player")
                    val clientMove = currentGame.dexerializeJsonFromStringToInfoSending(clientJSon)
                    currentGameState = currentGame.makeMove(clientMove)
                }catch (e: Exception) {
                    println("Json parsing error ${e.message}")
                }
            }
        }
        when(currentGameState){
            IGame.GameState.DRAW -> println("Draw")
            IGame.GameState.SERVER_WINS -> println("Server Wins")
            IGame.GameState.CLIENT_WINS -> println("Client Wins")
            else -> println("Incorrect state")
        }
    }
    suspend fun startServer(ip: String): Socket{
        val selector = ActorSelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selector).tcp().bind(InetSocketAddress(ip, 12345))
        val clientSocket = serverSocket.accept()
        return clientSocket
    }
}
fun main(){
    StartServer(TicTacToeGame(), "0.0.0.0")
}
