package org.project.controller;

import static org.project.constants.MessageConstants.*;
import org.project.dto.responseDto.ApiResponse;
import org.project.dto.responseDto.RegisterResponse;
import org.project.model.User;
import org.project.dto.requestDto.RegisterRequest;
import org.project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody RegisterRequest request) {
        try {
            RegisterResponse response = userService.register(request);

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

    @GetMapping("/login")
    public String login() {
        return "Login successful!";
    }

    @GetMapping("/logout")
    public String logout() { return "Logout successful!"; }

}
