package com.example.fraudcalldetection;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CallAnalysisActivity extends AppCompatActivity {
    private TextView analysisTitle, riskLevelText, transcriptionText, deepfakeResultText;
    private Button reportScamButton, blockNumberButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_analysis);

        analysisTitle = findViewById(R.id.analysisTitle);
        riskLevelText = findViewById(R.id.riskLevelText);
        transcriptionText = findViewById(R.id.transcriptionText);
        deepfakeResultText = findViewById(R.id.deepfakeResultText);
        reportScamButton = findViewById(R.id.reportScamButton);
        blockNumberButton = findViewById(R.id.blockNumberButton);

        // Placeholder data (replace with actual data from server if needed)
        riskLevelText.setText("Risk Level: Medium");
        transcriptionText.setText("Speech-to-Text Summary: Call content here...");
        deepfakeResultText.setText("Deepfake Detection: Real");

        reportScamButton.setOnClickListener(v -> {
            // Add report scam logic here
        });

        blockNumberButton.setOnClickListener(v -> {
            // Add block number logic here
        });
    }
}