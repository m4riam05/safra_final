package com.safra.app.service.signalloss;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SmsDeliveredReceiver extends BroadcastReceiver {
    private static final String TAG = "SMS_DEBUG";

    @Override
    public void onReceive(Context context, Intent intent) {
        // This is where you could check the result code, but for now, just logging is enough.
        Log.i(TAG, "SMS delivery report received.");
    }
}
