package com.techouts.service.impl;

import com.techouts.entity.Cart;
import com.techouts.entity.Gender;
import com.techouts.entity.User;
import com.techouts.entity.UserRole;
import com.techouts.repository.CartRepository;
import com.techouts.repository.UserRepository;
import com.techouts.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

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
    public User register(String name, String username, String email, String phone, String password, String confirmPassword) {
        String normalizedName = normalizeName(name);
        String normalizedUsername = normalizeUsername(username);
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phone);
        
        // Validate password confirmation
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        validatePassword(password);

        userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            throw new IllegalArgumentException("Email already exists");
        });
        userRepository.findByPhone(normalizedPhone).ifPresent(existing -> {
            throw new IllegalArgumentException("Phone number already exists");
        });
        userRepository.findByUsername(normalizedUsername).ifPresent(existing -> {
            throw new IllegalArgumentException("Username already exists");
        });

        User user = new User();
        user.setName(normalizedName);
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(UserRole.ROLE_USER);
        User saved = userRepository.save(user);

        Cart cart = new Cart();
        cart.setUser(saved);
        cartRepository.save(cart);
        return saved;
    }

    @Override
    public Optional<User> login(String identifier, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return Optional.empty();
        }

        String normalizedIdentifier = identifier == null ? "" : identifier.trim();
        if (normalizedIdentifier.isBlank()) {
            return Optional.empty();
        }

        Optional<User> userOptional;
        if (normalizedIdentifier.contains("@")) {
            // Login with email
            userOptional = userRepository.findByEmail(normalizeEmail(normalizedIdentifier));
        } else if (normalizedIdentifier.matches("\\d{10}")) {
            // Login with phone
            userOptional = userRepository.findByPhone(normalizePhone(normalizedIdentifier));
        } else {
            // Login with username
            userOptional = userRepository.findByUsername(normalizeUsername(normalizedIdentifier));
        }

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
    public void updateProfile(Long id, String name, String username, String email, String phone, String address, String gender, String dateOfBirth, MultipartFile profilePictureFile) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Validate and update name
        String normalizedName = normalizeName(name);
        user.setName(normalizedName);
        
        // Validate and update username if changed
        String normalizedUsername = normalizeUsername(username);
        if (!normalizedUsername.equals(user.getUsername())) {
            userRepository.findByUsername(normalizedUsername).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Username already exists");
                }
            });
            user.setUsername(normalizedUsername);
        }
        
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
        
        // Validate and update phone if provided
        if (phone != null && !phone.trim().isEmpty()) {
            String normalizedPhone = normalizePhone(phone);
            if (!normalizedPhone.equals(user.getPhone())) {
                userRepository.findByPhone(normalizedPhone).ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new IllegalArgumentException("Phone number already exists");
                    }
                });
                user.setPhone(normalizedPhone);
            }
        } else {
            user.setPhone(null);
        }
        
        // Update address (optional field)
        user.setAddress(address != null && !address.trim().isEmpty() ? address.trim() : null);
        
        // Update gender if provided
        if (gender != null && !gender.trim().isEmpty()) {
            try {
                user.setGender(Gender.valueOf(gender.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid gender value");
            }
        } else {
            user.setGender(null);
        }
        
        // Update date of birth if provided
        if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
            try {
                LocalDate dob = LocalDate.parse(dateOfBirth);
                user.setDateOfBirth(dob);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format for date of birth");
            }
        } else {
            user.setDateOfBirth(null);
        }
        
        // Handle profile picture upload
        if (profilePictureFile != null && !profilePictureFile.isEmpty()) {
            try {
                String profilePicturePath = saveProfilePicture(profilePictureFile);
                user.setProfilePicture(profilePicturePath);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to save profile picture: " + e.getMessage());
            }
        }
        
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
    
    private String normalizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username is required");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 3 || normalized.length() > 20) {
            throw new IllegalArgumentException("Username must be between 3 and 20 characters");
        }
        if (!normalized.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores");
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
        if (phone == null || phone.trim().isEmpty()) {
            return null;
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
    
    private String saveProfilePicture(MultipartFile file) throws IOException {
        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Profile picture size should be less than 5MB");
        }
        
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Please select a valid image file");
        }
        
        // Create upload directory if it doesn't exist
        String uploadDir = "uploads/profile-pictures/";
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ? 
            originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
        
        // Save file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return "/uploads/profile-pictures/" + uniqueFilename;
    }
}
