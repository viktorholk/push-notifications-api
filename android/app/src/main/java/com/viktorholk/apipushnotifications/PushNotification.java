package com.viktorholk.apipushnotifications;
public class PushNotification {

    private String title;
    private String message;
    private String url;

    public PushNotification(String title, String message, String url) {
        this.title = title;
        this.message = message;
        this.url = url;
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

}
