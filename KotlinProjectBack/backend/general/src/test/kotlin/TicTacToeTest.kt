package org.example

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TicTacToeGameTest {
    private val game = TicTacToeGame()

    @Test
    fun `test initial game state`() {
        assertNull(game.checkWinner())
        assertFalse(game.isBoardFull())
    }

    @Test
    fun `test getPlayerId`() {
        assertEquals("X", game.getPlayerId("server"))
        assertEquals("O", game.getPlayerId("client"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test getPlayerId with invalid input`() {
        game.getPlayerId("invalid")
    }

    @Test
    fun `test makeMove and checkWinner`() {
        var move = TicTacToeGame.GameMove("ходить", "X", 0, 0)
        assertEquals(IGame.GameState.ONGOING, game.makeMove(move))

        move = TicTacToeGame.GameMove("ходить", "O", 1, 1)
        assertEquals(IGame.GameState.ONGOING, game.makeMove(move))

        move = TicTacToeGame.GameMove("ходить", "X", 0, 1)
        game.makeMove(move)
        move = TicTacToeGame.GameMove("ходить", "X", 0, 2)
        assertEquals(IGame.GameState.SERVER_WINS, game.makeMove(move))
    }

    @Test
    fun `test surrender`() {
        val move = TicTacToeGame.GameMove("сдаться", "O", -1, -1)
        assertEquals(IGame.GameState.SERVER_WINS, game.makeMove(move))
    }

    @Test
    fun `test serialization and deserialization`() {
        val original = TicTacToeGame.GameMove("ходить", "X", 1, 2)
        val json = Json.encodeToString(original)
        val deserialized = game.decerializeJsonFromStringToInfoSending(json)

        assertEquals(original.action, deserialized.action)
        assertEquals(original.playerId, deserialized.playerId)
        assertEquals(original.x, deserialized.x)
        assertEquals(original.y, deserialized.y)
    }

    @Test
    fun `test isBoardFull`() {
        for (i in 0..2) {
            for (j in 0..2) {
                val move = TicTacToeGame.GameMove("ходить", if ((i + j) % 2 == 0) "X" else "O", i, j)
                game.makeMove(move)
            }
        }
        assertTrue(game.isBoardFull())
    }
}
