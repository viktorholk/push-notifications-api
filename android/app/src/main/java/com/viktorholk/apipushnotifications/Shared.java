package com.viktorholk.apipushnotifications;

import android.content.Context;
import android.content.SharedPreferences;

public class Shared {
    private static SharedPreferences instance;
    private static synchronized SharedPreferences getInstance(Context context) {
        if (instance == null) {
            instance = context.getSharedPreferences("APN Shared Preferences2", Context.MODE_PRIVATE);
        }
        return instance;
    }

    public static void saveData(Context context, String key, String value) {
        SharedPreferences.Editor editor = getInstance(context).edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getString(Context context, String key, String defaultValue) {
        return getInstance(context).getString(key, defaultValue);
    }
}