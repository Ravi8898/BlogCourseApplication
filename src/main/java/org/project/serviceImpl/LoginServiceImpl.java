package org.project.serviceImpl;

import org.project.constants.MessageConstants;
import org.project.dto.requestDto.LoginRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.project.mapper.UserToRegisterResponseMapper;
import org.project.model.User;
import org.project.repository.UserRepository;
import org.project.dto.requestDto.RegisterRequest;
import org.project.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import static org.project.constants.MessageConstants.*;

import java.util.Optional;
import static org.project.constants.MessageConstants.*;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private UserToRegisterResponseMapper mapper;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginServiceImpl(AuthenticationManager authenticationManager,
                            UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        try{
            if (userRepository.existsByUsername(request.getUsername())) {
                return null;
            }
            User user = User.builder()
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .email(request.getEmail())
                    .isActive("Y")
                    .build();

            User savedUser = userRepository.save(user);
            return mapper.map(savedUser);
        } catch (Exception e) {
            throw new RuntimeException(SOMETHING_WENT_WRONG, e);
        }

    }

    @Override
    public ApiResponse<RegisterResponse> login(LoginRequest request) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),request.getPassword()));

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND));

            RegisterResponse response = new RegisterResponse(user.getId(),
                            user.getUsername(),user.getEmail());

            return new ApiResponse<>(SUCCESS, LOGIN_SUCCESS, HttpStatus.OK.value(), response);
        } catch (BadCredentialsException ex) {
            return new ApiResponse<>(FAILED, LOGIN_FAILED, HttpStatus.UNAUTHORIZED.value(), null);

        } catch (Exception ex) {
            return new ApiResponse<>(ERROR, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }
}

