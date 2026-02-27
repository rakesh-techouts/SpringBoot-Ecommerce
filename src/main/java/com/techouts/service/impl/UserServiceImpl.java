package com.techouts.service.impl;

import com.techouts.entity.Cart;
import com.techouts.entity.User;
import com.techouts.repository.CartRepository;
import com.techouts.repository.UserRepository;
import com.techouts.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, CartRepository cartRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public User register(String name, String email, String phone, String rawPassword) {
        String normalizedName = normalizeName(name);
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phone);
        validatePassword(rawPassword);

        userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            throw new IllegalArgumentException("Email already exists");
        });
        userRepository.findByPhone(normalizedPhone).ifPresent(existing -> {
            throw new IllegalArgumentException("Phone number already exists");
        });

        User user = new User();
        user.setName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        user.setPassword(passwordEncoder.encode(rawPassword));
        User saved = userRepository.save(user);

        Cart cart = new Cart();
        cart.setUser(saved);
        cartRepository.save(cart);
        return saved;
    }

    @Override
    public Optional<User> login(String emailOrPhone, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return Optional.empty();
        }

        String identifier = emailOrPhone == null ? "" : emailOrPhone.trim();
        if (identifier.isBlank()) {
            return Optional.empty();
        }

        Optional<User> userOptional = identifier.contains("@")
                ? userRepository.findByEmail(normalizeEmail(identifier))
                : userRepository.findByPhone(normalizePhone(identifier));

        return userOptional
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()));
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    @Transactional
    public void updateProfile(Long id, String name, String email, String phone, String address, String profilePicture) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Validate and update name
        String normalizedName = normalizeName(name);
        user.setName(normalizedName);
        
        // Validate and update email if changed
        String normalizedEmail = normalizeEmail(email);
        if (!normalizedEmail.equals(user.getEmail())) {
            userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Email already exists");
                }
            });
            user.setEmail(normalizedEmail);
        }
        
        // Validate and update phone if changed
        String normalizedPhone = normalizePhone(phone);
        if (!normalizedPhone.equals(user.getPhone())) {
            userRepository.findByPhone(normalizedPhone).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Phone number already exists");
                }
            });
            user.setPhone(normalizedPhone);
        }
        
        // Update address (optional field)
        user.setAddress(address != null && !address.trim().isEmpty() ? address.trim() : null);
        
        // Update profile picture (optional field)
        user.setProfilePicture(profilePicture != null && !profilePicture.trim().isEmpty() ? profilePicture.trim() : null);
        
        userRepository.save(user);
    }

    private String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name is required");
        }
        String normalized = name.trim();
        if (normalized.length() < 2 || normalized.length() > 60) {
            throw new IllegalArgumentException("Name must be between 2 and 60 characters");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || !normalized.contains("@") || normalized.startsWith("@") || normalized.endsWith("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }
        return normalized;
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            throw new IllegalArgumentException("Phone number is required");
        }
        String normalized = phone.replaceAll("[^0-9]", "");
        if (!normalized.matches("\\d{10}")) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits");
        }
        return normalized;
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    }
}
