package com.viktorholk.apipushnotifications.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ConfigurationScreen(
    selfHostedUrl: String,
    selfHostedToken: String,
    isLoading: Boolean,
    onSelfHostedUrlChange: (String) -> Unit,
    onConnect: () -> Unit,
    onCopyToken: (String) -> Unit,
    onResetToken: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = selfHostedUrl,
            onValueChange = onSelfHostedUrlChange,
            label = { Text("Server URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            enabled = !isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (selfHostedToken.isBlank()) "A new token will be registered automatically." else "Device registered.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth(),
            enabled = selfHostedUrl.isNotBlank() && !isLoading
        ) {
            Text(if (selfHostedToken.isBlank()) "Register & Connect" else "Connect")
        }

        if (selfHostedToken.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            DeviceTokenCard(
                token = selfHostedToken,
                onCopyToken = onCopyToken,
                onResetToken = onResetToken
            )
        }
    }
}
