package com.techouts.service;

import com.techouts.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface UserService {
    User register(String name, String username, String email, String phone, String password, String confirmPassword);

    Optional<User> login(String emailOrPhone, String rawPassword);

    User findById(Long id);
    
    void updateProfile(Long id, String name, String username, String email, String phone, String address, String gender, String dateOfBirth, MultipartFile profilePictureFile);
}
