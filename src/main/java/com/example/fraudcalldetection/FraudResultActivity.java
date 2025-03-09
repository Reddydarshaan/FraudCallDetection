package com.example.fraudcalldetection;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class FraudResultActivity extends AppCompatActivity {
    private static final String TAG = "FraudResultActivity";
    private TextView filePathText, riskLevelText, transcriptionText, deepfakeResultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fraud_result);

        filePathText = findViewById(R.id.filePathText);
        riskLevelText = findViewById(R.id.riskLevelText);
        transcriptionText = findViewById(R.id.transcriptionText);
        deepfakeResultText = findViewById(R.id.deepfakeResultText);

        // Get the file path and result from the Intent
        String filePath = getIntent().getStringExtra("FILE_PATH");
        String result = getIntent().getStringExtra("result");

        // Set the recording file path
        filePathText.setText("Recording: " + (filePath != null ? filePath : "None"));

        // Parse the server response
        if (result != null) {
            try {
                JSONObject jsonResponse = new JSONObject(result);
                String deepfakeResult = jsonResponse.getString("result");

                // Set Deepfake Analysis
                deepfakeResultText.setText("Deepfake Analysis: " + deepfakeResult);

                // Derive Risk Level based on deepfake result
                String riskLevel = deepfakeResult.equals("Deepfake") ? "High" : "Low";
                riskLevelText.setText("Risk Level: " + riskLevel);

                // Transcription is not implemented
                transcriptionText.setText("Transcription: Not implemented");

                Log.d(TAG, "Parsed result: " + deepfakeResult);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse JSON: " + e.getMessage());
                riskLevelText.setText("Risk Level: Error");
                transcriptionText.setText("Transcription: Error");
                deepfakeResultText.setText("Deepfake Analysis: Error");
            }
        } else {
            Log.e(TAG, "No result received from intent");
            riskLevelText.setText("Risk Level: Not available");
            transcriptionText.setText("Transcription: Not available");
            deepfakeResultText.setText("Deepfake Analysis: Not available");
        }
    }
}