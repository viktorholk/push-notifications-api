package com.viktorholk.apipushnotifications.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.viktorholk.apipushnotifications.models.PushNotification

@Composable
fun StatusScreen(
    logs: String, 
    token: String, 
    connectionStatus: String,
    onStopService: () -> Unit,
    onCopyToken: (String) -> Unit,
    notifications: List<PushNotification> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Status: $connectionStatus", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (token.isNotBlank()) {
            DeviceTokenCard(
                token = token,
                onCopyToken = onCopyToken,
                onResetToken = null // No reset on status screen
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = onStopService,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop Service")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Recent Notifications:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(notifications) { notification ->
                NotificationItem(notification)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
