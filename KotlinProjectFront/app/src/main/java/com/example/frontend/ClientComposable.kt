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
import androidx.compose.ui.unit.dp
import org.example.IGame
import org.example.MainClient

class ClientComposable<T : IGame.InfoForSending>(
    private val currentGame: IGame<T>,
    private val port: Int,
    private val activity: ComponentActivity,
    private val onStatusUpdate: (String) -> Unit = {},
) : MainClient<T>(currentGame, port, onStatusUpdate) {
    private var selectedIp by mutableStateOf<String?>(null)
    private var showDialog by mutableStateOf(false)

    override fun selectIpFromList(list: List<String>): String? {
        println(list)
        if (list.size == 0) {
            onStatusUpdate("🔵 No Servers found 🔵")
            return null
        }
        activity.setContent {
            IpSelectionDialog(
                ipList = list,
                onIpSelected = { ip ->
                    selectedIp = ip
                    showDialog = false
                },
                onDismiss = { showDialog = false },
            )
        }
        return selectedIp
    }

    @Suppress("ktlint:standard:function-naming")
    @Composable
    private fun IpSelectionDialog(
        ipList: List<String>,
        onIpSelected: (String) -> Unit,
        onDismiss: () -> Unit,
    ) {
        if (showDialog) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Выберите сервер") },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(ipList) { ip ->
                            ListItem(
                                headlineContent = { Text(ip) },
                                leadingContent = {
                                    RadioButton(
                                        selected = selectedIp == ip,
                                        onClick = { onIpSelected(ip) },
                                    )
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onIpSelected(ip) },
                            )
                            Divider()
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedIp?.let { onIpSelected(it) }
                        },
                        enabled = selectedIp != null,
                    ) {
                        Text("Подтвердить")
                    }
                },
            )
        } else {
            showDialog = true
        }
    }
}
