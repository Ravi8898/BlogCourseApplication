package org.project.serviceImpl;

import org.project.constants.MessageConstants;
import org.project.dto.requestDto.LoginRequest;
import org.project.dto.responseDto.AddressResponse;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.LoginResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.project.mapper.AddressRequestMapper;
import org.project.mapper.AddressResponseMapper;
import org.project.model.Address;
import org.project.model.User;
import org.project.repository.AddressRepository;
import org.project.repository.UserRepository;
import org.project.dto.requestDto.RegisterRequest;
import org.project.security.JwtUtil;
import org.project.service.LoginService;
import org.project.service.UserTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.javapoet.ClassName;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.project.constants.MessageConstants.*;

import java.time.LocalDateTime;

/**
 * Service implementation for Login & Registration operations.
 * Handles authentication, password encryption, and response mapping.
 */
@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private AddressResponseMapper addressResponseMapper;

    @Autowired
    private AddressRequestMapper addressRequestMapper;

    /**
     * Spring Security authentication manager
     * Used to authenticate username & password
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Repository to interact with Users table
     */
    private final UserRepository userRepository;

    private final AddressRepository addressRepository;

    /**
     * Password encoder bean (BCrypt recommended)
     * Used for encrypting and validating passwords
     */
    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;
    private final UserTokenService userTokenService;

    private static final Logger log =
            LoggerFactory.getLogger(LoginServiceImpl.class);

    /**
     * Constructor-based dependency injection
     */
    public LoginServiceImpl(AuthenticationManager authenticationManager,
                            UserRepository userRepository, AddressRepository addressRepository,
                            PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                            UserTokenService userTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userTokenService = userTokenService;
    }

    /**
     * Registers a new user into the system
     *
     * @param request RegisterRequest containing user details
     * @return RegisterResponse DTO
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public RegisterResponse register(RegisterRequest request) {

        log.info("Register request received");

        try {
            // Check if user already exists
            if (userRepository.existsByEmail(request.getEmail()) ||
                    userRepository.existsByPhoneNumber(request.getPhoneNumber())) {

                log.info("Registration failed - email or phone already exists");
                return null;
            }

            Long addressId = null;
            AddressResponse addressResponse = null;

            if (request.getAddress() != null) {

                log.info("Address provided, processing address");

                Address address = addressRequestMapper.map(request.getAddress());
                Address savedAddress = addressRepository.save(address);

                log.info("Address saved successfully with id: {}", savedAddress.getId());

                addressId = savedAddress.getId();
                addressResponse = addressResponseMapper.map(savedAddress);
            } else {
                log.info("No address provided, skipping address save");
            }

            // Build User entity
            User user = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .addressId(addressId) // ✅ can be null
                    .role(request.getRole())
                    .isActive("Y")
                    .build();

            log.info("User entity built");

            User savedUser = userRepository.save(user);
            log.info("User saved successfully with userId={}", savedUser.getId());

            return new RegisterResponse(
                    savedUser.getId(),
                    savedUser.getFirstName(),
                    savedUser.getLastName(),
                    savedUser.getEmail(),
                    savedUser.getPhoneNumber(),
                    savedUser.getRole(),
                    addressResponse // ✅ null if not provided
            );

        } catch (Exception e) {
            log.error("Exception occurred during user registration", e);
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
    public ApiResponse<LoginResponse> login(LoginRequest request) {

        log.info("Inside LoginServiceImpl Login request received for username: {}", request.getUsername());

        try {
            // Authenticate user credentials using Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),request.getPassword()));

            log.info("Authentication successful for username: {}", request.getUsername());

            // Fetch user details with the is_active status after successful authentication
            User user = userRepository.findByEmailOrPhoneNumberAndIsActive(request.getUsername(),
                            request.getUsername(),"Y")
                    .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND));

            log.info("User fetched after authentication: {}", user);

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getEmail());
            log.info("JWT token generated for userId: {}", user.getId());

            // Save token with expiry
            userTokenService.saveToken(user, token, LocalDateTime.now().plusHours(1));
            log.info("Token saved with expiry for userId: {}", user.getId());

            AddressResponse addressResponse = null;

            if (user.getAddressId() != null) {

                Address address = addressRepository.getAddressById(user.getAddressId());
                addressResponse = addressResponseMapper.map(address);

                log.info(
                        "Address fetched during login | userId={} | addressId={}",
                        user.getId(),
                        user.getAddressId()
                );

            } else {

                log.info(
                        "User logged in without address | userId={}",
                        user.getId()
                );
            }


            // Prepare response DTO
            LoginResponse response = new LoginResponse(user.getId(),
                    user.getFirstName(), user.getLastName(), user.getEmail(),
                    user.getPhoneNumber(),user.getRole(), addressResponse, token);

            // Return success response
            log.info("Login successful for userId: {}", user.getId());
            return new ApiResponse<>(SUCCESS, LOGIN_SUCCESS, HttpStatus.OK.value(), response);

        } catch (BadCredentialsException ex) {
            // Invalid username or password
            log.info("Login failed due to bad credentials for username: {}", request.getUsername());
            return new ApiResponse<>(FAILED, LOGIN_FAILED, HttpStatus.UNAUTHORIZED.value(), null);

        } catch (Exception ex) {
            // Any other system error
            log.error("Exception occurred during login for username: {}", request.getUsername(), ex);
            return new ApiResponse<>(ERROR, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
        }
    }

    /**
     * Logout user by revoking JWT token
     */
    @Override
    public ApiResponse<RegisterResponse> logout(String authHeader) {

        log.info("Logout request received");

        ApiResponse<RegisterResponse> response = new ApiResponse<>();
        try{
            String token = authHeader.substring(7);

            log.info("Token extracted for logout");

            // invalidate JWT
            userTokenService.revokeToken(token);
            log.info("Token revoked successfully");

            response.setStatus(SUCCESS);
            response.setMessage(MessageConstants.LOGOUT_SUCCESS);
            response.setStatusCode(HttpStatus.OK.value());
            response.setData(null);

        } catch (Exception e) {

            log.error("Exception occurred during logout", e);

            response.setStatus(FAILED);
            response.setMessage(LOGOUT_FAILED);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setData(null);
        }
        return response;
    }
}
