package com.example.frontend

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.IGame
import org.example.IGame.GameState
import org.example.TicTacToeGame

class TicTacToeComposable(
    private val activity: ComponentActivity,
    private val DataFromGame: () -> String,
) : TicTacToeGame() {
    private var inputResult: GameMove? by mutableStateOf(null)
    private var currentPlayerId by mutableStateOf("")
    private var alreadyRendered = false
    private var fieldState by mutableStateOf(Array(3) { arrayOfNulls<String>(3) })
    var gameResult: GameState? by mutableStateOf(null)
    private var gameResultString: String? by mutableStateOf(null)
    private var isInputEnabled by mutableStateOf(false)
    private var onStatusUpdate_: (String) -> (Unit) = { a -> a }
    private val tgkURL = "https://t.me/+FCLWNaTEc7o3ZDUy"

    private fun updateFieldState() {
        val newField =
            Array(3) { i ->
                Array(3) { j ->
                    getPlayerByPos(i, j)
                }
            }
        fieldState = newField
    }

    override fun makeMove(move: IGame.InfoForSending): GameState {
        val state = super.makeMove(move)
        gameResult = state
        updateFieldState()
        when (state) {
            GameState.CLIENT_WINS -> gameResultString = "Победа клиента 🎉"
            GameState.SERVER_WINS -> gameResultString = "Победа сервера 🏆"
            GameState.DRAW -> gameResultString = "Ничья 🤝"
            else -> {}
        }

        return state
    }

    override fun printField() {
        updateFieldState()
        if (!alreadyRendered) {
            alreadyRendered = true
            activity.runOnUiThread {
                activity.setContent {
                    GameScreen()
                }
            }
        }
    }

    @Suppress("ktlint:standard:function-naming")
    @Composable
    fun GameScreen() {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (gameResultString != null) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Игра завершена") },
                        text = {
                            Column {
                                Text(
                                    "${gameResultString!!}\nИгра закончена. Если хотите следить за новостями разработчика, переходите в его тгк",
                                )
                                Text(
                                    text = "Перейти",
                                    style =
                                        MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            textDecoration = TextDecoration.Underline,
                                        ),
                                    modifier =
                                        Modifier
                                            .clickable {
                                                Toast
                                                    .makeText(
                                                        activity,
                                                        "Открываем ссылку",
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                val intent =
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse(tgkURL),
                                                    )
                                                activity.startActivity(intent)
                                            }.padding(4.dp),
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = { gameResultString = null }) {
                                Text("Ок")
                            }
                        },
                    )
                }

                printFieldComposable()
                Text(
                    text = if (isInputEnabled) "Ваш ход ($currentPlayerId)" else "Ожидаем хода",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (isInputEnabled) {
                    Button(
                        onClick = {
                            inputResult =
                                GameMove(
                                    action = "сдаться",
                                    playerId = getPlayerId(currentPlayerId),
                                    x = -1,
                                    y = -1,
                                )
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Сдаться")
                    }
                }
                Text(
                    text = DataFromGame(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            IconButton(
                onClick = { activity.finish() },
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть приложение",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }

    override suspend fun returnClassWithCorrectInput(
        playerId: String,
        onStatusUpdate: (String) -> (Unit),
    ): GameMove {
        onStatusUpdate_ = onStatusUpdate
        activity.runOnUiThread {
            currentPlayerId = playerId
            isInputEnabled = true
            inputResult = null
        }
        while (inputResult == null) {
            delay(100)
        }
        return inputResult!!.also {
            inputResult = null
            isInputEnabled = false
        }
    }

    @Composable
    fun printFieldComposable() {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.size(300.dp),
        ) {
            for (i in 0..2) {
                Row {
                    for (j in 0..2) {
                        Box(
                            modifier =
                                Modifier
                                    .size(80.dp)
                                    .border(1.dp, Color.Black)
                                    .clickable(
                                        enabled = isInputEnabled && fieldState[i][j] == null,
                                        onClick = {
                                            if (isInputEnabled && gameResultString == null) {
                                                inputResult =
                                                    GameMove(
                                                        action = "ходить",
                                                        playerId = getPlayerId(currentPlayerId),
                                                        x = i,
                                                        y = j,
                                                    )
                                            }
                                        },
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = fieldState[i][j] ?: "",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color =
                                    when (fieldState[i][j]) {
                                        "X" -> Color.Red
                                        "O" -> Color.Blue
                                        else -> Color.Transparent
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
