package org.example

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.gradle.internal.impldep.org.junit.Before
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.ServerSocket
import kotlin.test.Test

class MainServerTest {
    private lateinit var mockGame: TicTacToeGame
    private lateinit var mockServerSocket: ServerSocket
    private lateinit var mockSocket: java.net.Socket
    private lateinit var mockInput: BufferedReader
    private lateinit var mockOutput: PrintWriter
    private lateinit var server: MainServer<TicTacToeGame.GameMove>
    private var statusUpdates = mutableListOf<String>()

    @Before
    fun setUp() {
        mockGame = mockk()
        mockServerSocket = mockk()
        mockSocket = mockk()
        mockInput = mockk()
        mockOutput = mockk()

        every { mockGame.decerializeJsonFromStringToInfoSending(any()) } returns TicTacToeGame.GameMove("ходить", "X", 0, 0)
        every { mockGame.makeMove(any()) } returns IGame.GameState.ONGOING
        every { mockGame.getPlayerId(any()) } returns "X"
        every { mockGame.printField() } just Runs

        server =
            MainServer(
                mockGame,
                8080,
                { status -> statusUpdates.add(status) },
            ).apply {
                input = mockInput
                output = mockOutput
            }
    }

    @Test
    fun `test startServer successfully starts`() =
        runBlocking {
            mockkStatic(ServerSocket::class)
            every { anyConstructed<ServerSocket>().accept() } returns mockSocket
            every { anyConstructed<ServerSocket>().close() } just Runs
            every { anyConstructed<java.net.Socket>().getOutputStream() } returns mockk()
            every { anyConstructed<java.net.Socket>().getInputStream() } returns mockk()
            every { anyConstructed<BufferedReader>().readLine() } returns "connection" andThen
                "{\"action\":\"ходить\",\"playerId\":\"O\",\"x\":1,\"y\":1}"

            server.startServer()

            assertTrue(statusUpdates.any { it.contains("Сервер начал ожидать запросы") })
        }

    @Test
    fun `test startCommunicate handles game flow`() =
        runBlocking {
            every { mockInput.readLine() } returns "{\"action\":\"ходить\",\"playerId\":\"O\",\"x\":1,\"y\":1}" andThen
                "Game Over: SERVER_WINS"
            every { mockGame.makeMove(any()) } returns IGame.GameState.SERVER_WINS

            server.startCommunicate()

            verify { mockOutput.println(any<String>()) }
            assertTrue(statusUpdates.isNotEmpty())
        }
}
