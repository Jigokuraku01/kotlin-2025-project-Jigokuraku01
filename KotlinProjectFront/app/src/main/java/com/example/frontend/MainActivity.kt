@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.example.frontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.example.MainClient
import org.example.StartServer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            appTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    serverClientApp()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun serverClientApp() {
        var mode by remember { mutableStateOf("client") }
        var port by remember { mutableStateOf("8080") }
        var game by remember { mutableStateOf("TicTacToe") }
        var status by remember { mutableStateOf("Не подключено") }
        var isConnected by remember { mutableStateOf(false) }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Выберите режим:", style = MaterialTheme.typography.titleMedium)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(
                    selected = mode == "client",
                    onClick = { mode = "client" },
                )
                Text("Клиент")

                Spacer(modifier = Modifier.width(16.dp))

                RadioButton(
                    selected = mode == "server",
                    onClick = { mode = "server" },
                )
                Text("Сервер")
            }

            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Порт") },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = game,
                onValueChange = { game = it },
                label = { Text("Название игры: ") },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                    ),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    isConnected = true
                    status =
                        if (mode == "server") {
                            StartServer(TicTacToeComposable(this@MainActivity), port.toInt())
                            "Сервер запущен на порту $port"
                        } else {
                            MainClient(TicTacToeComposable(this@MainActivity), port.toInt())
                            "Подключено к серверу на порту $port"
                        }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (mode == "server") "Запустить сервер" else "Подключиться")
            }

            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (isConnected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
            )
        }
    }
}

@Composable
fun appTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content,
    )
}
