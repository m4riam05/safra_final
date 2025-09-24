package com.safra.app.util;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.safra.app.R;
import com.safra.app.Safra;
import com.safra.app.api.NotificationAPI;
import com.safra.app.common.Constants;
import com.safra.app.config.Prefs;
import com.safra.app.model.ContactModel;
import com.safra.app.service.SosService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class SosUtil {

    private static String mLocation = "";
    private static boolean sentSMS = false;
    private static boolean sentNotification = false;
    private static boolean calledEmergency = false;
    private static AudioManager audioManager = null;
    private static LocationRequest locationRequest = null;
    private static LocationManager locationManager = null;
    private static NotificationAPI notificationApiService = null;
    private static final MediaPlayer mediaPlayer = new MediaPlayer();
    private static final String TAG = "SOS_DEBUG";
    private static final String SENT_SMS_ACTION = "com.safra.app.SMS_SENT";
    private static final String DELIVERED_SMS_ACTION = "com.safra.app.SMS_DELIVERED";
    private static final AtomicInteger requestCodeGenerator = new AtomicInteger(0);

    static {
        if (locationRequest == null) {
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(2000)
                    .setMaxUpdateDelayMillis(5000)
                    .build();
        }
        if (notificationApiService == null) {
            notificationApiService = NotificationClient.getClient("https://fcm.googleapis.com/").create(NotificationAPI.class);
        }
    }

    // ✅ This is the new method for the Safe Check-in button
    public static void sendSafeCheckInMessage(Context context) {
        Log.d(TAG, "--- Starting Safe Check-in Process ---");

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "SMS permission is required for this feature.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Safe Check-in failed: SEND_SMS permission not granted.");
            return;
        }

        ArrayList<ContactModel> contacts = getTrustedContacts();
        if (contacts.isEmpty()) {
            Toast.makeText(context, "You haven't added any trusted contacts yet.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Safe Check-in failed: No trusted contacts found.");
            return;
        }

        String userName = Prefs.getString(Constants.PREFS_USER_NAME, "a friend");
        String messageTemplate = context.getString(R.string.safe_check_in_message);

        int contactsSent = 0;
        for (ContactModel contact : contacts) {
            String message = String.format(messageTemplate, contact.getName(), userName);
            sendSmsWithPendingIntent(context, contact.getPhone(), message);
            contactsSent++;
        }

        if (contactsSent > 0) {
            Toast.makeText(context, "Sending safe check-in to " + contactsSent + " contact(s).", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Queued safe check-in for " + contactsSent + " contact(s).");
        }
    }


    public static void sendSignalLossMessage(Context context, Location location) {
        Log.i(TAG, "--- Starting Signal Loss SMS Process ---");
        ArrayList<ContactModel> contacts = getTrustedContacts();
        if (contacts.isEmpty()) {
            Log.e(TAG, "No trusted contacts found for signal loss message.");
            return;
        }

        String locationString = (location != null)
                ? "https://maps.google.com/maps?q=loc:" + location.getLatitude() + "," + location.getLongitude()
                : "Location not available";

        String messageTemplate = context.getString(R.string.signal_loss_message);
        for (ContactModel contact : contacts) {
            String message = String.format(messageTemplate, contact.getName(), locationString);
            sendSmsWithPendingIntent(context, contact.getPhone(), message);
        }
    }

    private static void sendSmsWithPendingIntent(Context context, String phoneNumber, String message) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SEND_SMS permission is not granted. Cannot send.");
            return;
        }
        try {
            int requestCode = requestCodeGenerator.incrementAndGet();

            Intent sentIntent = new Intent(SENT_SMS_ACTION);
            PendingIntent sentPI = PendingIntent.getBroadcast(context, requestCode, sentIntent, PendingIntent.FLAG_IMMUTABLE);

            Intent deliveredIntent = new Intent(DELIVERED_SMS_ACTION);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(context, requestCode, deliveredIntent, PendingIntent.FLAG_IMMUTABLE);

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
            Log.i(TAG, "SMS queued successfully for " + phoneNumber + " with unique request code " + requestCode);

        } catch (Exception e) {
            Log.e(TAG, "Exception while queuing SMS for " + phoneNumber, e);
        }
    }

    private static ArrayList<ContactModel> getTrustedContacts() {
        ArrayList<ContactModel> contacts = new ArrayList<>();
        String jsonContacts = Prefs.getString(Constants.CONTACTS_LIST, "");
        if (!jsonContacts.isEmpty()) {
            Gson gson = Safra.GSON;
            Type type = new TypeToken<List<ContactModel>>() {}.getType();
            contacts.addAll(gson.fromJson(jsonContacts, type));
        }
        return contacts;
    }

    public static void activateInstantSosMode(Context context) {
        if (mediaPlayer.isPlaying()) {
            stopSiren();
            resetValues();
            return;
        }

        resetValues();
        ArrayList<ContactModel> contacts = getTrustedContacts();

        if (Prefs.getBoolean(Constants.SETTINGS_CALL_EMERGENCY_SERVICE, false) && !calledEmergency) {
            callEmergency(context);
            calledEmergency = true;
        }

        if (!contacts.isEmpty()) {
            sendLocation(context, contacts);
        }

        if (Prefs.getBoolean(Constants.SETTINGS_PLAY_SIREN, false) && !mediaPlayer.isPlaying()) {
            playSiren(context);
        } else {
            stopSiren();
        }
    }

    private static void sendLocation(Context context, ArrayList<ContactModel> contacts) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.getFusedLocationProviderClient(context)
                .requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(context).removeLocationUpdates(this);
                        if (!locationResult.getLocations().isEmpty()) {
                            int idx = locationResult.getLocations().size() - 1;
                            Location lastLocation = locationResult.getLocations().get(idx);
                            mLocation = "https://maps.google.com/maps?q=loc:" + lastLocation.getLatitude() + "," + lastLocation.getLongitude();
                            if (Prefs.getBoolean(Constants.SETTINGS_SEND_SMS, true) && !sentSMS) {
                                sendInstantSms(context, contacts);
                                sentSMS = true;
                            }
                            if (Prefs.getBoolean(Constants.SETTINGS_SEND_NOTIFICATION, true) && !sentNotification) {
                                sendNotification(context, contacts);
                                sentNotification = true;
                            }
                        }
                    }
                }, Looper.getMainLooper());
    }

    private static void sendInstantSms(Context context, ArrayList<ContactModel> contacts) {
        for (ContactModel contact : contacts) {
            String message = context.getString(R.string.sos_message, contact.getName(), mLocation);
            sendSmsWithPendingIntent(context, contact.getPhone(), message);
        }
    }

    public static void sendNotification(Context context, ArrayList<ContactModel> contacts) {
        for (ContactModel contact : contacts) {
            FirebaseFirestore.getInstance()
                    .collection(Constants.FIRESTORE_COLLECTION_PHONE2UID)
                    .document(contact.getPhone())
                    .get()
                    .addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            DocumentSnapshot document1 = task1.getResult();
                            if (document1.exists() && document1.getString("uid") != null) {
                                FirebaseFirestore.getInstance()
                                        .collection(Constants.FIRESTORE_COLLECTION_TOKENS)
                                        .document(Objects.requireNonNull(document1.getString("uid")))
                                        .get()
                                        .addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                DocumentSnapshot document2 = task2.getResult();
                                                if (document2.exists() && document2.getString("token") != null) {
                                                    sendNotification(document2.getString("token"), Prefs.getString(Constants.PREFS_USER_NAME, context.getString(R.string.app_name)), context.getString(R.string.sos_notification, mLocation));
                                                }
                                            }
                                        });
                            }
                        }
                    });
        }
    }

    @SuppressWarnings("deprecation")
    public static void sendNotification(String userToken, String title, String message) {
        new FirebaseUtil.SendNotificationTask(notificationApiService, userToken, title, message).execute();
    }

    private static void callEmergency(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        context.startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Constants.EMERGENCY_NUMBER)));
    }

    private static void playSiren(Context context) {
        if (mediaPlayer.isPlaying()) return;
        if (audioManager == null) {
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("police-operation-siren.mp3");
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.setVolume(1f, 1f);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e("SOS", "playSiren error: " + e.getMessage(), e);
        }
    }

    public static void stopSiren() {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
        } catch (Exception ignored) {}
    }

    private static void resetValues() {
        sentSMS = false;
        sentNotification = false;
        calledEmergency = false;
    }

    public static boolean isGPSEnabled(Context context) {
        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static void turnOnGPS(Context context) {
        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(context)
                .checkLocationSettings(new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest)
                        .setAlwaysShow(true)
                        .build()
                );

        result.addOnCompleteListener(task -> {
            try {
                task.getResult(ApiException.class);
            } catch (ApiException apiException) {
                if (apiException.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        ResolvableApiException resolvableApiException = (ResolvableApiException) apiException;
                        resolvableApiException.startResolutionForResult((AppCompatActivity) context, 2);
                    } catch (IntentSender.SendIntentException sendIntentException) {
                        Log.i("SOS", "turnOnGPS: " + sendIntentException.getMessage());
                    }
                }
            }
        });
    }

    public static void startSosNotificationService(Context context) {
        if (!SosService.isRunning) {
            Intent notificationIntent = new Intent(context, SosService.class);
            notificationIntent.setAction("START");
            context.startForegroundService(notificationIntent);
        }
    }

    public static void stopSosNotificationService(Context context) {
        if (SosService.isRunning) {
            Intent notificationIntent = new Intent(context, SosService.class);
            notificationIntent.setAction("STOP");
            context.startForegroundService(notificationIntent);
        }
    }
}

