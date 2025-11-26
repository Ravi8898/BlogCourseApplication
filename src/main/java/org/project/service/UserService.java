package org.project.service;

import org.project.model.User;
import org.project.request.RegisterRequest;

import java.util.Optional;

public interface UserService {
    User register(RegisterRequest req);
    Optional<User> findByUsername(String username);

    User login(String username, String password);
}
