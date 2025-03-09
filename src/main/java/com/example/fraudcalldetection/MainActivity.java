package com.example.fraudcalldetection;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1;
    private BroadcastReceiver phoneStateReceiver;
    private TextView filePathText;
    private Button startButton, stopButton, analysisButton;
    private CallRecorder callRecorder;
    private String lastRecordedFilePath; // Store the last recorded file path

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        filePathText = findViewById(R.id.filePathText);
        startButton = findViewById(R.id.startRecordingButton);
        stopButton = findViewById(R.id.stopRecordingButton);
        analysisButton = findViewById(R.id.openAnalysisButton);

        callRecorder = new CallRecorder();

        String storagePath = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "audio").getAbsolutePath();
        filePathText.setText("Recordings will be saved to: " + storagePath);

        startButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                callRecorder.startRecording(this, "manual");
                updateUI(true);
                Log.d(TAG, "Manual recording started");
            } else {
                requestPermissions();
            }
        });

        stopButton.setOnClickListener(v -> {
            callRecorder.stopRecording(this);
            updateUI(false);
            lastRecordedFilePath = callRecorder.getAudioFilePath();
            if (lastRecordedFilePath != null && new File(lastRecordedFilePath).exists()) {
                filePathText.setText("Last Recording: " + lastRecordedFilePath);
                Log.d(TAG, "Recording stopped, file saved at: " + lastRecordedFilePath);
            } else {
                Log.e(TAG, "No valid file path after recording");
                Toast.makeText(this, "Recording failed to save", Toast.LENGTH_SHORT).show();
            }
        });

        analysisButton.setOnClickListener(v -> analyzeAudio());

        requestPermissions();
    }

    private void updateUI(boolean isRecording) {
        startButton.setVisibility(isRecording ? View.GONE : View.VISIBLE);
        stopButton.setVisibility(isRecording ? View.VISIBLE : View.GONE);
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            registerPhoneStateReceiver();
        }
    }

    private boolean checkPermissions() {
        String[] permissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            registerPhoneStateReceiver();
            Toast.makeText(this, "✅ Permissions granted!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "❌ Permissions denied, app may not function", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Permissions not granted");
        }
    }

    private void registerPhoneStateReceiver() {
        phoneStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("android.intent.action.PHONE_STATE")) {
                    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                        callRecorder.startRecording(context, phoneNumber != null ? phoneNumber : "unknown");
                        updateUI(true);
                        Log.d(TAG, "Call recording started for: " + (phoneNumber != null ? phoneNumber : "unknown"));
                    } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                        callRecorder.stopRecording(context);
                        updateUI(false);
                        lastRecordedFilePath = callRecorder.getAudioFilePath();
                        if (lastRecordedFilePath != null) {
                            filePathText.setText("Last Recording: " + lastRecordedFilePath);
                            Log.d(TAG, "Call recording stopped, file saved at: " + lastRecordedFilePath);
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter("android.intent.action.PHONE_STATE");
        registerReceiver(phoneStateReceiver, filter);
    }

    private void analyzeAudio() {
        if (lastRecordedFilePath == null || !new File(lastRecordedFilePath).exists()) {
            Log.e(TAG, "No valid recording file to analyze: " + lastRecordedFilePath);
            Toast.makeText(this, "Please record a call first", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(lastRecordedFilePath);
        Log.d(TAG, "Sending file for analysis: " + file.getAbsolutePath());
        ApiClient.sendAudioFile(file, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API failure: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result = response.body().string();
                    Log.d(TAG, "Analysis result: " + result); // Log full JSON response
                    Intent intent = new Intent(MainActivity.this, FraudResultActivity.class);
                    intent.putExtra("result", result);
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Navigation error: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to open results: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No body";
                    Log.e(TAG, "Server error: " + response.code() + " - " + response.message() + " - " + errorBody);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Server error: " + response.message(), Toast.LENGTH_LONG).show());
                }
                response.close(); // Ensure response body is closed
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (phoneStateReceiver != null) {
            unregisterReceiver(phoneStateReceiver);
        }
        if (callRecorder != null) {
            callRecorder.stopRecording(this);
        }
    }
}