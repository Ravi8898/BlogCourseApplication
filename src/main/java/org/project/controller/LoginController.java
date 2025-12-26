package org.project.controller;

import static org.project.constants.MessageConstants.*;

import org.project.dto.requestDto.LoginRequest;
import org.project.dto.responseDto.ApiResponse;
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
@CrossOrigin(
        origins = "*",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS}
)
public class LoginController {

    @Autowired
    private LoginService loginService;

    private static final Logger log =
            LoggerFactory.getLogger(ClassName.class);

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody RegisterRequest request) {
        try {
            RegisterResponse response = loginService.register(request);

            if (response == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiResponse<>(FAILED, USERNAME_EXISTS, HttpStatus.CONFLICT.value(), null));
            }
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ApiResponse<>(SUCCESS, REGISTRATION_SUCCESSFUL, HttpStatus.OK.value(), response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(FAILED, BAD_REQUEST, HttpStatus.BAD_REQUEST.value(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(FAILED, SOMETHING_WENT_WRONG, HttpStatus.INTERNAL_SERVER_ERROR.value(), null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<RegisterResponse>> login(@RequestBody LoginRequest request) {
        log.info("Login request received for username: {}", request.getUsername());
        ApiResponse<RegisterResponse> response = loginService.login(request);
        log.info("Response from LoginServiceImpl: {} for username: {}",response, request.getUsername());

        if (!response.getStatus().equalsIgnoreCase(SUCCESS)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<RegisterResponse>> logout(
            @RequestHeader("Authorization") String authorizationHeader) {

        ApiResponse<RegisterResponse> response =
                loginService.logout(authorizationHeader);

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }

}
