package com.safra.app.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.safra.app.R;
import com.safra.app.common.Constants;
import com.safra.app.config.Prefs;
import com.safra.app.databinding.FragmentSettingsBinding;
import com.safra.app.service.SosService;
import com.safra.app.ui.activity.MainActivity;
import com.safra.app.util.SosUtil;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.header.toolbar);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            binding.header.collapsingToolbar.setTitle(getString(R.string.activity_settings_title));
            binding.header.collapsingToolbar.setSubtitle(getString(R.string.activity_settings_desc));
        }

        binding.switchShakeDetection.setChecked(Prefs.getBoolean(Constants.SETTINGS_SHAKE_DETECTION, false));
        binding.switchShakeDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putBoolean(Constants.SETTINGS_SHAKE_DETECTION, isChecked);
            MainActivity.shakeDetection.setValue(isChecked);
        });
        binding.shakeDetectionContainer.setOnClickListener(v -> binding.switchShakeDetection.toggle());

        binding.switchSendSms.setChecked(Prefs.getBoolean(Constants.SETTINGS_SEND_SMS, true));
        binding.switchSendSms.setOnCheckedChangeListener((buttonView, isChecked) -> Prefs.putBoolean(Constants.SETTINGS_SEND_SMS, isChecked));
        binding.sendSmsContainer.setOnClickListener(v -> binding.switchSendSms.toggle());

        binding.switchSendNotification.setChecked(Prefs.getBoolean(Constants.SETTINGS_SEND_NOTIFICATION, true));
        binding.switchSendNotification.setOnCheckedChangeListener((buttonView, isChecked) -> Prefs.putBoolean(Constants.SETTINGS_SEND_NOTIFICATION, isChecked));
        binding.sendNotificationContainer.setOnClickListener(v -> binding.switchSendNotification.toggle());

        binding.switchPlaySiren.setChecked(Prefs.getBoolean(Constants.SETTINGS_PLAY_SIREN, false));
        binding.switchPlaySiren.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Prefs.putBoolean(Constants.SETTINGS_PLAY_SIREN, isChecked);
            if (!isChecked) {
                SosService.stopSiren();
                SosUtil.stopSiren();
            }
        });
        binding.playSirenContainer.setOnClickListener(v -> binding.switchPlaySiren.toggle());

        binding.switchCallEmergencyService.setChecked(Prefs.getBoolean(Constants.SETTINGS_CALL_EMERGENCY_SERVICE, false));
        binding.switchCallEmergencyService.setOnCheckedChangeListener((buttonView, isChecked) -> Prefs.putBoolean(Constants.SETTINGS_CALL_EMERGENCY_SERVICE, isChecked));
        binding.callEmergencyServiceContainer.setOnClickListener(v -> binding.switchCallEmergencyService.toggle());

        return view;
    }
}