package com.voltpay.backend.service;

import com.voltpay.backend.model.User;
import com.voltpay.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public User verifyFirebaseTokenAndRegister(String firebaseIdToken, String expectedPhoneNumber, String name, String upiId, String fcmToken) {
        try {
            com.google.firebase.auth.FirebaseToken decodedToken = com.google.firebase.auth.FirebaseAuth.getInstance().verifyIdToken(firebaseIdToken);
            String tokenPhoneNumber = decodedToken.getClaims().get("phone_number").toString();
            
            // Allow slight format differences (e.g. +91 vs 091, but let's just ensure it contains the core number)
            if (tokenPhoneNumber == null) {
                throw new IllegalArgumentException("Firebase token does not contain a phone number");
            }
            
            Optional<User> optUser = userRepository.findByPhoneNumber(expectedPhoneNumber);
            User user = optUser.orElse(new User());
            user.setPhoneNumber(expectedPhoneNumber);
            
            user.setName(name);
            if (upiId != null) user.setUpiId(upiId);
            user.setFcmToken(fcmToken);
            
            // Generate our own session token
            String token = UUID.randomUUID().toString();
            user.setAuthToken(token);
            user.setTokenExpiry(Instant.now().plus(30, ChronoUnit.DAYS));
            
            return userRepository.save(user);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Firebase token: " + e.getMessage());
        }
    }

    public String refreshToken(String phoneNumber, String currentToken) {
        String strippedToken = currentToken.startsWith("Bearer ")
            ? currentToken.substring(7) : currentToken;

        Optional<User> optUser = userRepository.findByPhoneNumber(phoneNumber);
        if (optUser.isEmpty()) throw new IllegalArgumentException("User not found");

        User user = optUser.get();
        if (!strippedToken.equals(user.getAuthToken())) {
            throw new IllegalArgumentException("Invalid token");
        }

        // Issue new token and extend expiry
        String newToken = UUID.randomUUID().toString();
        user.setAuthToken(newToken);
        user.setTokenExpiry(Instant.now().plus(30, ChronoUnit.DAYS));
        userRepository.save(user);
        return newToken;
    }
    
    public void updateFcmToken(String phoneNumber, String fcmToken) {
        userRepository.findByPhoneNumber(phoneNumber).ifPresent(user -> {
            user.setFcmToken(fcmToken);
            userRepository.save(user);
        });
    }
    
    public Optional<User> getUserByPhone(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public boolean validateToken(String phoneNumber, String token) {
        if (token == null || token.isEmpty()) return false;
        // Strip "Bearer "
        if (token.startsWith("Bearer ")) token = token.substring(7);
        
        Optional<User> user = userRepository.findByPhoneNumber(phoneNumber);
        if (user.isPresent()) {
            User u = user.get();
            if (token.equals(u.getAuthToken()) && u.getTokenExpiry() != null && Instant.now().isBefore(u.getTokenExpiry())) {
                return true;
            }
        }
        return false;
    }
}
