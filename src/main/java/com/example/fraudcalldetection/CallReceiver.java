package com.example.fraudcalldetection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null ||
                !intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            return;
        }

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        Intent serviceIntent = new Intent(context, CallRecordingService.class);
        if (phoneNumber != null) {
            serviceIntent.putExtra("phoneNumber", phoneNumber);
        } else {
            Log.d(TAG, "Phone number unavailable, using 'unknown'");
            phoneNumber = "unknown";
        }

        if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            Log.d(TAG, "Call answered - Starting recording for " + phoneNumber);
            context.startService(serviceIntent);
        }
        // Do NOT stop the service on EXTRA_STATE_IDLE - let the service handle 30s limit
    }
}