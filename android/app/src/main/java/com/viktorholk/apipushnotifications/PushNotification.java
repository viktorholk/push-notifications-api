package com.viktorholk.apipushnotifications;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalDateTime;

public class PushNotification {

    private String title;
    private String message;

    public PushNotification(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

}
