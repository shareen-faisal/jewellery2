package com.ecommerce.jewelleryMart.controller;

import com.ecommerce.jewelleryMart.model.User;
import com.ecommerce.jewelleryMart.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    // -------- SIGNUP --------
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody User user) {
        Map<String, String> response = new HashMap<>();

        if (authService.emailExists(user.getEmail())) {
            response.put("error", "Email already registered.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        authService.registerUser(user);
        response.put("message", "User registered successfully!");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------- LOGIN --------
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody User loginRequest) {
        Map<String, String> response = new HashMap<>();

        if (authService.login(loginRequest.getEmail(), loginRequest.getPassword())) {
            response.put("message", "Login successful!");
            return ResponseEntity.ok(response);
        }

        response.put("error", "Invalid email or password.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // -------- GET USER --------
    @GetMapping("/user")
    public ResponseEntity<?> getUserByEmail(@RequestParam String email) {
        Optional<User> user = authService.getUserByEmail(email);

        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }

        Map<String, String> response = new HashMap<>();
        response.put("error", "User not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // -------- UPDATE USERNAME --------
    @PutMapping("/user")
    public ResponseEntity<?> updateUser(@RequestParam String email,
                                        @RequestBody User updatedData) {

        Optional<User> updatedUser =
                authService.updateUsername(email, updatedData.getUsername());

        if (updatedUser.isPresent()) {
            return ResponseEntity.ok(updatedUser.get());
        }

        Map<String, String> response = new HashMap<>();
        response.put("error", "User not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // -------- RESET PASSWORD --------
    @PutMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody Map<String, String> requestBody) {

        Map<String, String> response = new HashMap<>();
        String email = requestBody.get("email");
        String newPassword = requestBody.get("newPassword");

        if (email == null || newPassword == null) {
            response.put("error", "Email and new password are required.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (!authService.resetPassword(email, newPassword)) {
            response.put("error", "User not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        response.put("message", "Password updated successfully.");
        return ResponseEntity.ok(response);
    }
}
