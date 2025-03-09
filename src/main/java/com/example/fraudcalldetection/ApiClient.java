package com.example.fraudcalldetection;

import java.io.File;
import java.io.IOException;
import okhttp3.*;

public class ApiClient {
    private static final String BASE_URL = "http://172.16.45.151:5000"; // Replace with your server IP
    private static final OkHttpClient client = new OkHttpClient();

    public static void sendAudioFile(File audioFile, Callback callback) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(),
                        RequestBody.create(audioFile, MediaType.parse("audio/mpeg")))
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/analyze_audio")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(callback);
    }
}