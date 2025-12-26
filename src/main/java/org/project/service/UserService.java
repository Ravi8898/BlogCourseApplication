package org.project.service;

import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.UserResponse;

public interface UserService {
    ApiResponse<UserResponse> getUserById(Long id);
}
