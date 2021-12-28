package com.viktorholk.apipushnotifications;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    public static SharedPreferences sharedPreferences;
    public static FragmentManager   fragmentManager;

    public static BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the sharedPreferences and fragmentManager
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
//        sharedPreferences.edit().clear().apply();
        fragmentManager = getSupportFragmentManager();

        // setup the navigation view
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        //bottomNavigationView.getMenu().findItem(R.id.home).setVisible(false);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.configuration:
                        fragmentManager.beginTransaction()
                                .replace(R.id.fragmentView, ConfigurationFragment.class, null)
                                .commit();
                    return true;

                case R.id.service:
                    // Check if there is a url in the configuration, if not redirect to the configuration fragment
                    if (MainActivity.sharedPreferences.getString("url", "").equals("")) {
                        Toast.makeText(this, "You need to configure the URL before using the service.", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    MainActivity.fragmentManager.beginTransaction()
                            .replace(R.id.fragmentView, ServiceFragment.class, null)
                            .commit();

                    return true;
            }
            return false;
        });

        // If there is not a url configured direct to the configuration fragment, otherwise direct to the notifications service
        if (sharedPreferences.getString("url", "").equals("")) {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragmentView, ConfigurationFragment.class, null)
                    .commit();

        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragmentView, ServiceFragment.class, null)
                    .commit();
            // Update the selected item for the navigation
            bottomNavigationView.setSelectedItemId(R.id.service);
        }


    }
}