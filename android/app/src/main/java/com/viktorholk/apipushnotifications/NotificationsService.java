package com.viktorholk.apipushnotifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;

public class NotificationsService extends Service {
    public static boolean running = false;
    private static final String LOG_TAG = "NotificationsService";
    private static final String FOREGROUND_CHANNEL_ID = "FOREGROUND_PUSH_NOTIFICATIONS_API";
    private static final String NOTIFICATIONS_CHANNEL_ID = "PUSH_NOTIFICATIONS_API";
    private static final AtomicInteger notificationIdCounter = new AtomicInteger(1);

    private SharedPreferences sharedPreferences;
    private OkHttpClient client;
    private Call currentCall;
    private boolean isStoppedByUser = false;

    private static final int MAX_RETRIES = 5;
    private static final int RETRY_TIME = 2000;
    private int retryCount = 0;

    private final Intent serviceFragmentBroadcast = new Intent("serviceFragmentBroadcast");

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = MainActivity.sharedPreferences;
        client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        running = true;
        Log.i(LOG_TAG, "Service started");
        startForegroundService();
        listenForNotifications();
        return START_STICKY;
    }

    private void listenForNotifications() {
        broadcast("Connecting...", false);

        String url = MainActivity.sharedPreferences.getString("url", "");
        Request request = new Request.Builder()
                .addHeader("Accept", "text/event-stream")
                .url(url)
                .build();

        currentCall = client.newCall(request);
        currentCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handleFailure(e, true);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                retryCount = 0;
                if (!response.isSuccessful()) {
                    handleFailure(new IOException("Response failed with status code: " + response.code()), false);
                    return;
                }

                if (!"text/event-stream".equals(response.header("Content-Type"))) {
                    handleFailure(new IOException("Expected response content type to be an event stream"), false);
                    return;
                }

                handleSuccess(response);
            }
        });
    }


    private void handleFailure(IOException e, boolean withRetry) {
        if (isStoppedByUser) {
            broadcast("Stopped", false);
            return;
        }

        // Try to reconnect
        if (withRetry && (retryCount < MAX_RETRIES)) {
            retryCount++;
            broadcast(String.format("Retrying Connection (%s) \n%s", retryCount, e), false);
            try {
                Thread.sleep(RETRY_TIME);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
            listenForNotifications();
        } else {
            broadcast(e.toString(), true);
            stopSelf();
        }
    }

    private void handleSuccess(@NonNull Response response) throws IOException {
        Log.i(LOG_TAG, "Successfully connected to " + response.request().url().toString());
        broadcast("Connected", false);

        BufferedSource source = response.body().source();
        while (!isStoppedByUser) {
            try {
                String line = source.readUtf8Line();
                if (line != null && line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    Log.i(LOG_TAG, "Received data: " + data);
                    if (!data.contains("Connected")) {
                        PushNotification notification = new Gson().fromJson(data, PushNotification.class);
                        showNotification(notification);
                    }
                }
            } catch (Exception e) {
                if (isStoppedByUser) {
                    broadcast("Stopped", false);
                } else
                    handleFailure(new IOException("Lost connection"), true);
                break;
            }
        }
    }

    private void createNotificationChannels() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null) return;

        NotificationChannel foregroundChannel = new NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
        );
        foregroundChannel.setDescription("Push Notifications API Foreground Service");

        NotificationChannel notificationChannel = new NotificationChannel(
                NOTIFICATIONS_CHANNEL_ID,
                "Notification Service Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationChannel.setDescription("Channel for Push Notifications API Service");
        notificationChannel.setLightColor(R.color.blue);
        notificationChannel.setVibrationPattern(new long[]{0, 50, 250, 100});
        notificationChannel.enableVibration(true);
        notificationChannel.enableLights(true);

        notificationManager.createNotificationChannel(foregroundChannel);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        int serviceId = 1;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(serviceId, notification);
        } else {
            startForeground(serviceId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }
    }

    private void showNotification(PushNotification notification) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATIONS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setColor(ContextCompat.getColor(this, R.color.blue))
                .setContentTitle(notification.getTitle())
                .setContentText(notification.getMessage())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        String notificationUrl = notification.getUrl();
        if (notificationUrl != null && !notificationUrl.isEmpty()) {

            notificationUrl = Utils.parseURL(notificationUrl);

            // Create the intent and pending intent
            Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(notificationUrl));
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
            );

            builder.setContentIntent(pendingIntent);
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            Log.i(LOG_TAG, "Notifying: " + notification.getTitle());
            notificationManager.notify(notificationIdCounter.incrementAndGet(), builder.build());
        }
    }

    private void broadcast(String message, boolean isError) {
        if (!Objects.isNull(message)) {
            serviceFragmentBroadcast.putExtra("message", message);
            serviceFragmentBroadcast.putExtra("isError", isError);
            Log.i(LOG_TAG, message);
        }
        sendBroadcast(serviceFragmentBroadcast);
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "Service stopped");
        running = false;
        isStoppedByUser = true;
        if (currentCall != null) {
            currentCall.cancel();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}