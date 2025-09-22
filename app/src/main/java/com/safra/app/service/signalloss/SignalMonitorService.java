package com.safra.app.service.signalloss;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.safra.app.R;
import com.safra.app.util.SosUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SignalMonitorService extends Service {

    private static final String TAG = "SignalMonitorService";
    private static final String CHANNEL_ID = "SignalMonitorChannel";
    private TelephonyManager telephonyManager;
    private boolean isAlertSent = false;
    private static final int ALERT_NOTIFICATION_ID = 7;

    // ✅ These are the two different listeners for signal state.
    private PhoneStateListener phoneStateListener; // For older Android versions
    private SignalCallback signalCallback; // For modern Android versions

    // The modern TelephonyCallback for Android 12 (API 31) and newer
    @RequiresApi(api = Build.VERSION_CODES.S)
    private class SignalCallback extends TelephonyCallback implements TelephonyCallback.ServiceStateListener {
        @Override
        public void onServiceStateChanged(@NonNull ServiceState serviceState) {
            handleServiceStateChange(serviceState);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        // ✅ Initialize the correct listener based on the device's Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            signalCallback = new SignalCallback();
        } else {
            // Use the older PhoneStateListener for devices below Android 12
            Executor executor = Executors.newSingleThreadExecutor();
            phoneStateListener = new PhoneStateListener(executor) {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    super.onServiceStateChanged(serviceState);
                    handleServiceStateChange(serviceState);
                }
            };
        }
    }

    // ✅ This central method contains the logic and is called by BOTH listeners
    private void handleServiceStateChange(ServiceState serviceState) {
        if (serviceState.getState() == ServiceState.STATE_OUT_OF_SERVICE ||
                serviceState.getState() == ServiceState.STATE_POWER_OFF) {
            if (!isAlertSent) {
                Log.w(TAG, "Signal lost. Preparing to send alert.");
                sendSignalLossSms();
                isAlertSent = true;
            }
        } else {
            if (isAlertSent) {
                Log.i(TAG, "Signal restored. Resetting alert flag.");
                isAlertSent = false;
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Safra Protection")
                .setContentText("Actively monitoring for signal loss.")
                .setSmallIcon(R.drawable.ic_launcher_notification)
                .build();

        startForeground(6, notification);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // ✅ Register the appropriate listener based on the Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager.registerTelephonyCallback(getMainExecutor(), signalCallback);
            } else {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
            }
            Log.i(TAG, "Signal monitor service started and is now listening.");
        } else {
            Log.e(TAG, "Cannot start listener, READ_PHONE_STATE permission not granted.");
            stopSelf();
        }

        return START_STICKY;
    }

    private void sendSignalLossSms() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = null;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation == null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to get location.", e);
            }
        } else {
            Log.w(TAG, "Location permission not granted. Sending alert without location.");
        }

        SosUtil.sendSignalLossMessage(this, lastKnownLocation);
        sendUserAlertNotification("Signal lost. An alert SMS has been sent to your trusted contacts.");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Safra Background Monitoring & Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            serviceChannel.setDescription("Shows a persistent notification that Safra is running and displays high-priority alerts.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void sendUserAlertNotification(String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show alert notification.");
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_notification)
                .setContentTitle("Safra Alert Sent")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(ALERT_NOTIFICATION_ID, builder.build());
        Log.i(TAG, "User alert notification has been sent.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // ✅ Unregister the appropriate listener based on the Android version
        if (telephonyManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && signalCallback != null) {
                telephonyManager.unregisterTelephonyCallback(signalCallback);
            } else if (phoneStateListener != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
        }
        Log.i(TAG, "Signal monitor service destroyed.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

