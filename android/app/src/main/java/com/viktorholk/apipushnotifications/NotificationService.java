package com.viktorholk.apipushnotifications;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Objects;

public class NotificationService extends Service {
    private Boolean running = false;
    private NotificationManager notificationManager;

    private Notification getForegroundNotification(){
        // Create the notification channel
        String channelForegroundNotifications = "PNA_FOREGROUND_SERVICE_CHANNEL";
        NotificationChannel channel = new NotificationChannel(channelForegroundNotifications, "Foreground Notification", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Channel for the service notification");
        notificationManager.createNotificationChannel(channel);

        // Build and return the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelForegroundNotifications);
        return builder.setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private Notification getNotification(String title, String body){
        // Create the notification channel
        String channelNotifications = "PNA_PUSH_NOTIFICATIONS_CHANNEL";
        NotificationChannel channel = new NotificationChannel(channelNotifications, "Push Notifications", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Channel for push notifications");
        channel.setLightColor(R.color.blue);
        channel.setVibrationPattern(new long[]{0, 50, 250, 100});
        channel.enableVibration(true);
        channel.enableLights(true);
        notificationManager.createNotificationChannel(channel);

        // Build and return the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelNotifications);

        return builder
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setColor(ContextCompat.getColor(this, R.color.blue))
                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);
        // Start the foreground notification
        startForeground(1, getForegroundNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Broadcast Intent
        Intent broadcastIntent = new Intent().setAction("broadcastServiceError");

        running = true;
        Thread _thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        // Get the URL
                        final String url = MainActivity.sharedPreferences.getString("url", "");
                        if (url.equals("")) continue;

                        // Create the response
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    if (response.has("data")) {
                                        String title = response.getJSONObject("data").getString("title");
                                        String body = response.getJSONObject("data").getString("body");
                                        // broadcast empty error message to service fragment (to remove previous error message - if there was any)
                                        broadcastIntent.putExtra("message", "");
                                        sendBroadcast(broadcastIntent);

                                        // Each notification has to have a unique id so we dont overwrite it
                                        // We track the notificaitons the the shared preferences
                                        int notificationNumber = MainActivity.sharedPreferences.getInt("notificationNumber", 2);

                                        // Increment the notificaton number in the prefs
                                        SharedPreferences.Editor editor = MainActivity.sharedPreferences.edit();
                                        editor.putInt("notificationNumber", notificationNumber + 1);
                                        editor.apply();

                                        // Show the notificaton
                                        notificationManager.notify(notificationNumber, getNotification(title, body));
                                    }
                                } catch (JSONException error) {
                                    broadcastIntent.putExtra("message", error.toString());
                                    sendBroadcast(broadcastIntent);
                                }
                            }
                        }, new Response.ErrorListener() {
                            @SuppressLint("DefaultLocale")
                            @Override
                            public void onErrorResponse(VolleyError error) {

                                // Check whether or not it is a Java Volley error or the response
                                if (Objects.isNull(error.networkResponse)) {
                                    // Volley error
                                    broadcastIntent.putExtra("message", String.format("%s", error.toString()));
                                } else {
                                    final int errorCode = error.networkResponse.statusCode;
                                    final String errorMessage = error.toString();
                                    // Parse the data from the API
                                    try {
                                        final String responseBody = new String(error.networkResponse.data, "utf-8");
                                        JSONObject jsonData = new JSONObject(responseBody);
                                        Log.i("UnsupportedEncodingException | JSONException", jsonData.toString());
                                        // Add the broadcastIntent extra
                                        broadcastIntent.putExtra("message", String.format("STATUS: %d%n%s%n%s", errorCode, errorMessage, jsonData.toString()));

                                    } catch (UnsupportedEncodingException | JSONException e) {
                                        Log.i("UnsupportedEncodingException | JSONException", error.toString());
                                    }
                                }
                                sendBroadcast(broadcastIntent);
                                stopSelf();
                            }
                        });
                        // Add the request to the queue
                        RequestService.getInstance(getBaseContext()).addToRequestQueue(jsonObjectRequest);

                        // Convert the poll time to seconds for the thread to sleep
                        final int hour = MainActivity.sharedPreferences.getInt("pollHour", 0);
                        final int minute = MainActivity.sharedPreferences.getInt("pollMinute", 5);

                        final int sleepTime = (hour * 3600 + minute * 60) * 1000;
                        Thread.sleep(sleepTime);

                    } catch (InterruptedException error) {
                        broadcastIntent.putExtra("message", error.toString());
                        sendBroadcast(broadcastIntent);
                    }
                }
            }
        });
        _thread.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
