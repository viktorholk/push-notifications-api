package com.viktorholk.apipushnotifications.models;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;

import androidx.core.graphics.drawable.IconCompat;

import java.util.Objects;

public class PushNotification {

    private String title;
    private String message;
    private String url;

    private String icon;
    private String color;


    public PushNotification(String title, String message, String url, String icon, String color) {
        this.title = title;
        this.message = message;
        this.url = url;
        this.icon = icon;
        this.color = color;
    }


    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getUrl() {
        return url;
    }

    public IconCompat getIcon() {
        if (Objects.isNull(this.icon))
            return null;

        try {
            final byte[] bytes = Base64.decode(icon, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            return IconCompat.createWithBitmap(bitmap);
        } catch (Exception e) {
            return null;
        }


    }

    public int getColor() {
        try {
            return Color.parseColor(this.color);
        } catch (Exception e)
        {
            return -1;
        }
    }

}



