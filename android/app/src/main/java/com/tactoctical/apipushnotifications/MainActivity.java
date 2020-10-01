package com.tactoctical.apipushnotifications;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

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

        TextView _3 = findViewById(R.id.textView3);
        _3.setText(sharedPreferences.getString("endpoint", "empty"));
    }
}
