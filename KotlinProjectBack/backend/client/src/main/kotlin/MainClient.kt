package org.example
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.example.TicTacToeGame
import org.example.game

class MainClient<T: game.InfoForSending>(private val currentGame: game<T>, private val ip:String){
    init{
        runBlocking {
            startClient()
        }
    }

    suspend fun sendGameState(socket: Socket, output: ByteWriteChannel) {
        val classInfo = currentGame.returnClassWithCorrectInput("client")
        val json = Json.encodeToString(classInfo)
        output.writeStringUtf8("$json\n")
    }

    suspend fun startClient(){
        val selector = ActorSelectorManager(Dispatchers.IO)
        val socket = aSocket(selector).tcp().connect(InetSocketAddress(ip, 12345))

        val input = socket.openReadChannel()
        val output = socket.openWriteChannel(autoFlush = true)
        sendGameState(socket, output)
    }
}

fun main(){
    MainClient(TicTacToeGame(), "127.0.0.1")
}