package com.viktorholk.apipushnotifications;

import java.util.regex.Pattern;

public class Utils {
    public static String formatURL(String value) {
        final Pattern pattern = Pattern.compile("https?:\\/\\/(.*)");

        // Add http to the URL if no protocol is defined
        if (!pattern.matcher(value).matches()) {
            value = String.format("http://%s", value);
        }

        return value;
    }
}
