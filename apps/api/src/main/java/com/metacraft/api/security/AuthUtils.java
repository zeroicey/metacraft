package com.metacraft.api.security;

import com.metacraft.api.exception.UnauthorizedException;
import com.metacraft.api.response.ApiResponse;
import org.springframework.http.ResponseEntity;

public final class AuthUtils {
    public static void validateAuthorization(String authHeader, JwtTokenProvider jwtTokenProvider) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("未提供有效的认证令牌");
        }
        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("认证令牌无效或已过期");
        }
    }

    private AuthUtils() {}
}
