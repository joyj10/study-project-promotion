package com.shop.couponservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserIdInterceptor implements HandlerInterceptor {
    public static final String USER_ID_HEADER = "X-USER-ID";
    public static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            throw new IllegalStateException("X-USER-ID header is missing");
        }

        try {
            Long userId = Long.parseLong(userIdHeader);
            currentUserId.set(userId);
            return true;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid X-USER-ID header format", e);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

    public static Long getCurrentUserId() {
        Long userId = currentUserId.get();
        if (userId == null) {
            throw new IllegalStateException("No user ID found in the current thread");
        }
        return userId;
    }
}
