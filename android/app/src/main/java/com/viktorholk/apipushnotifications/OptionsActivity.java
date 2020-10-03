package com.viktorholk.apipushnotifications;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class OptionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        SharedPreferences sharedPreferences = MainActivity.sharedPreferences;
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        final EditText endpoint       = findViewById(R.id.editEndpoint);
        Button applyButton  = findViewById(R.id.applyButton);

        // Set the config as placeholder if it exists
        if (!sharedPreferences.getString("endpoint", "").equals("")){
            endpoint.setText(sharedPreferences.getString("endpoint", ""));
        }

        applyButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!endpoint.getText().toString().equals("")){
                            // update prefs
                            editor.putString("endpoint", endpoint.getText().toString());
                            editor.commit();

                            // Change indent
                            Intent intent = new Intent(view.getContext(), MainActivity.class);
                            startActivity(intent);
                        }
                    }
                }
        );

    }
}
