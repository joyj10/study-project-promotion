package com.shop.userservice.controller;

import com.shop.userservice.dto.UserDto;
import com.shop.userservice.entity.User;
import com.shop.userservice.service.JWTService;
import com.shop.userservice.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JWTService jwtService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<UserDto.LoginResponse> login(@RequestBody UserDto.LoginRequest request) {
        User user = userService.authenticate(request.getEmail(), request.getPassword());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(
                UserDto.LoginResponse.builder()
                        .token(token)
                        .user(UserDto.Response.from(user))
                        .build()
        );
    }

    @PostMapping("/token/validation")
    public ResponseEntity<UserDto.TokenResponse> validateToken(@RequestBody UserDto.TokenRequest request) {
        Claims claims = jwtService.validateToken(request.getToken());
        return ResponseEntity.ok(
                UserDto.TokenResponse.builder()
                        .userId(claims.get("userId", Long.class))
                        .email(claims.getSubject())
                        .valid(true)
                        .role(claims.get("role", String.class))
                        .build()
        );
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody UserDto.TokenRequest tokenRequest) {
        String newToken = jwtService.refreshToken(tokenRequest.getToken());
        return ResponseEntity.ok(Collections.singletonMap("token", newToken));
    }
}
