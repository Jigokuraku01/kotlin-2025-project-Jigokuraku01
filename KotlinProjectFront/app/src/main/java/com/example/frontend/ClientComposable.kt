@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.example.frontend

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.example.IGame
import org.example.MainClient

class ClientComposable<T : IGame.InfoForSending>(
    private val currentGame: IGame<T>,
    private val port: Int,
    private val activity: ComponentActivity,
    private val onStatusUpdate: (String) -> Unit = {},
    private val setGameResult: (IGame.GameState) -> Unit = {},
) : MainClient<T>(currentGame, port, onStatusUpdate, setGameResult) {
    private var availableServers by mutableStateOf<List<Pair<String, String>>>(emptyList())

    @Suppress("ktlint:standard:function-naming")
    @Composable
    private fun IpSelectionDialog(
        ipList: List<String>,
        onIpSelected: (String) -> Unit,
        onDismiss: () -> Unit,
    ) {
        var selectedIp by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Выберите сервер") },
            text = {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(ipList) { ip ->
                        ListItem(
                            headlineContent = { Text(ip) },
                            leadingContent = {
                                RadioButton(
                                    selected = selectedIp == ip,
                                    onClick = { selectedIp = ip },
                                )
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedIp = ip },
                        )
                        Divider()
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedIp?.let(onIpSelected) },
                    enabled = selectedIp != null,
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            },
        )
    }

    override suspend fun selectIpFromList(list: List<Pair<String, String>>): String? =
        coroutineScope {
            availableServers = list
            if (list.isEmpty()) {
                onStatusUpdate("🔵 No Servers found 🔵")
                return@coroutineScope null
            }

            val selectionDeferred = CompletableDeferred<String?>()

            activity.setContent {
                IpSelectionDialog(
                    ipList = availableServers.map { it -> "${it.first}(${it.second})" }.distinct(),
                    onIpSelected = { ip ->
                        activity.runOnUiThread {
                            activity.setContent {
                                Box(modifier = Modifier.fillMaxSize()) {}
                            }
                        }
                        selectionDeferred.complete(list.first { it.first == ip }.second)
                    },
                    onDismiss = {
                        activity.runOnUiThread {
                            activity.setContent {
                                Box(modifier = Modifier.fillMaxSize()) {}
                            }
                        }
                        selectionDeferred.complete(null)
                    },
                )
            }

            try {
                withTimeout(30_000) {
                    selectionDeferred.await()
                }
            } catch (e: TimeoutCancellationException) {
                onStatusUpdate("🟡 Время выбора сервера истекло")
                null
            }
        }
}
