package org.project.service;

import org.project.dto.requestDto.LoginRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.project.model.User;
import org.project.dto.requestDto.RegisterRequest;

import java.util.Optional;

public interface LoginService {
    RegisterResponse register(RegisterRequest req);
    ApiResponse<RegisterResponse> login(LoginRequest request);

    ApiResponse<RegisterResponse> logout(String authorizationHeader);
}
