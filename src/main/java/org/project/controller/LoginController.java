package org.project.controller;

import static org.project.constants.MessageConstants.*;

import io.swagger.v3.oas.annotations.Operation;
import org.project.dto.requestDto.ForgotPasswordRequest;
import org.project.dto.requestDto.LoginRequest;
import org.project.dto.requestDto.ResetPasswordRequest;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.LoginResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.project.dto.requestDto.RegisterRequest;
import org.project.service.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.javapoet.ClassName;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private LoginService loginService;

    private static final Logger log =
            LoggerFactory.getLogger(LoginController.class);

    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @RequestBody RegisterRequest request) {

        log.info("Registration request received for email: {}", request.getEmail());

        try {
            RegisterResponse response = loginService.register(request);

            if (response == null) {

                log.info("Registration failed - user already exists for email: {}", request.getEmail());

                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiResponse<>(FAILED, USERNAME_EXISTS, HttpStatus.CONFLICT.value(), null));
            }

            log.info("Registration successful for userId: {}", response.getUserId());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ApiResponse<>(SUCCESS, REGISTRATION_SUCCESSFUL, HttpStatus.OK.value(), response));

        } catch (IllegalArgumentException e) {

            log.error("Invalid registration request received: {}", request, e);

            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(FAILED, BAD_REQUEST, HttpStatus.BAD_REQUEST.value(), null));

        } catch (Exception e) {

            log.error("Exception occurred during registration", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(FAILED, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR.value(), null));
        }
    }

    @Operation(summary = "Authenticate user and return JWT token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request) {

        log.info("Login request received for username: {}", request.getUsername());

        ApiResponse<LoginResponse> response = loginService.login(request);

        log.info("Response from LoginServiceImpl: {} for username: {}", response, request.getUsername());

        if (!response.getStatus().equalsIgnoreCase(SUCCESS)) {

            log.info("Login failed for username: {}", request.getUsername());

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        log.info("Login successful for username: {}", request.getUsername());

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "Logout user by revoking current JWT token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<RegisterResponse>> logout(
            @RequestHeader("Authorization") String authorizationHeader) {

        log.info("Logout request received");

        ApiResponse<RegisterResponse> response =
                loginService.logout(authorizationHeader);

        log.info("Logout response status: {}", response.getStatus());

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }

    @Operation(summary = "Send password reset link to user's email")
    @PostMapping("/forgotPassword")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        log.info("Forgot password request received");

        ApiResponse<Void> response =
                loginService.forgotPassword(request);

        log.info("Forgot password flow completed with statusCode={}",
                response.getStatusCode());

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }

    @Operation(summary = "Reset password using reset token")
    @PostMapping("/resetPassword")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
              @RequestBody ResetPasswordRequest request) {

        log.info("Reset password request received");

        ApiResponse<Void> response = loginService.resetPassword(request);

        log.info("Reset password flow completed with statusCode={}",
                response.getStatusCode());

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }


}
