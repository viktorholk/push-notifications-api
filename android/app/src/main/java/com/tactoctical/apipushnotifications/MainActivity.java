package com.tactoctical.apipushnotifications;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;


public class MainActivity extends AppCompatActivity {

    public static final String PREFS = "Prefs";
    public static SharedPreferences sharedPreferences;

    private String endpoint;
    private TextView endpointTextView;
    private TextView statusTextView;

    private Intent AppService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences   = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        endpoint            = sharedPreferences.getString("endpoint", null);

        endpointTextView    = findViewById(R.id.endpointTextView);
        statusTextView      = findViewById(R.id.statusTextView);

        AppService = new Intent(this, AppService.class);

        // If no endpoint is cached in settings change activity
        if (endpoint == null){
            Intent intent = new Intent(this, OptionsActivity.class);
            startActivity(intent);
        }
        // Update text views
        endpointTextView.setText(endpoint);

        if (isServiceRunning(AppService.class)){
            statusTextView.setText("Running");
            statusTextView.setTextColor(getResources().getColor(R.color.colorPrimary));
        }else{
            statusTextView.setText("Stopped");
            statusTextView.setTextColor(getResources().getColor(R.color.colorAccent));
        }



        // Buttons
        // Options button
        Button optionsButton = findViewById(R.id.optionsButton);
        optionsButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(view.getContext(), OptionsActivity.class);
                        startActivity(intent);
                    }
                }
        );

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start service
                startService(AppService);
                statusTextView.setText("Running");
                statusTextView.setTextColor(getResources().getColor(R.color.colorPrimary));
            }
        });

        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(AppService);
                statusTextView.setText("Stopped");
                statusTextView.setTextColor(getResources().getColor(R.color.colorAccent));
            }
        });
    }


    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("Service status", "Running");
                return true;
            }
        }
        Log.i ("Service status", "Not running");
        return false;
    }
}
