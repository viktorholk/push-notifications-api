package com.viktorholk.apipushnotifications;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurationFragment extends Fragment {
    public ConfigurationFragment() {
        super(R.layout.fragment_configuration);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText urlEditText        = view.findViewById(R.id.urlEditText);
        Button pollTimeButton       = view.findViewById(R.id.pollTimeButton);
        TextView pollTimeTextView   = view.findViewById(R.id.pollTimeTextView);
        Button applyButton          = view.findViewById(R.id.applyButton);

        // Init the preferences
        SharedPreferences sharedPreferences = MainActivity.sharedPreferences;
        SharedPreferences.Editor editor     = MainActivity.sharedPreferences.edit();

        // Get the data from the shared preferences
        final String urlShared     = sharedPreferences.getString("url", "");

        final int pollHourShared       = sharedPreferences.getInt("pollHour", 0);
        final int pollMinuteShared     = sharedPreferences.getInt("pollMinute", 5);

        // Update poll text
        pollTimeTextView.setText(getPollText(pollHourShared, pollMinuteShared));

        if(!urlShared.isEmpty()) {
            urlEditText.setText(urlShared);
        }
        // Create the time picker
        MaterialTimePicker picker =  new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(pollHourShared)
                .setMinute(pollMinuteShared)
                .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
                .setTitleText("Poll time\nfor every request")
                .build();

        picker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Update the poll time
                if (picker.getHour() != pollHourShared) {
                    // Update the hour
                    editor.putInt("pollHour", picker.getHour());
                }

                if (picker.getMinute() != pollMinuteShared) {
                    // Update the minute
                    editor.putInt("pollMinute", picker.getMinute());
                }
                // Update poll text
                pollTimeTextView.setText(getPollText(picker.getHour(), picker.getMinute()));

            }
        });

        pollTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                picker.show(MainActivity.fragmentManager, "picker");
            }
        });

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Update the url in the preferences
                final String urlText = urlEditText.getText().toString();

                // Check if the url start with https?://
                String regex = "https?:\\/\\/(.*)";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(urlText);

                if (!matcher.matches()) {
                    Toast.makeText(getActivity(), "Please enter a valid URL", Toast.LENGTH_SHORT).show();
                    return;
                };

                if (!urlText.equals(urlShared)) {
                    editor.putString("url", urlEditText.getText().toString());
                    editor.apply();
                }

                try {
                    if (picker.getHour() != pollHourShared || picker.getMinute() != pollMinuteShared) {
                        editor.apply();
                    }
                } catch (NullPointerException e) {
                    // picker.getHour() and getMinute() will raise a NullPointerException if the dialog hasen't been shown.
                    // We don't need to update the editor if the dialog hasen't been shown since the time is stored in the preferences
                }

                // Change fragment to the notifications service
                MainActivity.fragmentManager.beginTransaction()
                        .replace(R.id.fragmentView, ServiceFragment.class, null)
                        .commit();
                MainActivity.bottomNavigationView.setSelectedItemId(R.id.service);
            }
        });
    }

    @SuppressLint("DefaultLocale")
    private String getPollText(int hour, int minute){
        if (hour > 0) {
            if (minute > 0) {
                return String.format("Current poll time is every %d hour(s) and %d minute(s)", hour, minute);
            }
            return String.format("Current poll time is every %d hour(s)", hour);
        } else {
            if (minute > 0) {
                return String.format("Current poll time is every %d minute(s)", minute);
            }
        }
        return "";
    }
}