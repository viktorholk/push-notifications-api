package com.tactoctical.apipushnotifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;


public class AppService extends Service {
    private int counter = 0;
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private Thread _thread;
    private boolean running = false;
    @Override
    public void onCreate() {
        super.onCreate();
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
                        String url = "http://10.161.84.81:5000/";

                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                    @Override
                                    public void onResponse(JSONObject response) {
                                        Log.i("response", response.toString());

                                    }
                                }, new Response.ErrorListener() {

                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        // TODO: Handle error
                                        Log.i("error", error.toString());
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
        String NOTIFICATION_CHANNEL_ID = "com.tactoctical.APN_NOTIFICATION_CHANNEl";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }
}
