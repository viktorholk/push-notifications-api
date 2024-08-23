package com.viktorholk.apipushnotifications;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.regex.Pattern;

public class ConfigurationFragment extends Fragment {

    private static final Pattern URL_PATTERN = Pattern.compile("https?:\\/\\/(.*)");
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public ConfigurationFragment() {
        super(R.layout.fragment_configuration);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = MainActivity.sharedPreferences;
        editor = sharedPreferences.edit();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText urlEditText = view.findViewById(R.id.urlEditText);
        Button applyButton = view.findViewById(R.id.applyButton);

        String urlShared = sharedPreferences.getString("url", "");
        if (!urlShared.isEmpty()) {
            urlEditText.setText(urlShared);
        }

        applyButton.setOnClickListener(v -> applyConfiguration(urlEditText.getText().toString()));
    }

    private void applyConfiguration(String urlText) {
        if (urlText.length() == 0)
            return;

        if (!URL_PATTERN.matcher(urlText).matches()) {
            urlText = "http://" + urlText;
        }

        String urlShared = sharedPreferences.getString("url", "");
        if (!urlText.equals(urlShared)) {
            editor.putString("url", urlText);
            editor.apply();
        }

        navigateToServiceFragment();
    }

    private void navigateToServiceFragment() {
        MainActivity.fragmentManager.beginTransaction()
                .replace(R.id.fragmentView, ServiceFragment.class, null)
                .commit();
        MainActivity.bottomNavigationView.setSelectedItemId(R.id.service);
    }
}
