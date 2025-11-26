package org.project.serviceImpl;

import org.project.dto.responseDto.RegisterResponse;
import org.project.mapper.UserToRegisterResponseMapper;
import org.project.model.User;
import org.project.repository.UserRepository;
import org.project.dto.requestDto.RegisterRequest;
import org.project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import static org.project.constants.MessageConstants.*;

import java.util.Optional;

@Service
@PropertySource("classpath:messages.properties")
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    private UserToRegisterResponseMapper mapper;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        try{
            if (userRepository.existsByUsername(request.getUsername())) {
                return null;
            }
            User user = User.builder()
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .email(request.getEmail())
                    .build();

            User savedUser = userRepository.save(user);
            return mapper.map(savedUser);
        } catch (Exception e) {
            throw new RuntimeException(SOMETHING_WENT_WRONG, e);
        }

    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return userRepository.findByUsername(username.trim());
    }
}

