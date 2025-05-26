@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.example

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.BufferedReader
import java.io.PrintWriter
import java.net.Socket

class MainClientTest {
    private lateinit var mockGame: TicTacToeGame
    private lateinit var mockSocket: Socket
    private lateinit var mockInput: BufferedReader
    private lateinit var mockOutput: PrintWriter
    private lateinit var client: MainClient<TicTacToeGame.GameMove>
    private var statusUpdates = mutableListOf<String>()

    @Before
    fun setUp() {
        mockGame = mockk()
        mockSocket = mockk()
        mockInput = mockk()
        mockOutput = mockk()

        every { mockGame.decerializeJsonFromStringToInfoSending(any()) } returns TicTacToeGame.GameMove("ходить", "X", 0, 0)
        every { mockGame.makeMove(any()) } returns IGame.GameState.ONGOING
        every { mockGame.getPlayerId(any()) } returns "X"
        every { mockGame.printField() } just Runs

        client =
            MainClient(
                mockGame,
                8080,
                { status -> statusUpdates.add(status) },
            ).apply {
                input = mockInput
                output = mockOutput
            }
    }

    @Test
    fun `test startClient successfully connects`() =
        runBlocking {
            val testClient =
                MainClient(
                    mockGame,
                    8080,
                    { status -> statusUpdates.add(status) },
                )

            mockkStatic(Socket::class)
            every { anyConstructed<Socket>().getOutputStream() } returns mockk()
            every { anyConstructed<Socket>().getInputStream() } returns mockk()
            every { anyConstructed<BufferedReader>().readLine() } returns "connection"

            testClient.startClient("127.0.0.1")

            assertTrue(statusUpdates.any { it.contains("Успешное подключение") })
        }

    @Test
    fun `test startCommunicate handles game flow`() =
        runBlocking {
            every { mockInput.readLine() } returns "{\"action\":\"ходить\",\"playerId\":\"O\",\"x\":1,\"y\":1}" andThen
                "Game Over: SERVER_WINS"
            every { mockGame.makeMove(any()) } returns IGame.GameState.SERVER_WINS

            client.startCommunicate()

            verify { mockOutput.println(any<String>()) }
            assertTrue(statusUpdates.isNotEmpty())
        }
}
