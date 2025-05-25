package com.example.frontend

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.IGame
import org.example.IGame.GameState
import org.example.TicTacToeGame

class TicTacToeComposable(
    private val activity: ComponentActivity,
) : TicTacToeGame() {
    private var inputResult: GameMove? by mutableStateOf(null)
    private var showInputDialog by mutableStateOf(false)
    private var currentPlayerId by mutableStateOf("")
    private var alreadyRendered = false
    private var fieldState by mutableStateOf(Array(3) { arrayOfNulls<String>(3) })

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
        updateFieldState()
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
        Column {
            printFieldComposable()
            if (showInputDialog) {
                moveInputDialog(
                    playerId = currentPlayerId,
                    onMoveSubmitted = { move ->
                        inputResult = move
                        showInputDialog = false
                    },
                )
            }
        }
    }

    override suspend fun returnClassWithCorrectInput(playerId: String): GameMove {
        activity.runOnUiThread {
            currentPlayerId = playerId
            showInputDialog = true
            inputResult = null
        }
        while (inputResult == null) {
            delay(100)
        }
        return inputResult!!.also { inputResult = null }
    }

    @Composable
    private fun moveInputDialog(
        playerId: String,
        onMoveSubmitted: (GameMove) -> Unit,
    ) {
        var action by remember { mutableStateOf("ходить") }
        var x by remember { mutableStateOf("0") }
        var y by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { /* Нельзя закрыть без ввода */ },
            title = { Text("Ход игрока $playerId") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = action == "ходить",
                            onClick = { action = "ходить" },
                        )
                        Text("Ходить")

                        Spacer(modifier = Modifier.width(16.dp))

                        RadioButton(
                            selected = action == "сдаться",
                            onClick = { action = "сдаться" },
                        )
                        Text("Сдаться")
                    }

                    if (action == "ходить") {
                        Row {
                            OutlinedTextField(
                                value = x,
                                onValueChange = { x = it },
                                label = { Text("X (0-2)") },
                                modifier = Modifier.width(100.dp),
                                keyboardOptions =
                                    KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                    ),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = y,
                                onValueChange = { y = it },
                                label = { Text("Y (0-2)") },
                                modifier = Modifier.width(100.dp),
                                keyboardOptions =
                                    KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                    ),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onMoveSubmitted(
                            GameMove(
                                action = action,
                                playerId = getPlayerId(playerId),
                                x = if (action == "ходить") x.toInt() else -1,
                                y = if (action == "ходить") y.toInt() else -1,
                            ),
                        )
                    },
                ) {
                    Text("Подтвердить")
                }
            },
        )
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
                                    .border(1.dp, Color.Black),
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
