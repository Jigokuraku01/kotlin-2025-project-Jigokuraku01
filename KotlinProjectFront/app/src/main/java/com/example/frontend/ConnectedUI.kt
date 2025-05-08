package com.example.frontend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConnectedUI(mode: String) {
    var command by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (mode == "server") "Статус сервера" else "Команды",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("Введите команду") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { /* Обработка команды */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Отправить")
        }
    }
}