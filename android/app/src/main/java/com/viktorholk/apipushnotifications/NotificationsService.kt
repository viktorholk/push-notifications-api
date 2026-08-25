package com.viktorholk.apipushnotifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.viktorholk.apipushnotifications.models.PushNotification
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NotificationsService : Service() {

    private val serviceFragmentBroadcast = Intent("serviceFragmentBroadcast")
    private var url: String? = null
    private lateinit var client: OkHttpClient
    private var currentCall: Call? = null
    
    // Coroutine Scope for background work
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectionJob: Job? = null
    
    // Connectivity
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val retryTrigger = Channel<Unit>(Channel.CONFLATED)

    // Optimization: Reuse Gson instance
    private val gson = Gson()
    
    private var haveEstablishedConnection = false

    override fun onCreate() {
        super.onCreate()
        serviceFragmentBroadcast.setPackage(packageName)
        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        url = Shared.getString(this, "url", "")

        try {
            // Validate URL
            if (url.isNullOrEmpty()) throw IllegalArgumentException("Empty URL")
            url?.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid URL")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Invalid URL format: $url", e)
            stopSelf()
            return
        }

        createNotificationChannels()
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        Log.i(LOG_TAG, "Service started")
        startForegroundService()
        
        // Start the connection loop in a coroutine
        startConnectionLoop()
        
        return START_STICKY
    }
    
    private fun startConnectionLoop() {
        connectionJob?.cancel()
        connectionJob = serviceScope.launch {
            listenForNotifications()
        }
    }
    
    private fun registerNetworkCallback() {
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i(LOG_TAG, "Network available, triggering retry")
                retryTrigger.trySend(Unit)
            }
        }
        
        try {
             connectivityManager?.registerDefaultNetworkCallback(networkCallback!!)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to register network callback", e)
        }
    }

    private suspend fun listenForNotifications() {
        val candidates = mutableListOf<String>()
        val configuredUrl = url!!
        candidates.add(configuredUrl)
        if (!configuredUrl.endsWith("/events")) {
            val baseUrl = if (configuredUrl.endsWith("/")) configuredUrl.dropLast(1) else configuredUrl
            candidates.add("$baseUrl/events")
        }
        
        var currentCandidateIndex = 0

        while (currentCoroutineContext().isActive) {
             // Try to connect using current candidates
             var connectionSuccess = false
             
             // Iterate through candidates until one works or all fail
             for (i in candidates.indices) {
                // If we have cycled through, start from the index we last left off (or 0)
                val index = (currentCandidateIndex + i) % candidates.size
                val targetUrl = candidates[index]
                
                broadcast("Connecting to $targetUrl...", false)
                
                try {
                    val request = Request.Builder()
                        .addHeader("Accept", "text/event-stream")
                        .url(targetUrl)
                        .build()

                    currentCall = client.newCall(request)
                    val response = currentCall!!.execute()

                    response.use { r ->
                        if (!r.isSuccessful) {
                            // If 404, we might want to try the next candidate immediately
                            if (r.code == 404 && candidates.size > 1) {
                                throw IOException("HTTP 404") // Throw to catch block
                            }
                            throw IOException("Response failed with status code: ${r.code}")
                        }

                        if ("text/event-stream" != r.header("Content-Type")) {
                             // If wrong content type, definitely try next candidate
                            if (candidates.size > 1) {
                                throw IOException("Wrong Content-Type")
                            }
                            throw IOException("Expected response content type to be an event stream")
                        }

                        // We found a working URL!
                        currentCandidateIndex = index // Remember this index for next time
                        haveEstablishedConnection = true
                        connectionSuccess = true
                        Log.i(LOG_TAG, "Successfully connected to " + r.request.url)
                        broadcast("Connected", false)

                        val source = r.body?.source() ?: throw IOException("Response body is null")
                        
                        // Read loop
                        while (currentCoroutineContext().isActive) {
                            val line = source.readUtf8Line() ?: break // EOF means disconnect
                            
                            if (line.isNotEmpty()) {
                                 if (line.startsWith("data: ")) {
                                    val data = line.substring(6).trim()
                                    Log.i(LOG_TAG, "Received data: $data")
                                    if (!data.contains("Connected")) {
                                        try {
                                            val notification = gson.fromJson(data, PushNotification::class.java)
                                            showNotification(notification)
                                            broadcastNotification(data)
                                        } catch (e: Exception) {
                                            Log.e(LOG_TAG, "Error parsing notification: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // If we break out of the read loop (EOF or timeout), it's a disconnect
                    throw IOException("Server closed connection")

                } catch (e: Exception) {
                    // Check if the coroutine was cancelled (service stopping)
                    if (e is CancellationException || !currentCoroutineContext().isActive) {
                        Log.i(LOG_TAG, "Connection loop cancelled")
                        return // Exit function completely
                    }
                    
                    Log.e(LOG_TAG, "Connection error on $targetUrl: ${e.message}")
                    
                    // Logic to decide whether to try next candidate immediately
                    // We only try next candidate if we haven't established a connection yet AND it looks like a configuration error (404, wrong type)
                    // If it's a network error (host unreachable), all candidates will likely fail, so we might as well break and wait.
                    // However, simplified logic: if we haven't succeeded yet, try next candidate.
                    
                    if (!connectionSuccess && candidates.size > 1) {
                         // Continue to next candidate in the for-loop
                         continue
                    }
                    
                    // If we are here, either we had a connection and lost it, or we tried all candidates.
                    // Break the for-loop to go to the wait/retry logic
                    break
                }
             } // End of candidates loop

            // If we are here, we are disconnected.
            if (haveEstablishedConnection) {
                 val disconnectedNotification = PushNotification(
                    "Lost Connection to the Server",
                    "Disconnected",
                    null, null, null, null, null
                )
                showNotification(disconnectedNotification)
            }
            
            broadcast("Reconnecting...", true)
            
            // Smart backoff: Wait for timer OR network signal
            Log.i(LOG_TAG, "Waiting for retry or network signal...")
            withTimeoutOrNull(RETRY_TIME.toLong()) {
                retryTrigger.receive() // Suspends until network is available
            }
            Log.i(LOG_TAG, "Retrying now")
        }
        
        broadcast("Stopped", false)
    }

    private fun broadcastNotification(json: String) {
        val intent = Intent("serviceFragmentBroadcast")
        intent.setPackage(packageName)
        intent.putExtra("notification_json", json)
        sendBroadcast(intent)
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java) ?: return

        val foregroundChannel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        foregroundChannel.description = "Push Notifications API Foreground Service"

        val notificationChannel = NotificationChannel(
            NOTIFICATIONS_CHANNEL_ID,
            "Notification Service Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationChannel.description = "Channel for Push Notifications API Service"
        notificationChannel.lightColor = getColor(R.color.blue)
        notificationChannel.vibrationPattern = longArrayOf(0, 50, 250, 100)
        notificationChannel.enableVibration(true)
        notificationChannel.enableLights(true)

        notificationManager.createNotificationChannel(foregroundChannel)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("Push Notifications API")
            .setContentText("Listening for notifications")
            .setOngoing(true)
            .build()

        val serviceId = 1
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(serviceId, notification)
        } else {
            startForeground(serviceId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w(LOG_TAG, "Foreground service timed out")
        running = false
        broadcast(TIMEOUT_MESSAGE, true)
        showConnectionPausedNotification()
        stopSelf()
    }

    private fun showConnectionPausedNotification() {
        val reopenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val reopenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            reopenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, NOTIFICATIONS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("Notification connection paused")
            .setContentText("Open the app to reconnect.")
            .setContentIntent(reopenPendingIntent)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java)
            ?.notify(TIMEOUT_NOTIFICATION_ID, notification)
    }

    private fun showNotification(notification: PushNotification) {
         val builder = NotificationCompat.Builder(this, NOTIFICATIONS_CHANNEL_ID)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val customIcon = notification.icon
        if (customIcon != null)
            builder.setSmallIcon(customIcon)
        else
            builder.setSmallIcon(R.drawable.ic_notifications_black_24dp)

        val customColor = notification.color
        if (customColor != -1)
            builder.color = customColor
        else
            builder.color = ContextCompat.getColor(this, R.color.blue)

        val notificationUrl = notification.url
        if (!notificationUrl.isNullOrEmpty()) {
            val formattedNotificationUrl = formatURL(notificationUrl)
            val notificationIntent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedNotificationUrl))
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager != null) {
            Log.i(LOG_TAG, "Notifying: " + notification.title)
            notificationManager.notify(notificationIdCounter.incrementAndGet(), builder.build())
        }
    }

    private fun formatURL(url: String): String {
        return if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url
    }

    private fun broadcast(message: String?, isError: Boolean) {
        if (message != null) {
            serviceFragmentBroadcast.putExtra("message", message)
            serviceFragmentBroadcast.putExtra("isError", isError)
            Log.i(LOG_TAG, message)
        }
        sendBroadcast(serviceFragmentBroadcast)
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "Service stopped")
        running = false
        // Cancel the scope to stop all coroutines
        serviceScope.cancel()
        currentCall?.cancel()
        
        // Unregister callback
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback!!)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error unregistering network callback", e)
            }
        }
        
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        var running = false
        const val TIMEOUT_MESSAGE = "Connection paused by Android. Open the app to reconnect."
        private const val LOG_TAG = "NotificationsService"
        private const val FOREGROUND_CHANNEL_ID = "FOREGROUND_PUSH_NOTIFICATIONS_API"
        private const val NOTIFICATIONS_CHANNEL_ID = "PUSH_NOTIFICATIONS_API"
        private const val TIMEOUT_NOTIFICATION_ID = 2
        private val notificationIdCounter = AtomicInteger(1)
        private const val RETRY_TIME = 20000
    }
}
