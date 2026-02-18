package com.viktorholk.apipushnotifications.ui

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.viktorholk.apipushnotifications.NotificationsService
import com.viktorholk.apipushnotifications.Shared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.viktorholk.apipushnotifications.models.PushNotification
import java.util.Calendar

class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient()

    init {
        loadConfig()
        checkServiceStatus()
        if (_uiState.value.isServiceRunning) {
            fetchNotifications()
        }
    }

    private fun loadConfig() {
        val context = getApplication<Application>().applicationContext

        _uiState.update { currentState ->
            currentState.copy(
                selfHostedUrl = Shared.getString(context, "self_hosted_url", ""),
                selfHostedToken = Shared.getString(context, "self_hosted_token", "")
            )
        }
    }

    private fun checkServiceStatus() {
         _uiState.update { it.copy(isServiceRunning = NotificationsService.running) }
    }

    fun updateSelfHostedUrl(url: String) {
        _uiState.update { it.copy(selfHostedUrl = url) }
    }

    fun updateSelfHostedToken(token: String) {
        _uiState.update { it.copy(selfHostedToken = token) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun registerAndConnect(baseUrl: String) {
        val formattedUrl = formatUrl(baseUrl)
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val token = registerDevice(formattedUrl)
                if (token != null) {
                    _uiState.update { it.copy(selfHostedToken = token, selfHostedUrl = formattedUrl) }
                    saveAndConnect()
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to register. Please check URL.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

data class RegistrationResponse(val token: String)

    private suspend fun registerDevice(baseUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/register")
                .post("".toRequestBody(null))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val registrationResponse = Gson().fromJson(responseBody, RegistrationResponse::class.java)
                    return@use registrationResponse.token
                }
                return@use null
            }
        } catch (e: Exception) {
            Log.e("ConfigViewModel", "Error registering device", e)
            null
        }
    }

    private fun formatUrl(url: String): String {
        return if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
    }

    fun saveAndConnect() {
        val context = getApplication<Application>().applicationContext
        val formattedUrl = formatUrl(uiState.value.selfHostedUrl)
        Shared.saveData(context, "self_hosted_url", formattedUrl)
        Shared.saveData(context, "self_hosted_token", uiState.value.selfHostedToken)
        val url = "$formattedUrl/events?token=${uiState.value.selfHostedToken}"

        Shared.saveData(context, "url", url)
        startService()
        fetchNotifications()
    }

    private fun startService() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, NotificationsService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        _uiState.update { it.copy(isServiceRunning = true, serviceLogs = "Starting service...") }
    }

    fun fetchNotifications() {
        val currentState = uiState.value
        val baseUrl = formatUrl(currentState.selfHostedUrl)
        val token = currentState.selfHostedToken

        // Default since 7 days ago
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val since = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(calendar.time)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val httpUrl = "$baseUrl/notifications".toHttpUrlOrNull()
                if (httpUrl == null) {
                    _uiState.update { it.copy(errorMessage = "Invalid URL") }
                    return@launch
                }
                val urlBuilder = httpUrl.newBuilder()
                urlBuilder.addQueryParameter("token", token)
                urlBuilder.addQueryParameter("since", since)

                val request = Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val type = object : TypeToken<List<PushNotification>>() {}.type
                            val notifications: List<PushNotification> = Gson().fromJson(responseBody, type)
                            _uiState.update { it.copy(notifications = notifications) }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ConfigViewModel", "Error fetching notifications", e)
            }
        }
    }

    fun stopService() {
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, NotificationsService::class.java)
        context.stopService(intent)
         _uiState.update { it.copy(isServiceRunning = false, serviceLogs = "Service stopped.") }
    }

    fun handleBroadcast(intent: Intent) {
        val message = intent.getStringExtra("message")
        val notificationJson = intent.getStringExtra("notification_json")

        if (notificationJson != null) {
            try {
                val notification = Gson().fromJson(notificationJson, PushNotification::class.java)
                _uiState.update {
                    it.copy(notifications = listOf(notification) + it.notifications)
                }
            } catch (e: Exception) {
                Log.e("ConfigViewModel", "Error parsing notification broadcast", e)
            }
        }

        if (message != null) {
            if (message == "Connected") {
                fetchNotifications()
            }

             _uiState.update {
                 val newStatus = when {
                     message == "Connected" -> "Connected"
                     message == "Connecting..." -> "Connecting..."
                     message.startsWith("Retrying") -> "Reconnecting..."
                     message == "Stopped" -> "Disconnected"
                     else -> it.connectionStatus
                 }

                 it.copy(
                     serviceLogs = if (it.serviceLogs.isEmpty()) message else "${it.serviceLogs}\n$message",
                     isServiceRunning = NotificationsService.running,
                     connectionStatus = newStatus
                 )
             }
        }
    }
}

data class ConfigUiState(
    val isServiceRunning: Boolean = false,
    val serviceLogs: String = "",
    val selfHostedUrl: String = "",
    val selfHostedToken: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val notifications: List<PushNotification> = emptyList(),
    val connectionStatus: String = "Disconnected"
)
