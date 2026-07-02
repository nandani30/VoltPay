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
    
    public void requestOtp(String phoneNumber) {
        Optional<User> existing = userRepository.findByPhoneNumber(phoneNumber);
        User user = existing.orElse(new User());
        user.setPhoneNumber(phoneNumber);
        
        // Generate a random 6 digit OTP
        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(999999));
        user.setCurrentOtp(otp);
        user.setOtpExpiry(Instant.now().plus(5, ChronoUnit.MINUTES));
        
        userRepository.save(user);
        
        // In a real app, send OTP via SMS here.
        System.out.println("LOCAL DEV ONLY — OTP for " + phoneNumber + " is " + otp);
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

    public User verifyOtpAndRegister(String phoneNumber, String otp, String name, String upiId, String fcmToken) {
        Optional<User> optUser = userRepository.findByPhoneNumber(phoneNumber);
        if (optUser.isEmpty()) throw new IllegalArgumentException("User not found");
        
        User user = optUser.get();
        if (user.getCurrentOtp() == null || !user.getCurrentOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP");
        }
        if (Instant.now().isAfter(user.getOtpExpiry())) {
            throw new IllegalArgumentException("OTP expired");
        }
        
        // Success
        user.setCurrentOtp(null);
        user.setOtpExpiry(null);
        user.setName(name);
        user.setUpiId(upiId);
        user.setFcmToken(fcmToken);
        
        // Generate token
        String token = UUID.randomUUID().toString();
        user.setAuthToken(token);
        user.setTokenExpiry(Instant.now().plus(30, ChronoUnit.DAYS));
        
        return userRepository.save(user);
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
