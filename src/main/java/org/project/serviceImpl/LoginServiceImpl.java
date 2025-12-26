package org.project.serviceImpl;

import org.project.constants.MessageConstants;
import org.project.dto.requestDto.LoginRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.project.mapper.UserToRegisterResponseMapper;
import org.project.model.User;
import org.project.repository.UserRepository;
import org.project.dto.requestDto.RegisterRequest;
import org.project.security.JwtUtil;
import org.project.service.LoginService;
import org.project.service.UserTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.javapoet.ClassName;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static org.project.constants.MessageConstants.*;

import java.time.LocalDateTime;
import java.util.Optional;
import static org.project.constants.MessageConstants.*;

/**
 * Service implementation for Login & Registration operations.
 * Handles authentication, password encryption, and response mapping.
 */
@Service
public class LoginServiceImpl implements LoginService {

    /**
     * Mapper to convert User entity to RegisterResponse DTO
     */
    @Autowired
    private UserToRegisterResponseMapper mapper;

    /**
     * Spring Security authentication manager
     * Used to authenticate username & password
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Repository to interact with Users table
     */
    private final UserRepository userRepository;

    /**
     * Password encoder bean (BCrypt recommended)
     * Used for encrypting and validating passwords
     */
    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;
    private final UserTokenService userTokenService;
    private static final Logger log =
            LoggerFactory.getLogger(ClassName.class);
    /**
     * Constructor-based dependency injection
     */
    public LoginServiceImpl(AuthenticationManager authenticationManager,
                            UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, UserTokenService userTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userTokenService = userTokenService;
    }

    /**
     * Registers a new user into the system
     *
     * @param  request RegisterRequest containing user details
     * @return RegisterResponse DTO
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public RegisterResponse register(RegisterRequest request) {
        try{
            // Check if username already exists
            if (userRepository.existsByUsername(request.getUsername())) {
                return null;
            }
            // Build User entity and encrypt password
            User user = User.builder()
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .isActive("Y")
                    .build();

            // Save user to database
            User savedUser = userRepository.save(user);
            // Convert entity to response DTO
            return mapper.map(savedUser);
        } catch (Exception e) {
            // Catch any unexpected exception and wrap it
            throw new RuntimeException(SOMETHING_WENT_WRONG, e);
        }

    }

    /**
     * Authenticates user credentials and returns login response
     *
     * @param request LoginRequest containing username & password
     * @return ApiResponse with RegisterResponse on success
     */
    @Override
    public ApiResponse<RegisterResponse> login(LoginRequest request) {
        log.info("Inside LoginServiceImpl Login request received for username: {}", request.getUsername());
        try {
            // Authenticate user credentials using Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),request.getPassword()));
            // Fetch user details after successful authentication
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND));

            // Generate JWT token (if applicable)
            String token = jwtUtil.generateToken(user.getUsername());
            //Save token with expiry
            userTokenService.saveToken(user, token, LocalDateTime.now().plusHours(1));

            // Prepare response DTO
            RegisterResponse response = new RegisterResponse(user.getId(),
                            user.getUsername(),user.getEmail(), user.getPhoneNumber(), token);
            // Return success response
            return new ApiResponse<>(SUCCESS, LOGIN_SUCCESS, HttpStatus.OK.value(), response);
        } catch (BadCredentialsException ex) {
            // Invalid username or password
            return new ApiResponse<>(FAILED, LOGIN_FAILED, HttpStatus.UNAUTHORIZED.value(), null);

        } catch (Exception ex) {
            // Any other system error
            return new ApiResponse<>(ERROR, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    @Override
    public ApiResponse<RegisterResponse> logout(String authHeader) {
        ApiResponse<RegisterResponse> response = new ApiResponse<>();
        try{
            String token = authHeader.substring(7);
            // invalidate JWT
            userTokenService.revokeToken(token);

            response.setStatus(SUCCESS);
            response.setMessage(MessageConstants.LOGOUT_SUCCESS);
            response.setStatusCode(HttpStatus.OK.value());
            response.setData(null);
        } catch (Exception e) {
            response.setStatus(FAILED);
            response.setMessage(LOGOUT_FAILED);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setData(null);
        }
        return response;
    }
}

