package com.safra.app.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.auth.FirebaseAuth;
import com.safra.app.R;
import com.safra.app.databinding.ActivityMainBinding;
import com.safra.app.service.signalloss.SignalMonitorService; // ✅ Import the new service
import com.safra.app.service.WakeWordService;
import com.safra.app.util.ObservableVariable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class MainActivity extends AppCompatActivity {

    public static ObservableVariable<Boolean> shakeDetection = new ObservableVariable<>();
    private ActivityMainBinding binding;

    private static final int PERMISSIONS_REQUEST_CODE = 101;
    private static final int BACKGROUND_LOCATION_REQUEST_CODE = 102;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkAndRequestInitialPermissions();
    }

    private void checkAndRequestInitialPermissions() {
        String[] requiredPermissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        } else {
            onInitialPermissionsGranted();
        }
    }

    private void onInitialPermissionsGranted() {
        Log.d(TAG, "Initial permissions granted. Checking for background location.");
        checkBackgroundLocationPermission();
    }

    private void checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setTitle("Background Location Required")
                        .setMessage("For the signal loss feature to work correctly when the app is closed, Safra needs to access your location in the background.")
                        .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_REQUEST_CODE))
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            Toast.makeText(this, "Background location is needed for the signal loss feature.", Toast.LENGTH_LONG).show();
                            onBackgroundPermissionHandled();
                        })
                        .show();
            } else {
                onBackgroundPermissionHandled();
            }
        } else {
            onBackgroundPermissionHandled();
        }
    }

    private void onBackgroundPermissionHandled() {
        Log.d(TAG, "Background location handled. Checking battery optimization.");
        checkBatteryOptimization();
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                        .setTitle("Disable Battery Optimization")
                        .setMessage("To ensure Safra can protect you at all times, please allow the app to run in the background without battery restrictions.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                            startBackgroundServices(); // Start services after user interacts
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            Toast.makeText(this, "Services may be stopped by the OS to save battery.", Toast.LENGTH_LONG).show();
                            startBackgroundServices(); // Still start services
                        })
                        .show();
            } else {
                startBackgroundServices();
            }
        } else {
            startBackgroundServices();
        }
    }

    // ✅ This single method now starts ALL required background services.
    private void startBackgroundServices() {
        // Start the voice command service
        Intent wakeWordIntent = new Intent(this, WakeWordService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(wakeWordIntent);
        } else {
            startService(wakeWordIntent);
        }

        // Start the new, persistent signal monitoring service
        Intent signalMonitorIntent = new Intent(this, SignalMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(signalMonitorIntent);
        } else {
            startService(signalMonitorIntent);
        }

        Log.i(TAG, "All background services have been started.");
        Toast.makeText(this, "Safra background services are running.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                onInitialPermissionsGranted();
            } else {
                Toast.makeText(this, "All permissions are required for Safra's core features.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == BACKGROUND_LOCATION_REQUEST_CODE) {
            onBackgroundPermissionHandled();
        }
    }

    // ... your other methods (toggleDrawer, onSupportNavigateUp, onStart) remain unchanged ...
    public void toggleDrawer() {
        if (binding.drawerLayout.isDrawerOpen(binding.navView)) {
            binding.drawerLayout.closeDrawer(binding.navView);
        } else {
            binding.drawerLayout.openDrawer(binding.navView);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.fragmentContainerView);
        return NavigationUI.navigateUp(navController, binding.drawerLayout) || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null || !firebaseAuth.getCurrentUser().isEmailVerified()) {
            startActivity(new Intent(MainActivity.this, SplashActivity.class));
            finishAffinity();
        }
    }
}

