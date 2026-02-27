package com.techouts.service;

import com.techouts.entity.User;

import java.util.Optional;

public interface UserService {
    User register(String name, String email, String phone, String rawPassword);

    Optional<User> login(String emailOrPhone, String rawPassword);

    User findById(Long id);
    
    void updateProfile(Long id, String name, String email, String phone, String address, String profilePicture);
}
