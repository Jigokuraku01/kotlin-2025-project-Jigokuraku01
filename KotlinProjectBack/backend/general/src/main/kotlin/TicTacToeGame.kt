package org.example
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

open class TicTacToeGame : IGame<TicTacToeGame.GameMove> {
    private val field = Array(3) { Array(3) { "" } }

    @Serializable
    class GameMove(
        var action: String,
        var playerId: String,
        var x: Int,
        var y: Int,
    ) : IGame.InfoForSending()

    data class SettingInfoImpl(
        val playerId: String,
        val x: Int,
        val y: Int,
    ) : IGame.InnerLogic.SettingInfo()

    override fun returnInfoSendingClass(): KSerializer<out IGame.InfoForSending> = GameMove.serializer()

    override fun getPlayerId(name: String): String {
        if (name == "server") {
            return "X"
        }
        if (name == "client") {
            return "O"
        }
        throw IllegalArgumentException("invalid player id")
    }

    override suspend fun returnClassWithCorrectInput(
        playerId: String,
        onStatusUpdate: (String) -> Unit,
    ): GameMove {
        while (true) {
            try {
                print("Вы играете за ${getPlayerId(playerId)}. Что бы вы хотели сделать?(ходить, сдаться): ")
                val action = readln()
                if (!(action == "ходить" || action == "сдаться")) {
                    throw IllegalArgumentException("invalid action: $action")
                }
                var x = -1
                var y = -1
                if (action == "ходить") {
                    print("Введите x: ")
                    x = readln().trim().toInt()
                    print("Введите y: ")
                    y = readln().trim().toInt()
                }
                if (!logic.checkIfPosIsGood(SettingInfoImpl(playerId = getPlayerId(playerId), x = x, y = y))) {
                    throw Exception("Invalid input x = $x, y = $y\n")
                }
                val ans = GameMove(action = action, playerId = getPlayerId(playerId), x = x, y = y)
                return ans
            } catch (e: Exception) {
                onStatusUpdate("Exception ${e.message} handled")
            }
        }
    }

    override fun decerializeJsonFromStringToInfoSending(input: String): GameMove {
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString<GameMove>(input)
    }

    fun getPlayerByPos(
        i: Int,
        j: Int,
    ): String? {
        if (field[i][j] != "X" && field[i][j] != "O") {
            return null
        }
        return field[i][j]
    }

    protected val logic =
        object : IGame.InnerLogic() {
            override fun checkIfPosIsGood(info: SettingInfo): Boolean {
                val actualInfo = info as SettingInfoImpl
                if (!(actualInfo.x >= 0 && actualInfo.x < field.size)) {
                    return false
                }
                if (!(actualInfo.y >= 0 && actualInfo.y < field[actualInfo.x].size)) {
                    return false
                }
                return field[actualInfo.x][actualInfo.y].isEmpty()
            }

            override fun setToPos(info: SettingInfo) {
                val actualInfo = info as SettingInfoImpl
                field[actualInfo.x][actualInfo.y] = actualInfo.playerId
            }
        }

    override fun printField() {
        field.forEach { row ->
            println(row.joinToString(" | ") { it.ifEmpty { " " } })
            println("---------")
        }
    }

    fun isBoardFull(): Boolean {
        for (i in field.indices) {
            for (j in field[i].indices) {
                if (field[i][j].isEmpty()) {
                    return false
                }
            }
        }
        return true
    }

    override fun checkWinner(): String? {
        for (i in field.indices) {
            val row = field[i]
            if (row.all { it == row[0] && it.isNotEmpty() }) {
                return row[0]
            }
        }

        for (i in field.indices) {
            val col = List(field.size) { j -> field[j][i] }
            if (col.all { it == col[0] && it.isNotEmpty() }) {
                return col[0]
            }
        }

        val mainDiagonal = List(field.size) { i -> field[i][i] }
        if (mainDiagonal.all { it == mainDiagonal[0] && it.isNotEmpty() }) {
            return mainDiagonal[0]
        }

        val antiDiagonal = List(field.size) { i -> field[i][field.size - 1 - i] }
        if (antiDiagonal.all { it == antiDiagonal[0] && it.isNotEmpty() }) {
            return antiDiagonal[0]
        }

        return null
    }

    override fun makeMove(info: IGame.InfoForSending): IGame.GameState {
        printField()
        val move = info as GameMove
        if (info.action == "сдаться") {
            return when (info.playerId) {
                "X" -> IGame.GameState.CLIENT_WINS
                "O" -> IGame.GameState.SERVER_WINS
                else -> throw RuntimeException("invalid playerID: ${info.playerId}")
            }
        }
        val settingInfo = SettingInfoImpl(playerId = move.playerId, x = move.x, y = move.y)
        if (!logic.checkIfPosIsGood(settingInfo)) {
            throw IllegalArgumentException("Illegal move: player ${move.playerId}, x = ${move.x}, y = ${move.y}")
        }
        logic.setToPos(settingInfo)
        printField()
        return when (checkWinner()) {
            "X" -> IGame.GameState.SERVER_WINS
            "O" -> IGame.GameState.CLIENT_WINS
            else -> if (isBoardFull()) IGame.GameState.DRAW else IGame.GameState.ONGOING
        }
    }
}
