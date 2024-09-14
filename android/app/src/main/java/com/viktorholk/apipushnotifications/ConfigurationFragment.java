package com.viktorholk.apipushnotifications;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

        EditText urlEditText = view.findViewById(R.id.urlEditText);
        Button applyButton = view.findViewById(R.id.applyButton);

        String url = Shared.getString(getActivity(), "url", "");
        if (!url.isEmpty()) {
            urlEditText.setText(url);
        }

        applyButton.setOnClickListener(v -> applyConfiguration(urlEditText.getText().toString()));
    }

    private void applyConfiguration(String newUrl) {
        if (newUrl.length() == 0)
            return;

        String url = Utils.formatURL(newUrl);

        Shared.saveData(getActivity(), "url", url);

        navigateToServiceFragment();
    }

    private void navigateToServiceFragment() {
        MainActivity.fragmentManager.beginTransaction()
                .replace(R.id.fragmentView, ServiceFragment.class, null)
                .commit();

        MainActivity.bottomNavigationView.setSelectedItemId(R.id.service);
    }
}
