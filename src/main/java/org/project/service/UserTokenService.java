package org.project.service;

import org.project.dto.requestDto.UserTokenRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.model.User;

import java.time.LocalDateTime;

public interface UserTokenService {

    void saveToken(User user, String token, LocalDateTime expiryTime);
    boolean isTokenValid(String token);
    void revokeToken(String token);
    void revokeAllTokensForUser(Long userId);
    boolean isTokenExpired(String token);
    ApiResponse<?> revokeAllTokensByUserId(UserTokenRequest userTokenRequest) ;
}

