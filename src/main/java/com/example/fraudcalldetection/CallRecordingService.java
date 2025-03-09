package com.example.fraudcalldetection;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.io.IOException;

public class CallRecordingService extends Service {
    private MediaRecorder recorder;
    private boolean isRecording = false;
    private File audioFile;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int MAX_DURATION_MS = 30000; // 30 seconds
    private static final String TAG = "CallRecordingService";
    private long startTime;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String phoneNumber = intent.getStringExtra("phoneNumber");
        startRecording(phoneNumber != null ? phoneNumber : "unknown");
        return START_NOT_STICKY;
    }

    private void startRecording(String phoneNumber) {
        if (isRecording) {
            Log.d(TAG, "Already recording, skipping new request");
            return;
        }

        // Set storage directory to Music/audio
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "audio");
        if (!directory.exists()) {
            directory.mkdirs();
            Log.d(TAG, "Created audio directory: " + directory.getAbsolutePath());
        }

        String fileName = "call_" + phoneNumber + "_" + System.currentTimeMillis() + ".3gp";
        audioFile = new File(directory, fileName);

        recorder = new MediaRecorder();
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.setMaxDuration(MAX_DURATION_MS); // Enforce 30 seconds

            // Stop when max duration is reached
            recorder.setOnInfoListener((mr, what, extra) -> {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    Log.d(TAG, "Max duration (30s) reached via OnInfoListener");
                    stopRecording();
                }
            });

            recorder.prepare();
            recorder.start();
            isRecording = true;
            startTime = System.currentTimeMillis();
            Log.d(TAG, "Recording started at " + startTime + "ms: " + audioFile.getAbsolutePath());
            Toast.makeText(this, "📞 Recording Started (30s max)", Toast.LENGTH_SHORT).show();

            // Backup: Force stop after 30 seconds
            handler.postDelayed(() -> {
                long elapsed = System.currentTimeMillis() - startTime;
                if (isRecording) {
                    Log.d(TAG, "Handler forcing stop after " + elapsed + "ms");
                    stopRecording();
                }
            }, MAX_DURATION_MS);

        } catch (IOException e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage(), e);
            Toast.makeText(this, "❌ Error Starting Recording", Toast.LENGTH_LONG).show();
            cleanupRecorder();
            stopSelf();
        }
    }

    private void stopRecording() {
        if (recorder == null || !isRecording) {
            Log.d(TAG, "No active recording to stop");
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        try {
            recorder.stop();
            isRecording = false;
            Log.d(TAG, "Recording stopped after " + elapsed + "ms");

            // Convert 3GP to MP3
            File mp3File = new File(audioFile.getParent(), audioFile.getName().replace(".3gp", ".mp3"));
            String command = "-i " + audioFile.getAbsolutePath() + " -c:a mp3 -y " + mp3File.getAbsolutePath();
            Log.d(TAG, "Executing FFmpeg command: " + command);

            FFmpegSession session = FFmpegKit.execute(command);
            if (ReturnCode.isSuccess(session.getReturnCode())) {
                if (mp3File.exists()) {
                    audioFile.delete(); // Delete original 3GP
                    audioFile = mp3File;
                    Log.d(TAG, "Converted to MP3: " + audioFile.getAbsolutePath());
                } else {
                    Log.e(TAG, "MP3 file not created after conversion");
                }
            } else {
                Log.e(TAG, "FFmpeg failed: " + session.getFailStackTrace());
            }

            // Ensure file is saved (MP3 or 3GP fallback)
            if (audioFile.exists()) {
                Log.d(TAG, "Recording saved: " + audioFile.getAbsolutePath());
                Toast.makeText(this, "✅ Recording Saved: " + audioFile.getName(), Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "No file saved after recording");
                Toast.makeText(this, "❌ No Recording Saved", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording or converting: " + e.getMessage(), e);
            Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

            // Fallback: Keep 3GP if MP3 fails
            if (audioFile.exists() && audioFile.getName().endsWith(".3gp")) {
                Log.d(TAG, "Fallback: Keeping 3GP file: " + audioFile.getAbsolutePath());
                Toast.makeText(this, "✅ Saved 3GP (MP3 failed): " + audioFile.getName(), Toast.LENGTH_LONG).show();
            }
        } finally {
            cleanupRecorder();
            stopSelf();
        }
    }

    private void cleanupRecorder() {
        if (recorder != null) {
            try {
                recorder.reset();
                recorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing recorder: " + e.getMessage());
            }
            recorder = null;
        }
        isRecording = false;
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Recorder cleaned up");
    }

    @Override
    public void onDestroy() {
        if (isRecording) {
            stopRecording();
        }
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }
}