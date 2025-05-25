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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.example.MainServer

class MainActivity : ComponentActivity() {
    val customScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val job = SupervisorJob()

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

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun serverClientApp() {
        var mode by remember { mutableStateOf("client") }
        var port by remember { mutableStateOf("8080") }
        var game by remember { mutableStateOf("TicTacToe") }
        var status by remember { mutableStateOf("Не подключено") }
        var isConnected by remember { mutableStateOf(false) }
        var ipInputVisible by remember { mutableStateOf(true) }
        var manualIp by remember { mutableStateOf("") }
        var serverName by remember { mutableStateOf("") }
        val currentGame = remember { TicTacToeComposable(this@MainActivity) { return@TicTacToeComposable status } }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Выберите режим:", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioButton(
                    selected = mode == "client",
                    onClick = {
                        mode = "client"
                        ipInputVisible = true
                    },
                )
                Text("Клиент")

                Spacer(modifier = Modifier.width(16.dp))

                RadioButton(
                    selected = mode == "server",
                    onClick = {
                        mode = "server"
                        ipInputVisible = false
                    },
                )
                Text("Сервер")
            }

            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Порт") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            if (ipInputVisible) {
                OutlinedTextField(
                    value = manualIp,
                    onValueChange = { manualIp = it },
                    label = { Text("IP сервера (оставьте пустым для автопоиска серверов)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text("Имя сервера(если хотите имя сервера, совпадающим с IP, оставьте поле пустым)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = game,
                onValueChange = { game = it },
                label = { Text("Название игры: ") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (mode == "server") {
                        lifecycleScope.launch(Dispatchers.IO) {
                            customScope
                                .launch {
                                    val server =
                                        MainServer(
                                            currentGame,
                                            port.toInt(),
                                            onStatusUpdate = { newStatus ->
                                                status = newStatus
                                                println("--------SERVER--------\n$newStatus")
                                            },
                                            setGameResult = { newGameResult ->
                                                currentGame.gameResult = newGameResult
                                            },
                                        )
                                    if (serverName != "") {
                                        server.setNewServerName(serverName)
                                    }
                                    server.startServer()
                                    isConnected = true
                                }.join()
                        }
                    } else {
                        lifecycleScope.launch(Dispatchers.IO) {
                            customScope
                                .launch {
                                    val client =
                                        ClientComposable(
                                            currentGame,
                                            port.toInt(),
                                            this@MainActivity,
                                            onStatusUpdate = { newStatus ->
                                                status = newStatus
                                                println("--------CLIENT--------\n$newStatus")
                                            },
                                            setGameResult = { newGameResult ->
                                                currentGame.gameResult = newGameResult
                                            },
                                        )

                                    val selectedIp = if (manualIp.isNotBlank()) manualIp else client.selectGoodServer()
                                    if (selectedIp != null) {
                                        client.startClient(selectedIp)
                                        isConnected = true
                                    } else {
                                        status = "🔴 Сервер не выбран или не найден"
                                    }
                                }.join()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (mode == "server") "Запустить сервер" else "Подключиться")
            }

            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
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
