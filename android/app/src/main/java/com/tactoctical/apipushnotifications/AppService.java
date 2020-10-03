package com.tactoctical.apipushnotifications;
import android.os.Build;
import android.util.Log;
import android.os.IBinder;

import org.json.JSONException;
import org.json.JSONObject;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.app.Notification;
import com.android.volley.Request;
import com.android.volley.Response;
import androidx.annotation.Nullable;
import com.android.volley.VolleyError;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import com.android.volley.toolbox.JsonObjectRequest;

public class AppService extends Service {
    private Thread _thread;
    private boolean running = false;

    private String notificationChannelIdForeground  = "com.tactoctical.APN_FOREGROUND";
    private String notificationChannelIdAPI         = "com.tactoctical.APN_NOTIFICATION_CHANNEl";
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            foregroundNotification();
        else
            startForeground(1, new Notification());
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        running = true;
        _thread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (running){
                    try{
                        String url = MainActivity.sharedPreferences.getString("endpoint", "");
                        if (url.equals("")){
                            continue;
                        }
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try{
                                            int code = response.getInt("status");
                                            if (code == 200){
                                                String title        = response.getJSONObject("data").getString("title");
                                                String description  = response.getJSONObject("data").getString("description");
                                                apiNotification(title,description);
                                            }

                                        } catch (JSONException e){
                                            Toast.makeText(AppService.this, e.toString(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }, new Response.ErrorListener() {

                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Toast.makeText(AppService.this, error.toString(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                        RequestService.getInstance(getBaseContext()).addToRequestQueue(jsonObjectRequest);
                        Thread.sleep(1000);
                    } catch (InterruptedException e){
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        _thread.start();
        return START_STICKY;
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

    private void foregroundNotification(){
        NotificationChannel chan = new NotificationChannel(notificationChannelIdForeground, "Foreground Service", NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        notificationManager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, notificationChannelIdForeground);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    private void apiNotification(String title, String description){
        NotificationChannel channel = new NotificationChannel(notificationChannelIdAPI, "Push Notifications", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Channel for all APN API notifications");

        notificationManager.createNotificationChannel(channel);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, notificationChannelIdAPI)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.DEFAULT_ALL);

        notificationManager.notify(1, builder.build());
    }
}
