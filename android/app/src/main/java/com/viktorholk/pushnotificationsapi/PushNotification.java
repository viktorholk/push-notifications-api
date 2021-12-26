package com.viktorholk.pushnotificationsapi;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalDateTime;

public class PushNotification {

    private String title;
    private String body;
    private LocalDateTime date;

    public PushNotification(String title, String body) {
        this.title = title;
        this.body = body;
        this.date = LocalDateTime.now();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getDate() {
        return date;
    }
}
