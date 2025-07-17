package com.example.miniproj;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.FileInputStream;
import java.io.IOException;

public class FirebaseInitializer {
    public static void initialize() throws IOException {
        FileInputStream serviceAccount =
                new FileInputStream("src/main/resources/firebase-key.json");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://aaa2040-c4b67.firebaseio.com")
                .build();

        FirebaseApp.initializeApp(options);
    }
}
