package com.example.fraudcalldetection;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LiveCallAlertActivity extends AppCompatActivity {
    private TextView warningText, riskLevelText;
    private Button reportScamButton, blockNumberButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_call_alert);

        warningText = findViewById(R.id.warningText);
        riskLevelText = findViewById(R.id.riskLevelText);
        reportScamButton = findViewById(R.id.reportScamButton);
        blockNumberButton = findViewById(R.id.blockNumberButton);

        // Placeholder data (replace with actual data if triggered by call)
        riskLevelText.setText("Risk Level: HIGH");

        reportScamButton.setOnClickListener(v -> {
            // Add report scam logic here
        });

        blockNumberButton.setOnClickListener(v -> {
            // Add block number logic here
        });
    }
}