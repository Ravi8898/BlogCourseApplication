package org.project.serviceImpl;


import org.project.model.User;
import org.project.repository.UserRepository;
import org.project.request.RegisterRequest;
import org.project.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@PropertySource("classpath:messages.properties")
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Value("${USERNAME.PASSWORD.REQUIRED}")
    private String userRequiredMessage;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User register(RegisterRequest req) {
        if (req == null || req.getUsername() == null || req.getUsername().isBlank()
                || req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException(userRequiredMessage);
        }

        String username = req.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            return null;
        }

        User user = User.builder()
                .username(username)
                .password(req.getPassword())
                .email(req.getEmail())
                .build();

        userRepository.save(user);
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return userRepository.findByUsername(username.trim());
    }
}

