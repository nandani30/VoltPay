package com.voltpay.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount = null;
            if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") == null) {
                File localFile = new File("firebase-credentials.json");
                File renderSecretFile = new File("/etc/secrets/firebase-credentials.json");
                File renderSecretRoot = new File("/firebase-credentials.json");
                
                if (localFile.exists()) {
                    serviceAccount = new FileInputStream(localFile);
                } else if (renderSecretFile.exists()) {
                    serviceAccount = new FileInputStream(renderSecretFile);
                } else if (renderSecretRoot.exists()) {
                    serviceAccount = new FileInputStream(renderSecretRoot);
                }
            }

            GoogleCredentials credentials;
            if (serviceAccount != null) {
                credentials = GoogleCredentials.fromStream(serviceAccount);
            } else {
                credentials = GoogleCredentials.getApplicationDefault();
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Admin SDK initialized.");
            }
        } catch (IOException e) {
            System.err.println("Firebase credentials not found. Phone Auth and FCM disabled. Error: " + e.getMessage());
        }
    }
}
