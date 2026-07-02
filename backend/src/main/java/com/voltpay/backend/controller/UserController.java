package com.voltpay.backend.controller;

import com.voltpay.backend.model.User;
import com.voltpay.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/verify-firebase-token")
    public ResponseEntity<?> verifyFirebaseTokenAndRegister(@RequestBody RegisterDto dto) {
        try {
            User user = userService.verifyFirebaseTokenAndRegister(dto.getFirebaseIdToken(), dto.getPhoneNumber(), dto.getName(), dto.getUpiId(), dto.getFcmToken());
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "userId", user.getId(),
                "authToken", user.getAuthToken()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/fcm-token")
    public ResponseEntity<?> updateFcmToken(@RequestHeader("Authorization") String token, @RequestBody FcmTokenDto dto) {
        if (!userService.validateToken(dto.getPhoneNumber(), token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        userService.updateFcmToken(dto.getPhoneNumber(), dto.getFcmToken());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String token,
                                          @RequestBody PhoneDto dto) {
        try {
            String newToken = userService.refreshToken(dto.getPhoneNumber(), token);
            return ResponseEntity.ok(Map.of("authToken", newToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    static class PhoneDto {
        private String phoneNumber;
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    static class RegisterDto {
        private String phoneNumber;
        private String firebaseIdToken;
        private String name;
        private String upiId;
        private String fcmToken;
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public String getFirebaseIdToken() { return firebaseIdToken; }
        public void setFirebaseIdToken(String firebaseIdToken) { this.firebaseIdToken = firebaseIdToken; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getUpiId() { return upiId; }
        public void setUpiId(String upiId) { this.upiId = upiId; }
        
        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    }

    static class FcmTokenDto {
        private String phoneNumber;
        private String fcmToken;
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        
        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    }
}
