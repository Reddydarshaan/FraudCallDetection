package com.example.fraudcalldetection;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.io.IOException;

public class CallRecorder {
    private MediaRecorder recorder;
    private boolean isRecording = false;
    private File audioFile;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int MAX_DURATION_MS = 30000; // 30 seconds
    private static final String TAG = "CallRecorder";
    private long startTime; // Track start time for precise logging

    public void startRecording(Context context, String phoneNumber) {
        if (isRecording) {
            Toast.makeText(context, "Already recording!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Already recording, skipping new request");
            return;
        }

        // Set storage directory to Music/audio
        File folder = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "audio");
        if (!folder.exists()) {
            folder.mkdirs();
            Log.d(TAG, "Created audio directory: " + folder.getAbsolutePath());
        }

        String fileName = "call_" + (phoneNumber != null ? phoneNumber : "unknown") + "_" + System.currentTimeMillis() + ".3gp";
        audioFile = new File(folder, fileName);

        recorder = new MediaRecorder();
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.setMaxDuration(MAX_DURATION_MS); // Enforce 30-second limit

            // Listener for max duration
            recorder.setOnInfoListener((mr, what, extra) -> {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    Log.d(TAG, "Max duration (30s) reached via OnInfoListener");
                    stopRecording(context);
                }
            });

            recorder.prepare();
            recorder.start();
            isRecording = true;
            startTime = System.currentTimeMillis(); // Record start time
            Log.d(TAG, "Recording started at " + startTime + "ms: " + audioFile.getAbsolutePath());
            Toast.makeText(context, "📞 Recording Started! (30s max)", Toast.LENGTH_SHORT).show();

            // Force stop after 30 seconds as backup
            handler.postDelayed(() -> {
                long elapsed = System.currentTimeMillis() - startTime;
                if (isRecording) {
                    Log.d(TAG, "Handler forcing stop after " + elapsed + "ms");
                    stopRecording(context);
                } else {
                    Log.d(TAG, "Handler triggered at " + elapsed + "ms but recording already stopped");
                }
            }, MAX_DURATION_MS);

        } catch (IOException e) {
            Log.e(TAG, "Error starting recording for " + audioFile.getAbsolutePath(), e);
            Toast.makeText(context, "❌ Error Starting Recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
            cleanupRecorder();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Recorder state error: " + e.getMessage(), e);
            Toast.makeText(context, "❌ Recorder Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            cleanupRecorder();
        }
    }

    public void stopRecording(Context context) {
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
                    Log.d(TAG, "Converted to MP3 after " + elapsed + "ms: " + audioFile.getAbsolutePath());
                    Toast.makeText(context, "✅ Recording Saved: " + audioFile.getName(), Toast.LENGTH_LONG).show();
                } else {
                    throw new Exception("MP3 file not found after conversion");
                }
            } else {
                throw new Exception("FFmpeg failed: " + session.getFailStackTrace());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording or converting to MP3 after " + elapsed + "ms: " + e.getMessage(), e);
            Toast.makeText(context, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

            // Fallback: Keep 3GP if MP3 fails
            if (audioFile.exists() && audioFile.getName().endsWith(".3gp")) {
                Log.d(TAG, "Fallback: Keeping 3GP file: " + audioFile.getAbsolutePath());
                Toast.makeText(context, "✅ Saved 3GP (MP3 failed): " + audioFile.getName(), Toast.LENGTH_LONG).show();
            }
        } finally {
            cleanupRecorder();
            handler.removeCallbacksAndMessages(null);
            Log.d(TAG, "Recorder cleaned up after " + elapsed + "ms");
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
    }

    public String getAudioFilePath() {
        return audioFile != null ? audioFile.getAbsolutePath() : null;
    }

    public boolean isRecording() {
        return isRecording;
    }
}