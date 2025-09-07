package com.safra.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.safra.app.util.SosUtil;
import com.safra.app.R;

import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineManager;

public class WakeWordService extends Service {

    private static final String CHANNEL_ID = "WakeWordServiceChannel";
    private PorcupineManager porcupineManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getServiceNotification());

        try {
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey("tZuE/VdmUYTDyPlKuB2fPjZqfWBqzsh/XwN64NNu9/gS/nv/5CzqvQ==") // 👈 Replace with your real AccessKey
                    .setKeywordPath("safra-help_en_android_v3_0_0.ppn") // 👈 your assets file
                    .setSensitivity(0.7f)
                    .build(getApplicationContext(), (keywordIndex) -> {
                        try {
                            // ✅ Trigger detected
                            sendAlertMessage();
                            showNotification("Alert sent. Don’t worry.");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

            porcupineManager.start();
        } catch (PorcupineException e) {
            e.printStackTrace();
        }
    }

    private void sendAlertMessage() {
        SosUtil.activateInstantSosMode(this);
        // sends location + "I am in danger"
    }

    private void showNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Safra Alert")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(2, builder.build());
    }

    private Notification getServiceNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Wake Word Service")
                .setContentText("Listening for 'safra help'...")
                .setSmallIcon(R.drawable.ic_notification)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Wake Word Detection Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // keeps service alive
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                porcupineManager.delete();
            } catch (PorcupineException e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
