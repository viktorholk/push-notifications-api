package com.viktorholk.apipushnotifications;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.w3c.dom.Text;

import java.util.Objects;

public class ServiceFragment extends Fragment {

    private Intent notificatonsService;

    private TextView serviceErrorTextView;
    private ImageView serviceIcon;
    private TextView stateTextView;
    private MaterialButton serviceToggleButton;

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void toggleServiceView(Context context, Boolean state){
        // state true   = service is running
        // state false  = service have been stopped

        // Update the icon, text and button dependent if the service is either running or have been stopped
        if (state) {
            // Icon
            serviceIcon.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            serviceIcon.setColorFilter(ContextCompat.getColor(context, R.color.blue));
            // Text
            stateTextView.setText("Running");
            stateTextView.setTextColor(ContextCompat.getColor(context, R.color.blue));

            // button
            serviceToggleButton.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_close_black_24dp));
            serviceToggleButton.setText("STOP");
        } else {
            // Icon
            serviceIcon.setImageResource(R.drawable.ic_close_black_24dp);
            serviceIcon.setColorFilter(ContextCompat.getColor(context, R.color.blue));
            // Text
            stateTextView.setText("Stopped");
            stateTextView.setTextColor(ContextCompat.getColor(context, R.color.blue));

            // Button
            serviceToggleButton.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_done_black_24dp));
            serviceToggleButton.setText("START");
        }
    }

    public ServiceFragment() {
        super(R.layout.fragment_service);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create the indent from the NotificatonService class
        notificatonsService = new Intent(getActivity(), NotificationService.class);

        // Register the receiver from the service
        // To update the status message whether it is an error or success message.
        getActivity().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String errorMessage = intent.getStringExtra("message");
                // Update the TextView
                serviceErrorTextView.setText(errorMessage);

                // Toggle the service view since it has been stopped
                // If the errorMessage is empty it is a broadcast from onResponse, so no error // Dont toggleTheServiceView
                if (errorMessage.length() > 0) toggleServiceView(context, false);
            }
        }, new IntentFilter("broadcastServiceError"));
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        serviceErrorTextView    = view.findViewById(R.id.serviceErrorTextView);
        serviceIcon         = view.findViewById(R.id.serviceIconImageView);
        stateTextView       = view.findViewById(R.id.serviceStateTextView);
        serviceToggleButton = view.findViewById(R.id.serviceToggleButton);

        if (isServiceRunning(NotificationService.class)) {
            toggleServiceView(getContext(), true);
        } else {
            toggleServiceView(getContext(), false);
        }

        serviceToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(getActivity(), Boolean.toString(isServiceRunning(NotificationService.class)), Toast.LENGTH_SHORT).show();
                if (isServiceRunning(NotificationService.class)) {
                    // Stop the service
                    getActivity().stopService(new Intent(getActivity(), NotificationService.class));
                    toggleServiceView(getContext(), false);
                } else {
                    // Start the service
                    getActivity().startService(new Intent(getActivity(), NotificationService.class));
                    toggleServiceView(getContext(), true);
                }
            }
        });
    }
}