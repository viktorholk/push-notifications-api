package com.viktorholk.apipushnotifications.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.viktorholk.apipushnotifications.ui.components.ConfigurationScreen
import com.viktorholk.apipushnotifications.ui.components.StatusScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ConfigViewModel,
    onConnectRequested: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Push Notifications API") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (uiState.isServiceRunning) {
                StatusScreen(
                    logs = uiState.serviceLogs,
                    token = uiState.selfHostedToken,
                    connectionStatus = uiState.connectionStatus,
                    onStopService = { viewModel.stopService() },
                    onCopyToken = {
                         scope.launch {
                             snackbarHostState.showSnackbar("Token copied to clipboard")
                         }
                    },
                    notifications = uiState.notifications
                )
            } else {
                ConfigurationScreen(
                    selfHostedUrl = uiState.selfHostedUrl,
                    selfHostedToken = uiState.selfHostedToken,
                    isLoading = uiState.isLoading,
                    onSelfHostedUrlChange = viewModel::updateSelfHostedUrl,
                    onConnect = onConnectRequested,
                    onCopyToken = {
                         scope.launch {
                             snackbarHostState.showSnackbar("Token copied to clipboard")
                         }
                    },
                    onResetToken = {
                        viewModel.updateSelfHostedToken("")
                    }
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
