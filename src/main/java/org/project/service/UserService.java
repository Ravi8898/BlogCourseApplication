package org.project.service;

import org.project.dto.responseDto.RegisterResponse;
import org.project.model.User;
import org.project.dto.requestDto.RegisterRequest;

import java.util.Optional;

public interface UserService {
    RegisterResponse register(RegisterRequest req);
    Optional<User> findByUsername(String username);
}
