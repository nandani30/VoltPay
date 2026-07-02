package com.voltpay.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class FcmService {

    public void sendPushNotification(String targetToken, String title, String body) {
        if (targetToken == null || targetToken.isEmpty()) return;
        
        try {
            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            System.err.println("Failed to send FCM message: " + e.getMessage());
        }
    }

    public void sendDataNotification(String targetToken, String title, String body, java.util.Map<String, String> data) {
        if (targetToken == null || targetToken.isEmpty()) return;
        
        try {
            Message message = Message.builder()
                    .setToken(targetToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .build();

            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            System.err.println("Failed to send FCM data message: " + e.getMessage());
        }
    }
}
