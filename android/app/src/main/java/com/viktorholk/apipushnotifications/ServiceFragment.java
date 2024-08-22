package com.viktorholk.apipushnotifications;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.Objects;

public class ServiceFragment extends Fragment {
    private TextView serviceMessageTextView;
    private ImageView serviceIcon;
    private MaterialButton serviceToggleButton;

    public ServiceFragment() {
        super(R.layout.fragment_service);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register the receiver to update the status message from the service.
        getActivity().registerReceiver(new ServiceBroadcastReceiver(),
                new IntentFilter("serviceFragmentBroadcast"), Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        serviceMessageTextView = view.findViewById(R.id.serviceMessageTextView);
        serviceIcon = view.findViewById(R.id.serviceIconImageView);
        serviceToggleButton = view.findViewById(R.id.serviceToggleButton);

        toggleServiceView(getContext(), NotificationsService.running);

        serviceToggleButton.setOnClickListener(view1 -> toggleService());
    }


    private void toggleServiceView(Context context, boolean isRunning) {
        int iconRes = isRunning ? R.drawable.ic_play_arrow_black_24dp : R.drawable.ic_close_black_24dp;
        int iconColor = isRunning ? R.color.blue : R.color.red;
        int buttonIcon = isRunning ? R.drawable.ic_close_black_24dp : R.drawable.ic_done_black_24dp;
        String buttonText = isRunning ? "STOP" : "START";

        serviceIcon.setImageResource(iconRes);
        serviceIcon.setColorFilter(ContextCompat.getColor(context, iconColor));
        serviceToggleButton.setIcon(ContextCompat.getDrawable(context, buttonIcon));
        serviceToggleButton.setText(buttonText);
    }

    private void toggleService() {
        Context context = getContext();
        if (context == null) return;

        if (NotificationsService.running) {
            // Stop the service
            getActivity().stopService(new Intent(getActivity(), NotificationsService.class));
            toggleServiceView(context, false);
        } else {
            // Clear the previous error message
            serviceMessageTextView.setText("");
            Log.e("test", Boolean.toString(NotificationsService.running));

            // Start the service
            getActivity().startService(new Intent(getActivity(), NotificationsService.class));
            toggleServiceView(context, true);
        }
    }

    private class ServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            serviceMessageTextView.setText("");
            String message = intent.getStringExtra("message");
            boolean isError = intent.getBooleanExtra("isError", false);

            if (!Objects.isNull(message))
                serviceMessageTextView.setText(Objects.requireNonNull(message, ""));

            if (isError) {
                toggleServiceView(context, false);
            }
        }
    }
}