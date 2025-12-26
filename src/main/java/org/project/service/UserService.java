package org.project.service;

import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.UserResponse;

import java.util.List;

public interface UserService {
    ApiResponse<UserResponse> getUserById(Long userId);
    ApiResponse<List<UserResponse>> getAllUsers();
}
