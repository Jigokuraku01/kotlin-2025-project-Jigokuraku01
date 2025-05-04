package org.example
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class StartServer<T: game.InfoForSending>(private val currentGame: game<T>){
    init{
        runBlocking {
            startServer()
        }
    }

    suspend fun listenForIncomingMessages(socket: Socket, input: ByteReadChannel){
        while(true){
            val jsonString = input.readUTF8Line() ?: break
            try{
                val move = Json.decodeFromString(currentGame.returnInfoSendingClass(), jsonString)
                println(move)
            }
            catch (e: Exception){
                println("Json parsing error ${e.message}")
            }
        }
    }

    suspend fun startServer(){
        val selector = ActorSelectorManager(Dispatchers.IO)
        val serverSocket = aSocket(selector).tcp().bind(InetSocketAddress("0.0.0.0", 12345))
        val clientSocket = serverSocket.accept()
        val input = clientSocket.openReadChannel()
        val output = clientSocket.openWriteChannel(autoFlush = true)
        output.writeStringUtf8("Server is Ready for work. First Move to server\n")

            runBlocking {
                launch {
                    listenForIncomingMessages(clientSocket, input)
                }
            }

    }
}
fun main(){
    StartServer(TicTacToeGame())
}
