package com.shop.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {
    @GetMapping("/users")
    public Mono<Map<String, Object>> userFallback() {
        return Mono.just(Map.of("status", "down"));
    }
}
