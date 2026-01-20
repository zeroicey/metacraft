package com.metacraft.api.security;

import com.metacraft.api.response.ApiResponse;
import com.metacraft.api.response.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class AuthUtils {
    public static ResponseEntity<ApiResponse<?>> validateAuthorization(String authHeader, JwtTokenProvider jwtTokenProvider) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Response.error("未提供有效的认证令牌").build().getBody());
        }
        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Response.error("认证令牌无效或已过期").build().getBody());
        }
        return null;
    }

    private AuthUtils() {}
}
