package com.shop.userservice.service;

import com.shop.userservice.entity.User;
import com.shop.userservice.entity.UserLoginHistory;
import com.shop.userservice.exception.DuplicateUserException;
import com.shop.userservice.exception.UnauthorizedAccessException;
import com.shop.userservice.exception.UserNotFoundException;
import com.shop.userservice.repository.UserLoginHistoryRepository;
import com.shop.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final UserLoginHistoryRepository userLoginHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(String email, String password, String name) {
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new DuplicateUserException("User already exists with email: " + email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setName(name);

        return userRepository.save(user);
    }

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedAccessException("Invalid credentials");
        }

        return user;
    }

    public User getUserById(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    @Transactional
    public User updateUser(Long userId, String name) {
        User user = getUserById(userId);
        user.setName(name);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedAccessException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<UserLoginHistory> getUserLoginHistory(Long userId) {
        User user = getUserById(userId);
        return userLoginHistoryRepository.findByUserOrderByLoginTimeDesc(user);
    }

}
