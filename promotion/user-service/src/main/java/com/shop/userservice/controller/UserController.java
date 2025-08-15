package com.shop.userservice.controller;

import com.shop.userservice.dto.UserDto;
import com.shop.userservice.entity.User;
import com.shop.userservice.entity.UserLoginHistory;
import com.shop.userservice.exception.DuplicateUserException;
import com.shop.userservice.exception.UnauthorizedAccessException;
import com.shop.userservice.exception.UserNotFoundException;
import com.shop.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    private static final String X_USER_ID = "X-USER-ID";

    @PostMapping("/signup")
    public ResponseEntity<UserDto.Response> createUser(@RequestBody UserDto.SignupRequest request) {
        User user = userService.createUser(request.getEmail(), request.getPassword(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserDto.Response.from(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto.Response> getProfile(@RequestHeader(X_USER_ID) Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserDto.Response.from(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto.Response> updateProfile(@RequestHeader(X_USER_ID) Long userId,
                                           @RequestBody UserDto.UpdateRequest request) {
        User user = userService.updateUser(userId, request.getName());
        return ResponseEntity.ok(UserDto.Response.from(user));
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@RequestHeader(X_USER_ID) Long userId,
                                            @RequestBody UserDto.PasswordChangeRequest request) {
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/login-history")
    public ResponseEntity<List<UserLoginHistory>> getLoginHistory(@RequestHeader(X_USER_ID) Long userId) {
        List<UserLoginHistory> history = userService.getUserLoginHistory(userId);
        return ResponseEntity.ok(history);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<String> handleDuplicateUser(DuplicateUserException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<String> handleUnauthorizedAccess(UnauthorizedAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }
}
