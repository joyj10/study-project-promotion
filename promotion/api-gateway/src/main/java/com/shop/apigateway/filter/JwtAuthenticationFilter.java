package com.shop.apigateway.filter;


import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @LoadBalanced
    private final WebClient webClient;  // 서비스 간 호출 시 로드밸런싱 적용된 WebClient

    // 생성자에서 WebClient 생성, user-service로 요청할 때 사용
    public JwtAuthenticationFilter(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        super(Config.class);
        this.webClient = WebClient.builder()
                .filter(lbFunction)                 // LB 필터 적용
                .baseUrl("http://user-service")
                .build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // "Bearer " 제거 후 JWT 토큰 추출
                String token = authHeader.substring(7);
                return validateToken(token)
                        .flatMap(userId -> proceedWithUserId(userId, exchange, chain))    // "Bearer " 제거 후 JWT 토큰 추출
                        .switchIfEmpty(chain.filter(exchange))    // 토큰이 없거나 invalid 시 그냥 다음 필터 진행
                        .onErrorResume(e -> handleAuthenticationError(exchange, e));   // 에러 발생 시 401 반환
          }

          // Authorization 헤더가 없는 경우, 그냥 필터 체인 진행
          return chain.filter(exchange);
        };
    }

    // JWT 토큰 user-service에 검증 요청
    private Mono<Long> validateToken(String token) {
        Map<String, String> requestBody = Map.of("token", token);

        return webClient.post()
                .uri("/api/v1/users/token/validation")
                .bodyValue(requestBody)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> Long.valueOf(response.get("userId").toString()));
    }

    // header에 userId 추가 작업
    private Mono<Void> proceedWithUserId(Long userId, ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate().header("X-USER-ID", String.valueOf(userId)).build();// downstream 서비스에 userId 전달
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    // 인증 실패 처리
    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, Throwable e) {
        e.printStackTrace();
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // 필터 구성을 위한 설정 클래스, 필요 시 옵션 추가 가능
    }
}
