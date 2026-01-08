package org.project.service;

import jakarta.servlet.http.HttpServletRequest;
import org.project.dto.requestDto.UserTokenRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.model.User;

import java.time.LocalDateTime;

public interface UserTokenService {

    void saveToken(User user, String token, LocalDateTime expiryTime);
    boolean isTokenValid(String token);
    void revokeToken(String token);
    boolean isTokenExpired(String token);
    ApiResponse<?> revokeAllTokensExceptCurrentTokenByUserId(HttpServletRequest httpServletRequest) ;
}

