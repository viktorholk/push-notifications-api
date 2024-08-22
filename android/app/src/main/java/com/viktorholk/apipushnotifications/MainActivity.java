package com.viktorholk.apipushnotifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    public static SharedPreferences sharedPreferences;
    public static FragmentManager fragmentManager;
    public static BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initFields();

        setupBottomNavigationView();

        if (isUrlConfigured()) {
            loadFragment(ServiceFragment.class);
            bottomNavigationView.setSelectedItemId(R.id.service);
        } else {
            loadFragment(ConfigurationFragment.class);
        }
    }

    private void initFields() {
        sharedPreferences = getSharedPreferences(getString(R.string.sharedPreferences), Context.MODE_PRIVATE);
        fragmentManager = getSupportFragmentManager();
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.configuration:
                    loadFragment(ConfigurationFragment.class);
                    return true;
                case R.id.service:
                    if (!isUrlConfigured()) {
                        showToast("You need to configure the URL before using the service.");
                        return false;
                    }
                    loadFragment(ServiceFragment.class);
                    return true;
                default:
                    return false;
            }
        });
    }

    private boolean isUrlConfigured() {
        return !sharedPreferences.getString("url", "").isEmpty();
    }

    private void loadFragment(Class<? extends Fragment> fragmentClass) {
        try {
            Fragment fragment = fragmentClass.newInstance();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragmentView, fragment)
                    .commit();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            showToast("Failed to load fragment.");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
