package com.ecommerce.jewelleryMart.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.jewelleryMart.model.User;
import com.ecommerce.jewelleryMart.repository.UserRepository;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public void registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public boolean login(String email, String rawPassword) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.isPresent() &&
                passwordEncoder.matches(rawPassword, user.get().getPassword());
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> updateUsername(String email, String username) {
        Optional<User> user = userRepository.findByEmail(email);
        user.ifPresent(u -> {
            u.setUsername(username);
            userRepository.save(u);
        });
        return user;
    }

    public boolean resetPassword(String email, String newPassword) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) return false;

        user.get().setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user.get());
        return true;
    }
}
