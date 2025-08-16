package com.shop.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate: 초당 허용 되는 요청 수
        // burstCapacity: 최대 누적 가능한 요청 수
        return new RedisRateLimiter(10, 20);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-USER-ID");
            // 사용자 헤더 없으면 Proxy 환경 고려하여 X-Forwarded-For, 없으면 RemoteAddress
            if (!StringUtils.hasText(userId)) {
                String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
                userId = StringUtils.hasText(forwarded) ? forwarded.split(",")[0] :
                        exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            return Mono.just(userId);
        };
    }
}
