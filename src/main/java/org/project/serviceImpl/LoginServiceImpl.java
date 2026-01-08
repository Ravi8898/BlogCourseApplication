package org.project.serviceImpl;

import org.project.constants.MessageConstants;
import org.project.dto.requestDto.ForgotPasswordRequest;
import org.project.dto.requestDto.LoginRequest;
import org.project.dto.requestDto.ResetPasswordRequest;
import org.project.dto.responseDto.*;
import org.project.mapper.AddressRequestMapper;
import org.project.mapper.AddressResponseMapper;
import org.project.model.Address;
import org.project.model.PasswordResetToken;
import org.project.model.User;
import org.project.repository.AddressRepository;
import org.project.repository.PasswordResetTokenRepository;
import org.project.repository.UserRepository;
import org.project.dto.requestDto.RegisterRequest;
import org.project.repository.UserTokenRepository;
import org.project.security.JwtUtil;
import org.project.service.EmailService;
import org.project.service.LoginService;
import org.project.service.UserTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.javapoet.ClassName;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.project.constants.MessageConstants.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Password encoder bean (BCrypt recommended)
     * Used for encrypting and validating passwords
     */
    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;
    private final UserTokenService userTokenService;

    private final EmailService emailService;

    @Value("${frontend.reset.password.url}")
    private String resetPasswordBaseUrl;


    private static final Logger log =
            LoggerFactory.getLogger(LoginServiceImpl.class);

    private UserTokenRepository userTokenRepository;

    /**
     * Constructor-based dependency injection
     */
    public LoginServiceImpl(AuthenticationManager authenticationManager,
                            UserRepository userRepository, AddressRepository addressRepository,PasswordResetTokenRepository passwordResetTokenRepository,UserTokenRepository userTokenRepository,
                            PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                            UserTokenService userTokenService,EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userTokenRepository = userTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userTokenService = userTokenService;
        this.emailService = emailService;
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

    /**
     * Handles forgot password request.
     * *
     * This method:
     * 1. Accepts user's email via ForgotPasswordRequest.
     * 2. Checks if an active user exists for the given email.
     * 3. If user does not exist, returns a generic success response
     *    (to prevent user enumeration attacks).
     * 4. If user exists:
     *    - Generates a secure random reset token.
     *    - Saves the token with expiry and unused status.
     *    - Sends a password reset link to the user's email.
     *
     * @param request ForgotPasswordRequest containing user's email
     * @return ApiResponse<Void> with generic success or failure response
     */
    @Override
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {

        ApiResponse<Void> response;

        try {
            log.info("Forgot password process started");

            // Fetch active user by email
            Optional<User> userOpt =
                    userRepository.findByEmailAndIsActive(request.getEmail(), "Y");

            // If user does not exist, return generic success response
            // This is intentional to prevent user enumeration
            if (userOpt.isEmpty()) {

                log.info("Forgot password requested for non-existing or inactive email");

                return new ApiResponse<>(
                        SUCCESS,
                        RESET_LINK_SUCCESS,
                        HttpStatus.OK.value(),
                        null
                );
            }

            User user = userOpt.get();

            // Generate secure random token
            String token = UUID.randomUUID().toString();

            // Create password reset token entity with expiry
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .userId(user.getId())
                    .expiryTime(LocalDateTime.now().plusMinutes(15))
                    .used("N")
                    .build();

            // Persist reset token
            passwordResetTokenRepository.save(resetToken);

            // Build password reset link for frontend
            String resetLink = resetPasswordBaseUrl
                    + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

            // Send password reset email
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

            log.info("Password reset link sent successfully for userId={}", user.getId());

            response = new ApiResponse<>(
                    SUCCESS,
                    RESET_LINK_SUCCESS,
                    HttpStatus.OK.value(),
                    null
            );

        } catch (Exception ex) {

            // Log error without exposing sensitive data
            log.error("Error occurred during forgot password process", ex);

            response = new ApiResponse<>(
                    FAILED,
                    SOMETHING_WENT_WRONG,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    null
            );
        }

        return response;
    }

    /**
     * Resets the user's password using a valid password reset token.
     *
     * <p>This API performs the following:
     * <ul>
     *   <li>Validates the reset token (exists, unused, not expired)</li>
     *   <li>Updates the user's password with an encoded value</li>
     *   <li>Revokes all existing login tokens for the user</li>
     *   <li>Marks the reset token as used</li>
     * </ul>
     *
     * @param request contains reset token (extracted from link) and new password
     * @return ApiResponse indicating success or failure of password reset
     */
    @Override
    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {

        try {
            log.info("Reset password process started");

            // 1. Fetch unused reset token
            PasswordResetToken resetToken =
                    passwordResetTokenRepository
                            .findByTokenAndUsed(request.getToken(), "N")
                            .orElseThrow(() ->
                                    new RuntimeException("Invalid or expired reset token"));

            // 2. Check token expiry
            if (resetToken.getExpiryTime().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Reset token has expired");
            }

            // 3. Fetch user
            User user = userRepository.findByIdAndIsActive(resetToken.getUserId(),"Y")
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 4. Update password (always encode)
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            // 5. Revoke all active login tokens
            userTokenRepository.revokeAllTokensByUserId(user.getId());

            // 6. Mark reset token as used
            resetToken.setUsed("Y");
            passwordResetTokenRepository.save(resetToken);

            log.info("Password reset successful for userId={}", user.getId());

            return new ApiResponse<>(
                    SUCCESS,
                    PASSWORD_RESET_SUCCESS,
                    HttpStatus.OK.value(),
                    null
            );

        } catch (Exception ex) {
            log.error("Error during reset password process", ex);

            return new ApiResponse<>(
                    FAILED,
                    SOMETHING_WENT_WRONG,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    null
            );
        }
    }

}
