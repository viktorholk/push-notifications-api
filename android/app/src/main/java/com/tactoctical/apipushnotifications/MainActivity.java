package com.tactoctical.apipushnotifications;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.tactoctical.apipushnotifications.NotificationService;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    public static final String PREFS = "Prefs";
    public static SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String endpoint     = sharedPreferences.getString("endpoint", null);

        if (endpoint == null){
            Intent intent = new Intent(this, Options.class);
            startActivity(intent);
        }

        Button optionsButton = findViewById(R.id.optionsButton);
        optionsButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(view.getContext(), Options.class);
                        startActivity(intent);
                    }
                }
        );
        final TextView _2 = findViewById(R.id.textView2);
        final TextView _3 = findViewById(R.id.textView3);
        _3.setText(sharedPreferences.getString("endpoint", "empty"));

        Button notificationButton = findViewById(R.id.notificationButton);
        notificationButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        NotificationService notificationService = new NotificationService(view.getContext());
                        notificationService.DeployNotification();
                    }
                }
        );

        Button requestButton = findViewById(R.id.requestButton);
        requestButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String url = "https://api.tactoctical.com/twitch-app/token";

                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                    @Override
                                    public void onResponse(JSONObject response) {
                                        _2.setText("Response: " + response.toString());
                                    }
                                }, new Response.ErrorListener() {

                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        // TODO: Handle error

                                    }
                                });
                        RequestService.getInstance(view.getContext()).addToRequestQueue(jsonObjectRequest);
                    }
                }
        );
    }
}
