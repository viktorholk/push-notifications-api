package com.viktorholk.apipushnotifications.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.viktorholk.apipushnotifications.models.PushNotification
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotificationItem(notification: PushNotification) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (notification.token == null) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Broadcast",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            val formattedDate = remember(notification.createdAt) {
                try {
                    // Assuming ISO 8601 format from server
                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                    val date = parser.parse(notification.createdAt)
                    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    formatter.format(date!!)
                } catch (e: Exception) {
                    notification.createdAt ?: ""
                }
            }
            
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                if (!notification.message.isNullOrBlank()) {
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (!notification.url.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notification.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
