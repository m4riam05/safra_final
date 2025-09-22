package com.safra.app.service.signalloss;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

public class SmsSentReceiver extends BroadcastReceiver {
    private static final String TAG = "SMS_DEBUG";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.startsWith("com.safra.app.SMS_SENT_")) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Log.i(TAG, "SMS Sent broadcast received: RESULT_OK. Message sent successfully.");
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Log.e(TAG, "SMS Sent broadcast received: GENERIC_FAILURE.");
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Log.e(TAG, "SMS Sent broadcast received: NO_SERVICE. Queued for later.");
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    Log.e(TAG, "SMS Sent broadcast received: NULL_PDU.");
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Log.e(TAG, "SMS Sent broadcast received: RADIO_OFF (Airplane Mode). Queued for later.");
                    break;
                default:
                    Log.w(TAG, "SMS Sent broadcast received with unknown result code: " + getResultCode());
                    break;
            }
        }
    }
}

